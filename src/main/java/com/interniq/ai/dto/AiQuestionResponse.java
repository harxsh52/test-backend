package com.interniq.ai.dto;

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
public class AiQuestionResponse {

    private String questionText;
    private QuestionType questionType;
    private Integer orderNumber;
    private String provider;
    private Boolean mockResult;
}
