package com.interniq.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interniq.ai.dto.AiQuestionResponse;
import com.interniq.ai.dto.InterviewEvaluationResponse;
import com.interniq.ai.dto.ResumeScreeningResponse;
import com.interniq.candidate.Candidate;
import com.interniq.exception.BadRequestException;
import com.interniq.interview.Interview;
import com.interniq.interview.InterviewAnswer;
import com.interniq.interview.InterviewQuestion;
import com.interniq.interview.QuestionType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAiCompatibleProvider implements AiProvider {

    private final ObjectMapper objectMapper;

    @Value("${application.ai.base-url:https://api.openai.com}")
    private String aiBaseUrl;

    @Value("${application.ai.api-key:}")
    private String apiKey;

    @Value("${application.ai.model:gpt-4o-mini}")
    private String model;

    @Override
    public String name() {
        return "OPENAI_COMPATIBLE";
    }

    @Override
    public boolean isMock() {
        return false;
    }

    @Override
    public ResumeScreeningResponse screenResume(Candidate candidate, String resumeText) {
        try {
            Map<String, Object> json = requestJson(buildResumePrompt(candidate, resumeText));

            return ResumeScreeningResponse.builder()
                    .candidateId(candidate.getId())
                    .candidateName(candidate.getName())
                    .appliedRole(candidate.getAppliedRole())
                    .resumeFileName(candidate.getResumeFileName())
                    .candidateStatus(candidate.getStatus())
                    .extractedSkills(toStringList(json.get("extractedSkills")))
                    .strongAreas(toStringList(json.get("strongAreas")))
                    .weakAreas(toStringList(json.get("weakAreas")))
                    .projectQuality(stringValue(json.get("projectQuality")))
                    .experienceSummary(stringValue(json.get("experienceSummary")))
                    .roleMatchScore(intValue(json.get("roleMatchScore")))
                    .roleMatch(stringValue(json.get("roleMatch")))
                    .communicationScore(intValue(json.get("communicationScore")))
                    .finalScore(intValue(json.get("finalScore")))
                    .recommendation(stringValue(json.get("recommendation")))
                    .aiSummary(stringValue(json.get("aiSummary")))
                    .suggestedInterviewQuestions(toStringList(json.get("suggestedInterviewQuestions")))
                    .provider(name())
                    .mockResult(false)
                    .build();
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("AI provider failed to screen the resume. Check AI_BASE_URL, AI_MODEL, provider response format, and network access.");
        }
    }

    @Override
    public List<AiQuestionResponse> generateInterviewQuestions(Interview interview, ResumeScreeningResult screeningResult) {
        try {
            Map<String, Object> json = requestJson(buildQuestionPrompt(interview, screeningResult));
            List<Map<String, Object>> questionMaps = toMapList(json.get("questions"));
            List<AiQuestionResponse> questions = new ArrayList<>();
            int order = 1;

            for (Map<String, Object> questionMap : questionMaps) {
                questions.add(AiQuestionResponse.builder()
                        .questionText(stringValue(questionMap.get("questionText")))
                        .questionType(questionType(stringValue(questionMap.get("questionType"))))
                        .orderNumber(intValue(questionMap.get("orderNumber"), order))
                        .provider(name())
                        .mockResult(false)
                        .build());
                order++;
            }

            if (questions.isEmpty()) {
                throw new BadRequestException("AI provider returned no interview questions.");
            }

            return questions;
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("AI provider failed to generate interview questions. Check provider configuration and response format.");
        }
    }

    @Override
    public AnswerEvaluation evaluateAnswer(String answerText, InterviewQuestion question) {
        try {
            Map<String, Object> json = requestJson(buildAnswerPrompt(answerText, question));
            return new AnswerEvaluation(
                    intValue(json.get("score")),
                    stringValue(json.get("feedback"))
            );
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("AI provider failed to evaluate the interview answer.");
        }
    }

    @Override
    public InterviewEvaluationResponse evaluateInterview(Interview interview) {
        try {
            Map<String, Object> json = requestJson(buildInterviewEvaluationPrompt(interview));

            return InterviewEvaluationResponse.builder()
                    .technicalScore(intValue(json.get("technicalScore")))
                    .communicationScore(intValue(json.get("communicationScore")))
                    .problemSolvingScore(intValue(json.get("problemSolvingScore")))
                    .confidenceScore(intValue(json.get("confidenceScore")))
                    .finalScore(intValue(json.get("finalScore")))
                    .strengths(stringValue(json.get("strengths")))
                    .weaknesses(stringValue(json.get("weaknesses")))
                    .recommendation(stringValue(json.get("recommendation")))
                    .aiSummary(stringValue(json.get("aiSummary")))
                    .provider(name())
                    .mockResult(false)
                    .build();
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("AI provider failed to complete the interview evaluation.");
        }
    }

    private Map<String, Object> requestJson(String prompt) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BadRequestException("AI_API_KEY is required when AI_PROVIDER=openai-compatible.");
        }

        Map<String, Object> request = Map.of(
                "model", model,
                "temperature", 0.2,
                "messages", List.of(
                        Map.of("role", "system", "content", "You are InternIQ's structured AI assessment engine. Return only valid JSON."),
                        Map.of("role", "user", "content", prompt)
                )
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> response = RestClient.builder()
                .baseUrl(aiBaseUrl)
                .build()
                .post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(request)
                .retrieve()
                .body(Map.class);

        String content = extractMessageContent(response);
        String json = content.trim();
        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');

        if (start >= 0 && end > start) {
            json = json.substring(start, end + 1);
        }

        return objectMapper.readValue(json, new TypeReference<>() {
        });
    }

    private String buildResumePrompt(Candidate candidate, String resumeText) {
        return """
                Analyze this resume for InternIQ candidate screening.

                Candidate:
                Name: %s
                Applied role: %s
                Declared skills: %s

                Evaluate skills, projects, experience, GitHub or portfolio mentions, role match, strong areas, weak areas, red flags, final score out of 100, recommendation, and suggested interview questions.

                Return this exact JSON shape:
                {
                  "extractedSkills": ["..."],
                  "strongAreas": ["..."],
                  "weakAreas": ["..."],
                  "projectQuality": "...",
                  "experienceSummary": "...",
                  "roleMatchScore": 0,
                  "roleMatch": "...",
                  "communicationScore": 0,
                  "finalScore": 0,
                  "recommendation": "SHORTLIST | REVIEW | REJECT",
                  "aiSummary": "...",
                  "suggestedInterviewQuestions": ["..."]
                }

                Resume text:
                %s
                """.formatted(
                candidate.getName(),
                candidate.getAppliedRole(),
                candidate.getSkills(),
                truncate(resumeText, 15_000)
        );
    }

    private String buildQuestionPrompt(Interview interview, ResumeScreeningResult screeningResult) {
        return """
                Generate concise text-based interview questions for InternIQ.

                Role: %s
                Resume skills: %s
                Resume weak areas: %s

                Include theory, scenario, debugging, project explanation, and behavioral coverage where relevant.

                Return this exact JSON shape:
                {
                  "questions": [
                    {
                      "questionText": "...",
                      "questionType": "THEORY | SCENARIO | DEBUGGING | PROJECT_EXPLANATION | BEHAVIORAL",
                      "orderNumber": 1
                    }
                  ]
                }
                """.formatted(
                interview.getRole(),
                screeningResult == null ? "" : screeningResult.getExtractedSkills(),
                screeningResult == null ? "" : screeningResult.getWeakAreas()
        );
    }

    private String buildAnswerPrompt(String answerText, InterviewQuestion question) {
        return """
                Evaluate one InternIQ interview answer.

                Question type: %s
                Question: %s
                Answer: %s

                Evaluate technical correctness, clarity, confidence, problem-solving, depth, and role readiness.

                Return this exact JSON shape:
                {
                  "score": 0,
                  "feedback": "..."
                }
                """.formatted(
                question.getQuestionType(),
                question.getQuestionText(),
                truncate(answerText, 6_000)
        );
    }

    private String buildInterviewEvaluationPrompt(Interview interview) {
        StringBuilder answers = new StringBuilder();
        for (InterviewAnswer answer : interview.getAnswers()) {
            answers.append("Question: ")
                    .append(answer.getQuestion() == null ? "" : answer.getQuestion().getQuestionText())
                    .append("\nAnswer: ")
                    .append(answer.getAnswerText())
                    .append("\nScore: ")
                    .append(answer.getScore())
                    .append("\n\n");
        }

        return """
                Generate the final InternIQ AI interview result.

                Role: %s
                Answers:
                %s

                Return this exact JSON shape:
                {
                  "technicalScore": 0,
                  "communicationScore": 0,
                  "problemSolvingScore": 0,
                  "confidenceScore": 0,
                  "finalScore": 0,
                  "strengths": "...",
                  "weaknesses": "...",
                  "recommendation": "RECOMMENDED | CONSIDER_WITH_FOLLOW_UP | NOT_RECOMMENDED",
                  "aiSummary": "..."
                }
                """.formatted(interview.getRole(), truncate(answers.toString(), 12_000));
    }

    @SuppressWarnings("unchecked")
    private String extractMessageContent(Map<String, Object> response) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new BadRequestException("AI provider response did not include choices.");
        }

        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        Object content = message == null ? null : message.get("content");
        if (content == null) {
            throw new BadRequestException("AI provider response did not include message content.");
        }

        return content.toString();
    }

    private List<String> toStringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }

        if (value == null) {
            return List.of();
        }

        return List.of(value.toString());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> toMapList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> (Map<String, Object>) item)
                    .toList();
        }

        return List.of();
    }

    private QuestionType questionType(String value) {
        try {
            return QuestionType.valueOf(value);
        } catch (Exception ex) {
            return QuestionType.THEORY;
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private int intValue(Object value) {
        return intValue(value, 0);
    }

    private int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }

        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String truncate(String value, int limit) {
        if (value == null) {
            return "";
        }

        return value.length() <= limit ? value : value.substring(0, limit);
    }
}
