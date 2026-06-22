package com.interniq.interview.dto;

import com.interniq.interview.QuestionType;
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
public class InterviewQuestionResponse {

    private Long id;
    private String questionText;
    private QuestionType questionType;
    private Integer orderNumber;
}
