package com.interniq.ai.dto;

import com.interniq.candidate.CandidateStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeScreeningResponse {

    private Long id;
    private Long candidateId;
    private String candidateName;
    private String appliedRole;
    private String resumeFileName;
    private CandidateStatus candidateStatus;
    private List<String> extractedSkills;
    private List<String> strongAreas;
    private List<String> weakAreas;
    private String projectQuality;
    private String experienceSummary;
    private Integer roleMatchScore;
    private String roleMatch;
    private Integer communicationScore;
    private Integer finalScore;
    private String recommendation;
    private String aiSummary;
    private List<String> suggestedInterviewQuestions;
    private String provider;
    private Boolean mockResult;
    private LocalDateTime createdAt;
}
