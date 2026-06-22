package com.interniq.interview;

import com.interniq.audit.AuditLogService;
import com.interniq.ai.AiProviderFactory;
import com.interniq.ai.MockAiProvider;
import com.interniq.ai.ResumeScreeningResultRepository;
import com.interniq.candidate.CandidateService;
import com.interniq.intern.InternProfile;
import com.interniq.intern.InternProfileService;
import com.interniq.interview.dto.InterviewAnswerRequest;
import com.interniq.interview.dto.InterviewRequest;
import com.interniq.notification.EmailNotificationService;
import com.interniq.notification.NotificationPriority;
import com.interniq.notification.NotificationService;
import com.interniq.notification.NotificationType;
import com.interniq.user.Role;
import com.interniq.user.User;
import com.interniq.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterviewServiceTest {

    @Mock
    private InterviewRepository interviewRepository;

    @Mock
    private ResumeScreeningResultRepository screeningResultRepository;

    @Mock
    private AiProviderFactory aiProviderFactory;

    @Mock
    private CandidateService candidateService;

    @Mock
    private InternProfileService internProfileService;

    @Mock
    private UserService userService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private EmailNotificationService emailNotificationService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private InterviewService interviewService;

    @Test
    void scheduleInterview() {
        User admin = user(4L, Role.ADMIN);
        InternProfile intern = profile(10L, user(1L, Role.INTERN), user(2L, Role.MANAGER));
        Authentication authentication = authentication(admin);
        InterviewRequest request = new InterviewRequest();
        request.setInternId(10L);
        request.setRole("React Intern");
        request.setScheduledAt(LocalDateTime.now().plusDays(1));

        when(internProfileService.getProfileOrThrow(10L)).thenReturn(intern);
        when(userService.getCurrentUser(authentication)).thenReturn(admin);
        when(interviewRepository.save(any(Interview.class))).thenAnswer(invocation -> {
            Interview interview = invocation.getArgument(0);
            interview.setId(77L);
            return interview;
        });

        var response = interviewService.scheduleInterview(request, authentication);

        assertThat(response.getId()).isEqualTo(77L);
        assertThat(response.getInternId()).isEqualTo(10L);
        assertThat(response.getStatus()).isEqualTo(InterviewStatus.SCHEDULED);
        verify(auditLogService).record(admin, "INTERVIEW_SCHEDULED", "Interview", 77L);
        verify(notificationService).createNotification(
                intern.getUser(),
                "AI Interview Scheduled",
                "Your AI interview for React Intern has been scheduled",
                NotificationType.INTERVIEW,
                NotificationPriority.HIGH,
                "/intern/ai-interview",
                null
        );
        verify(emailNotificationService).sendInterviewScheduled(any(Interview.class), any(Authentication.class));
    }

    @Test
    void generateMockQuestions() {
        User hr = user(3L, Role.HR);
        Interview interview = interview(77L, profile(10L, user(1L, Role.INTERN), user(2L, Role.MANAGER)), InterviewStatus.SCHEDULED);
        Authentication authentication = authentication(hr);

        when(interviewRepository.findById(77L)).thenReturn(Optional.of(interview));
        when(aiProviderFactory.getProvider()).thenReturn(new MockAiProvider());
        when(userService.getCurrentUser(authentication)).thenReturn(hr);

        var response = interviewService.generateQuestions(77L, authentication);

        assertThat(response.getQuestions()).hasSize(6);
        assertThat(response.getQuestions().get(0).getQuestionText()).contains("useState");
        verify(auditLogService).record(hr, "INTERVIEW_QUESTIONS_GENERATED", "Interview", 77L);
    }

    @Test
    void submitAnswer() {
        User internUser = user(1L, Role.INTERN);
        InternProfile intern = profile(10L, internUser, user(2L, Role.MANAGER));
        Interview interview = interview(77L, intern, InterviewStatus.IN_PROGRESS);
        InterviewQuestion question = question(100L, interview, "How do you handle API errors?", QuestionType.SCENARIO, 1);
        interview.getQuestions().add(question);
        Authentication authentication = authentication(internUser);
        InterviewAnswerRequest request = new InterviewAnswerRequest();
        request.setQuestionId(100L);
        request.setAnswerText("I handle API errors by showing loading and error states, validating responses, logging useful details, and giving the user a retry path because reliability matters.");

        when(userService.getCurrentUser(authentication)).thenReturn(internUser);
        when(interviewRepository.findById(77L)).thenReturn(Optional.of(interview));
        when(aiProviderFactory.getProvider()).thenReturn(new MockAiProvider());

        var response = interviewService.submitAnswer(77L, request, authentication);

        assertThat(response.getQuestionId()).isEqualTo(100L);
        assertThat(response.getAnswerText()).contains("API errors");
        assertThat(response.getScore()).isGreaterThan(0);
        assertThat(response.getFeedback()).isNotBlank();
        assertThat(interview.getAnswers()).hasSize(1);
    }

    @Test
    void completeInterviewAndGenerateMockResult() {
        User internUser = user(1L, Role.INTERN);
        InternProfile intern = profile(10L, internUser, user(2L, Role.MANAGER));
        Interview interview = interview(77L, intern, InterviewStatus.IN_PROGRESS);
        InterviewQuestion question = question(100L, interview, "What is useState?", QuestionType.THEORY, 1);
        InterviewAnswer answer = InterviewAnswer.builder()
                .interview(interview)
                .question(question)
                .answerText("useState stores component state. I used it in a project to track loading, error, and form values because the UI needed to react to user input.")
                .score(82)
                .feedback("Strong answer.")
                .submittedAt(LocalDateTime.now())
                .build();
        interview.getQuestions().add(question);
        interview.getAnswers().add(answer);
        Authentication authentication = authentication(internUser);

        when(userService.getCurrentUser(authentication)).thenReturn(internUser);
        when(interviewRepository.findById(77L)).thenReturn(Optional.of(interview));
        when(aiProviderFactory.getProvider()).thenReturn(new MockAiProvider());

        var response = interviewService.completeInterview(77L, authentication);

        assertThat(response.getStatus()).isEqualTo(InterviewStatus.COMPLETED);
        assertThat(response.getFinalScore()).isGreaterThan(0);
        assertThat(response.getResult()).isNotNull();
        assertThat(response.getResult().getRecommendation()).isNotBlank();
        verify(auditLogService).record(internUser, "INTERVIEW_COMPLETED", "Interview", 77L);
    }

    @Test
    void getFinalInterviewResult() {
        User hr = user(3L, Role.HR);
        Interview interview = interview(77L, profile(10L, user(1L, Role.INTERN), user(2L, Role.MANAGER)), InterviewStatus.COMPLETED);
        InterviewResult result = InterviewResult.builder()
                .id(500L)
                .interview(interview)
                .technicalScore(80)
                .communicationScore(75)
                .problemSolvingScore(78)
                .confidenceScore(70)
                .finalScore(76)
                .strengths("Clear basics")
                .weaknesses("Needs more examples")
                .recommendation("CONSIDER_WITH_FOLLOW_UP")
                .aiSummary("Mock result")
                .build();
        interview.setResult(result);
        Authentication authentication = authentication(hr);

        when(userService.getCurrentUser(authentication)).thenReturn(hr);
        when(interviewRepository.findById(77L)).thenReturn(Optional.of(interview));

        var response = interviewService.getResult(77L, authentication);

        assertThat(response.getInterviewId()).isEqualTo(77L);
        assertThat(response.getFinalScore()).isEqualTo(76);
        assertThat(response.getAiSummary()).isEqualTo("Mock result");
    }

    private Interview interview(Long id, InternProfile intern, InterviewStatus status) {
        return Interview.builder()
                .id(id)
                .intern(intern)
                .role("React Intern")
                .status(status)
                .scheduledAt(LocalDateTime.now().plusDays(1))
                .build();
    }

    private InterviewQuestion question(Long id, Interview interview, String text, QuestionType type, int orderNumber) {
        return InterviewQuestion.builder()
                .id(id)
                .interview(interview)
                .questionText(text)
                .questionType(type)
                .orderNumber(orderNumber)
                .build();
    }

    private InternProfile profile(Long id, User intern, User manager) {
        return InternProfile.builder()
                .id(id)
                .user(intern)
                .manager(manager)
                .status("ACTIVE")
                .build();
    }

    private Authentication authentication(User user) {
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    private User user(Long id, Role role) {
        return User.builder()
                .id(id)
                .name(role.name())
                .email(role.name().toLowerCase() + id + "@test.com")
                .password("encoded")
                .role(role)
                .active(true)
                .build();
    }
}
