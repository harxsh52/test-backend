package com.interniq.interview;

import com.interniq.audit.AuditLogService;
import com.interniq.ai.AiProvider;
import com.interniq.ai.AiProviderFactory;
import com.interniq.ai.ResumeScreeningResult;
import com.interniq.ai.ResumeScreeningResultRepository;
import com.interniq.ai.dto.InterviewEvaluationResponse;
import com.interniq.candidate.Candidate;
import com.interniq.candidate.CandidateService;
import com.interniq.candidate.CandidateStatus;
import com.interniq.intern.InternProfile;
import com.interniq.intern.InternProfileService;
import com.interniq.interview.dto.InterviewAnswerRequest;
import com.interniq.interview.dto.InterviewAnswerResponse;
import com.interniq.interview.dto.InterviewQuestionResponse;
import com.interniq.interview.dto.InterviewRequest;
import com.interniq.interview.dto.InterviewResponse;
import com.interniq.interview.dto.InterviewResultResponse;
import com.interniq.notification.EmailNotificationService;
import com.interniq.notification.NotificationPriority;
import com.interniq.notification.NotificationService;
import com.interniq.notification.NotificationType;
import com.interniq.user.Role;
import com.interniq.user.User;
import com.interniq.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final ResumeScreeningResultRepository screeningResultRepository;
    private final AiProviderFactory aiProviderFactory;
    private final CandidateService candidateService;
    private final InternProfileService internProfileService;
    private final UserService userService;
    private final AuditLogService auditLogService;
    private final EmailNotificationService emailNotificationService;
    private final NotificationService notificationService;

    @Transactional
    public InterviewResponse scheduleInterview(InterviewRequest request) {
        return scheduleInterview(request, null);
    }

    @Transactional
    public InterviewResponse scheduleInterview(InterviewRequest request, Authentication authentication) {
        if (request.getCandidateId() == null && request.getInternId() == null) {
            throw new IllegalArgumentException("Candidate or intern is required");
        }

        Candidate candidate = null;
        InternProfile intern = null;

        if (request.getCandidateId() != null) {
            candidate = candidateService.getCandidateOrThrow(request.getCandidateId());
            candidate.setStatus(CandidateStatus.INTERVIEW_SCHEDULED);
        }

        if (request.getInternId() != null) {
            intern = internProfileService.getProfileOrThrow(request.getInternId());
        }

        String role = firstNonBlank(request.getRole(), candidate == null ? null : candidate.getAppliedRole(), "Intern");

        Interview interview = Interview.builder()
                .candidate(candidate)
                .intern(intern)
                .role(role)
                .status(InterviewStatus.SCHEDULED)
                .scheduledAt(request.getScheduledAt() == null ? LocalDateTime.now().plusDays(1) : request.getScheduledAt())
                .build();

        Interview savedInterview = interviewRepository.save(interview);
        auditLogService.record(actor(authentication), "INTERVIEW_SCHEDULED", "Interview", savedInterview.getId());
        if (savedInterview.getIntern() != null) {
            notificationService.createNotification(
                    savedInterview.getIntern().getUser(),
                    "AI Interview Scheduled",
                    "Your AI interview for " + role + " has been scheduled",
                    NotificationType.INTERVIEW,
                    NotificationPriority.HIGH,
                    "/intern/ai-interview",
                    null
            );
        }
        emailNotificationService.sendInterviewScheduled(savedInterview, authentication);
        return toResponse(savedInterview);
    }

    @Transactional(readOnly = true)
    public List<InterviewResponse> getMyInterviews(Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);

        List<Interview> interviews = switch (currentUser.getRole()) {
            case ADMIN, HR -> interviewRepository.findAllByOrderByScheduledAtDesc();
            case MANAGER -> interviewRepository.findByIntern_Manager_IdOrderByScheduledAtDesc(currentUser.getId());
            case INTERN -> interviewRepository.findByIntern_User_IdOrderByScheduledAtDesc(currentUser.getId());
        };

        return interviews.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public InterviewResponse getInterview(Long id, Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        Interview interview = getInterviewOrThrow(id);
        ensureCanViewInterview(currentUser, interview);
        return toResponse(interview);
    }

    @Transactional
    public InterviewResponse generateQuestions(Long id) {
        return generateQuestions(id, null);
    }

    @Transactional
    public InterviewResponse generateQuestions(Long id, Authentication authentication) {
        Interview interview = getInterviewOrThrow(id);
        interview.getAnswers().clear();
        interview.getQuestions().clear();

        List<InterviewQuestion> questions = buildQuestions(interview);
        questions.forEach(question -> {
            question.setInterview(interview);
            interview.getQuestions().add(question);
        });
        auditLogService.record(actor(authentication), "INTERVIEW_QUESTIONS_GENERATED", "Interview", interview.getId());

        return toResponse(interview);
    }

    @Transactional
    public InterviewResponse startInterview(Long id, Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        Interview interview = getInterviewOrThrow(id);
        ensureCanTakeOrAdministerInterview(currentUser, interview);

        if (interview.getStatus() == InterviewStatus.COMPLETED) {
            throw new IllegalArgumentException("Completed interviews cannot be started again");
        }

        if (interview.getQuestions().isEmpty()) {
            buildQuestions(interview).forEach(question -> {
                question.setInterview(interview);
                interview.getQuestions().add(question);
            });
        }

        interview.setStatus(InterviewStatus.IN_PROGRESS);
        interview.setStartedAt(LocalDateTime.now());
        auditLogService.record(currentUser, "INTERVIEW_STARTED", "Interview", interview.getId());

        return toResponse(interview);
    }

    @Transactional
    public InterviewAnswerResponse submitAnswer(Long id, InterviewAnswerRequest request, Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        Interview interview = getInterviewOrThrow(id);
        ensureCanTakeOrAdministerInterview(currentUser, interview);

        if (interview.getStatus() != InterviewStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Interview must be in progress before submitting answers");
        }

        InterviewQuestion question = interview.getQuestions()
                .stream()
                .filter(currentQuestion -> Objects.equals(currentQuestion.getId(), request.getQuestionId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Question not found for this interview"));

        InterviewAnswer answer = interview.getAnswers()
                .stream()
                .filter(currentAnswer -> Objects.equals(currentAnswer.getQuestion().getId(), question.getId()))
                .findFirst()
                .orElseGet(() -> {
                    InterviewAnswer newAnswer = InterviewAnswer.builder()
                            .interview(interview)
                            .question(question)
                            .build();
                    interview.getAnswers().add(newAnswer);
                    return newAnswer;
                });

        AiProvider.AnswerEvaluation evaluation = aiProviderFactory.getProvider().evaluateAnswer(request.getAnswerText(), question);
        answer.setAnswerText(request.getAnswerText().trim());
        answer.setScore(evaluation.score());
        answer.setFeedback(evaluation.feedback());
        answer.setSubmittedAt(LocalDateTime.now());

        return toAnswerResponse(answer);
    }

    @Transactional
    public InterviewResponse completeInterview(Long id, Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        Interview interview = getInterviewOrThrow(id);
        ensureCanTakeOrAdministerInterview(currentUser, interview);

        if (interview.getStatus() == InterviewStatus.COMPLETED) {
            return toResponse(interview);
        }

        InterviewResult result = buildResult(interview);
        result.setInterview(interview);
        interview.setResult(result);
        interview.setStatus(InterviewStatus.COMPLETED);
        interview.setCompletedAt(LocalDateTime.now());
        interview.setFinalScore(result.getFinalScore());
        interview.setRecommendation(result.getRecommendation());
        auditLogService.record(currentUser, "INTERVIEW_COMPLETED", "Interview", interview.getId());
        String candidateName = interview.getIntern() != null
                ? interview.getIntern().getUser().getName()
                : interview.getCandidate() == null ? "Candidate" : interview.getCandidate().getName();
        String completionMessage = candidateName + " completed AI interview for " + interview.getRole();
        notificationService.notifyRole(
                Role.HR,
                "AI Interview Completed",
                completionMessage,
                NotificationType.INTERVIEW,
                NotificationPriority.HIGH,
                "/hr/interviews/" + interview.getId() + "/result"
        );
        notificationService.notifyRole(
                Role.ADMIN,
                "AI Interview Completed",
                completionMessage,
                NotificationType.INTERVIEW,
                NotificationPriority.HIGH,
                "/admin/interviews/" + interview.getId() + "/result"
        );

        return toResponse(interview);
    }

    @Transactional(readOnly = true)
    public InterviewResultResponse getResult(Long id, Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        Interview interview = getInterviewOrThrow(id);
        ensureCanViewInterview(currentUser, interview);

        if (interview.getResult() == null) {
            throw new IllegalArgumentException("Interview result is not available yet");
        }

        return toResultResponse(interview.getResult());
    }

    private Interview getInterviewOrThrow(Long id) {
        return interviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found"));
    }

    private void ensureCanViewInterview(User currentUser, Interview interview) {
        if (isHrOrAdmin(currentUser)) {
            return;
        }

        if (currentUser.getRole() == Role.INTERN
                && interview.getIntern() != null
                && Objects.equals(interview.getIntern().getUser().getId(), currentUser.getId())) {
            return;
        }

        if (currentUser.getRole() == Role.MANAGER
                && interview.getIntern() != null
                && internProfileService.isManagerOf(currentUser, interview.getIntern())) {
            return;
        }

        throw new AccessDeniedException("You are not allowed to access this interview");
    }

    private void ensureCanTakeOrAdministerInterview(User currentUser, Interview interview) {
        if (isHrOrAdmin(currentUser)) {
            return;
        }

        if (currentUser.getRole() == Role.INTERN
                && interview.getIntern() != null
                && Objects.equals(interview.getIntern().getUser().getId(), currentUser.getId())) {
            return;
        }

        throw new AccessDeniedException("You are not allowed to take this interview");
    }

    private boolean isHrOrAdmin(User user) {
        return user.getRole() == Role.HR || user.getRole() == Role.ADMIN;
    }

    private User actor(Authentication authentication) {
        return authentication == null ? null : userService.getCurrentUser(authentication);
    }

    private List<InterviewQuestion> buildQuestions(Interview interview) {
        ResumeScreeningResult screeningResult = interview.getCandidate() == null
                ? null
                : screeningResultRepository.findByCandidateId(interview.getCandidate().getId()).orElse(null);

        return aiProviderFactory.getProvider()
                .generateInterviewQuestions(interview, screeningResult)
                .stream()
                .map(question -> question(question.getQuestionText(), question.getQuestionType(), question.getOrderNumber()))
                .toList();
    }

    private InterviewQuestion question(String text, QuestionType type, int orderNumber) {
        return InterviewQuestion.builder()
                .questionText(text)
                .questionType(type)
                .orderNumber(orderNumber)
                .build();
    }

    private InterviewResult buildResult(Interview interview) {
        InterviewEvaluationResponse evaluation = aiProviderFactory.getProvider().evaluateInterview(interview);

        return InterviewResult.builder()
                .technicalScore(evaluation.getTechnicalScore())
                .communicationScore(evaluation.getCommunicationScore())
                .problemSolvingScore(evaluation.getProblemSolvingScore())
                .confidenceScore(evaluation.getConfidenceScore())
                .finalScore(evaluation.getFinalScore())
                .strengths(evaluation.getStrengths())
                .weaknesses(evaluation.getWeaknesses())
                .recommendation(evaluation.getRecommendation())
                .aiSummary(evaluation.getAiSummary())
                .provider(evaluation.getProvider())
                .mockResult(evaluation.getMockResult())
                .build();
    }

    private InterviewResponse toResponse(Interview interview) {
        return InterviewResponse.builder()
                .id(interview.getId())
                .candidateId(interview.getCandidate() == null ? null : interview.getCandidate().getId())
                .candidateName(interview.getCandidate() == null ? null : interview.getCandidate().getName())
                .internId(interview.getIntern() == null ? null : interview.getIntern().getId())
                .internName(interview.getIntern() == null ? null : interview.getIntern().getUser().getName())
                .role(interview.getRole())
                .status(interview.getStatus())
                .scheduledAt(interview.getScheduledAt())
                .startedAt(interview.getStartedAt())
                .completedAt(interview.getCompletedAt())
                .finalScore(interview.getFinalScore())
                .recommendation(interview.getRecommendation())
                .questions(interview.getQuestions()
                        .stream()
                        .sorted(Comparator.comparing(InterviewQuestion::getOrderNumber))
                        .map(this::toQuestionResponse)
                        .toList())
                .answers(interview.getAnswers()
                        .stream()
                        .sorted(Comparator.comparing(answer -> answer.getQuestion().getOrderNumber()))
                        .map(this::toAnswerResponse)
                        .toList())
                .result(interview.getResult() == null ? null : toResultResponse(interview.getResult()))
                .build();
    }

    private InterviewQuestionResponse toQuestionResponse(InterviewQuestion question) {
        return InterviewQuestionResponse.builder()
                .id(question.getId())
                .questionText(question.getQuestionText())
                .questionType(question.getQuestionType())
                .orderNumber(question.getOrderNumber())
                .build();
    }

    private InterviewAnswerResponse toAnswerResponse(InterviewAnswer answer) {
        return InterviewAnswerResponse.builder()
                .id(answer.getId())
                .questionId(answer.getQuestion() == null ? null : answer.getQuestion().getId())
                .answerText(answer.getAnswerText())
                .score(answer.getScore())
                .feedback(answer.getFeedback())
                .submittedAt(answer.getSubmittedAt())
                .build();
    }

    private InterviewResultResponse toResultResponse(InterviewResult result) {
        return InterviewResultResponse.builder()
                .id(result.getId())
                .interviewId(result.getInterview().getId())
                .technicalScore(result.getTechnicalScore())
                .communicationScore(result.getCommunicationScore())
                .problemSolvingScore(result.getProblemSolvingScore())
                .confidenceScore(result.getConfidenceScore())
                .finalScore(result.getFinalScore())
                .strengths(result.getStrengths())
                .weaknesses(result.getWeaknesses())
                .recommendation(result.getRecommendation())
                .aiSummary(result.getAiSummary())
                .provider(result.getProvider())
                .mockResult(result.getMockResult())
                .build();
    }

    private String firstNonBlank(String first, String second, String fallback) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }

        if (second != null && !second.isBlank()) {
            return second.trim();
        }

        return fallback;
    }
}
