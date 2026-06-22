package com.interniq.interview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewAnswerResponse {

    private Long id;
    private Long questionId;
    private String answerText;
    private Integer score;
    private String feedback;
    private LocalDateTime submittedAt;
}
