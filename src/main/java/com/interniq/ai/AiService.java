package com.interniq.ai;

import com.interniq.audit.AuditLogService;
import com.interniq.ai.dto.ResumeScreeningResponse;
import com.interniq.candidate.Candidate;
import com.interniq.candidate.CandidateRepository;
import com.interniq.candidate.CandidateStatus;
import com.interniq.notification.EmailNotificationService;
import com.interniq.resume.ResumeStorageService;
import com.interniq.resume.ResumeStorageService.StoredResume;
import com.interniq.user.User;
import com.interniq.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiService {

    private final CandidateRepository candidateRepository;
    private final ResumeScreeningResultRepository screeningResultRepository;
    private final ResumeStorageService resumeStorageService;
    private final AiProviderFactory aiProviderFactory;
    private final AuditLogService auditLogService;
    private final EmailNotificationService emailNotificationService;
    private final UserService userService;

    @Transactional
    public ResumeScreeningResponse screenResume(Long candidateId, MultipartFile resumeFile) {
        return screenResume(candidateId, resumeFile, null);
    }

    @Transactional
    public ResumeScreeningResponse screenResume(Long candidateId, MultipartFile resumeFile, Authentication authentication) {
        Candidate candidate = getCandidateOrThrow(candidateId);
        StoredResume storedResume = resumeStorageService.store(candidate, resumeFile);
        ResumeScreeningResponse analysis = aiProviderFactory.getProvider().screenResume(candidate, storedResume.extractedText());

        ResumeScreeningResult result = screeningResultRepository.findByCandidateId(candidateId)
                .orElseGet(() -> ResumeScreeningResult.builder().candidate(candidate).build());

        applyAnalysis(result, analysis);

        candidate.setResumeFileName(storedResume.resumeFile().getOriginalFileName());
        candidate.setAiScore(analysis.getFinalScore());
        candidate.setAiRecommendation(analysis.getRecommendation());
        candidate.setStatus(toCandidateStatus(analysis.getFinalScore(), analysis.getRecommendation()));
        candidateRepository.save(candidate);
        auditLogService.record(actor(authentication), "RESUME_SCREENED", "Candidate", candidate.getId());
        emailNotificationService.sendResumeScreened(candidate, authentication);

        return toResponse(screeningResultRepository.save(result));
    }

    @Transactional(readOnly = true)
    public ResumeScreeningResponse getScreeningResult(Long candidateId) {
        return screeningResultRepository.findByCandidateId(candidateId)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Resume screening result not found"));
    }

    private Candidate getCandidateOrThrow(Long candidateId) {
        return candidateRepository.findById(candidateId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found"));
    }

    private void applyAnalysis(ResumeScreeningResult result, ResumeScreeningResponse analysis) {
        result.setExtractedSkills(join(analysis.getExtractedSkills()));
        result.setStrongAreas(join(analysis.getStrongAreas()));
        result.setWeakAreas(join(analysis.getWeakAreas()));
        result.setProjectQuality(analysis.getProjectQuality());
        result.setExperienceSummary(analysis.getExperienceSummary());
        result.setRoleMatchScore(analysis.getRoleMatchScore());
        result.setCommunicationScore(analysis.getCommunicationScore());
        result.setFinalScore(analysis.getFinalScore());
        result.setRecommendation(analysis.getRecommendation());
        result.setAiSummary(analysis.getAiSummary());
        result.setSuggestedInterviewQuestions(join(analysis.getSuggestedInterviewQuestions()));
        result.setProvider(analysis.getProvider());
        result.setMockResult(Boolean.TRUE.equals(analysis.getMockResult()));
    }

    private CandidateStatus toCandidateStatus(Integer score, String recommendation) {
        String normalizedRecommendation = recommendation == null ? "" : recommendation.toUpperCase();

        if (normalizedRecommendation.contains("SHORTLIST") || score != null && score >= 75) {
            return CandidateStatus.SHORTLISTED;
        }

        if (normalizedRecommendation.contains("REJECT") || score != null && score < 50) {
            return CandidateStatus.REJECTED;
        }

        return CandidateStatus.SCREENED;
    }

    private ResumeScreeningResponse toResponse(ResumeScreeningResult result) {
        Candidate candidate = result.getCandidate();

        return ResumeScreeningResponse.builder()
                .id(result.getId())
                .candidateId(candidate.getId())
                .candidateName(candidate.getName())
                .appliedRole(candidate.getAppliedRole())
                .resumeFileName(candidate.getResumeFileName())
                .candidateStatus(candidate.getStatus())
                .extractedSkills(split(result.getExtractedSkills()))
                .strongAreas(split(result.getStrongAreas()))
                .weakAreas(split(result.getWeakAreas()))
                .projectQuality(result.getProjectQuality())
                .experienceSummary(result.getExperienceSummary())
                .roleMatchScore(result.getRoleMatchScore())
                .roleMatch(result.getRoleMatchScore() == null ? "" : result.getRoleMatchScore() + "/100")
                .communicationScore(result.getCommunicationScore())
                .finalScore(result.getFinalScore())
                .recommendation(result.getRecommendation())
                .aiSummary(result.getAiSummary())
                .suggestedInterviewQuestions(split(result.getSuggestedInterviewQuestions()))
                .provider(result.getProvider())
                .mockResult(result.getMockResult())
                .createdAt(result.getCreatedAt())
                .build();
    }

    private String join(List<String> values) {
        return values == null ? "" : String.join("\n", values);
    }

    private List<String> split(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        return Arrays.stream(value.split("\\R"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private User actor(Authentication authentication) {
        return authentication == null ? null : userService.getCurrentUser(authentication);
    }
}
