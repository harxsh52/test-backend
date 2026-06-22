package com.interniq.interview.dto;

import com.interniq.interview.InterviewStatus;
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
public class InterviewResponse {

    private Long id;
    private Long candidateId;
    private String candidateName;
    private Long internId;
    private String internName;
    private String role;
    private InterviewStatus status;
    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Integer finalScore;
    private String recommendation;
    private List<InterviewQuestionResponse> questions;
    private List<InterviewAnswerResponse> answers;
    private InterviewResultResponse result;
}
