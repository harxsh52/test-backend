package com.interniq.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewEvaluationResponse {

    private Integer technicalScore;
    private Integer communicationScore;
    private Integer problemSolvingScore;
    private Integer confidenceScore;
    private Integer finalScore;
    private String strengths;
    private String weaknesses;
    private String recommendation;
    private String aiSummary;
    private String provider;
    private Boolean mockResult;
}
