package com.interniq.ai;

import com.interniq.ai.dto.AiQuestionResponse;
import com.interniq.ai.dto.InterviewEvaluationResponse;
import com.interniq.ai.dto.ResumeScreeningResponse;
import com.interniq.candidate.Candidate;
import com.interniq.interview.Interview;
import com.interniq.interview.InterviewQuestion;

import java.util.List;

public interface AiProvider {

    String name();

    boolean isMock();

    ResumeScreeningResponse screenResume(Candidate candidate, String resumeText);

    List<AiQuestionResponse> generateInterviewQuestions(Interview interview, ResumeScreeningResult screeningResult);

    AnswerEvaluation evaluateAnswer(String answerText, InterviewQuestion question);

    InterviewEvaluationResponse evaluateInterview(Interview interview);

    record AnswerEvaluation(Integer score, String feedback) {
    }
}
