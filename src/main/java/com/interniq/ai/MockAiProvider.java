package com.interniq.ai;

import com.interniq.ai.dto.AiQuestionResponse;
import com.interniq.ai.dto.InterviewEvaluationResponse;
import com.interniq.ai.dto.ResumeScreeningResponse;
import com.interniq.candidate.Candidate;
import com.interniq.interview.Interview;
import com.interniq.interview.InterviewAnswer;
import com.interniq.interview.InterviewQuestion;
import com.interniq.interview.QuestionType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class MockAiProvider implements AiProvider {

    @Override
    public String name() {
        return "MOCK";
    }

    @Override
    public boolean isMock() {
        return true;
    }

    @Override
    public ResumeScreeningResponse screenResume(Candidate candidate, String resumeText) {
        String text = (resumeText == null ? "" : resumeText).toLowerCase(Locale.ROOT);
        List<String> extractedSkills = extractSkills(candidate, text);
        boolean hasProjects = containsAny(text, "project", "github", "portfolio", "built", "developed");
        boolean hasExperience = containsAny(text, "intern", "experience", "worked", "freelance", "team");
        boolean hasPortfolio = containsAny(text, "github", "portfolio", "linkedin", "vercel", "netlify");
        int skillScore = Math.min(45, extractedSkills.size() * 7);
        int roleBonus = roleMatchBonus(candidate, extractedSkills);
        int projectBonus = hasProjects ? 18 : 6;
        int experienceBonus = hasExperience ? 12 : 4;
        int portfolioBonus = hasPortfolio ? 8 : 0;
        int finalScore = Math.min(100, 25 + skillScore + roleBonus + projectBonus + experienceBonus + portfolioBonus);
        int roleMatchScore = Math.min(100, 35 + skillScore + roleBonus + projectBonus);
        int communicationScore = Math.min(100, Math.max(45, resumeText == null ? 45 : resumeText.length() / 35));

        List<String> weakAreas = new ArrayList<>();
        if (!hasProjects) weakAreas.add("Project details are limited or unclear");
        if (!hasPortfolio) weakAreas.add("No strong GitHub or portfolio signal found");
        if (!hasExperience) weakAreas.add("Experience summary needs more evidence");
        if (weakAreas.isEmpty()) weakAreas.add("Could add more measurable impact and metrics");

        String recommendation = finalScore >= 75 ? "SHORTLIST" : finalScore >= 55 ? "REVIEW" : "REJECT";

        return ResumeScreeningResponse.builder()
                .candidateId(candidate.getId())
                .candidateName(candidate.getName())
                .appliedRole(candidate.getAppliedRole())
                .resumeFileName(candidate.getResumeFileName())
                .candidateStatus(candidate.getStatus())
                .extractedSkills(extractedSkills)
                .strongAreas(List.of("Relevant skills for " + candidate.getAppliedRole(), hasProjects ? "Project work is visible" : "Found baseline technical signal"))
                .weakAreas(weakAreas)
                .projectQuality(hasProjects ? "Projects show practical implementation and role readiness." : "Project quality cannot be fully verified from the uploaded resume.")
                .experienceSummary(hasExperience ? "Resume indicates some practical or collaborative experience." : "Resume appears early-career with limited experience detail.")
                .roleMatchScore(roleMatchScore)
                .roleMatch(roleMatchScore + "/100")
                .communicationScore(communicationScore)
                .finalScore(finalScore)
                .recommendation(recommendation)
                .aiSummary("Local mock screening result generated without calling an external AI provider.")
                .suggestedInterviewQuestions(List.of(
                        "Explain one project from your resume and the main technical challenge.",
                        "How do your listed skills apply to the " + candidate.getAppliedRole() + " role?",
                        "What would you improve in your strongest project if given more time?"
                ))
                .provider(name())
                .mockResult(true)
                .build();
    }

    @Override
    public List<AiQuestionResponse> generateInterviewQuestions(Interview interview, ResumeScreeningResult screeningResult) {
        String role = interview.getRole() == null ? "intern" : interview.getRole().toLowerCase(Locale.ROOT);
        List<AiQuestionResponse> questions;

        if (role.contains("react") || role.contains("frontend")) {
            questions = new ArrayList<>(List.of(
                    question("What is useState, and when would you use it?", QuestionType.THEORY, 1),
                    question("Explain useEffect and a common mistake developers make with dependencies.", QuestionType.THEORY, 2),
                    question("What is prop drilling, and how can you reduce it?", QuestionType.SCENARIO, 3),
                    question("How do you handle API loading and error states in React?", QuestionType.SCENARIO, 4),
                    question("Debug a component that keeps calling an API repeatedly after render.", QuestionType.DEBUGGING, 5),
                    question("Explain one frontend project you built and the tradeoffs you made.", QuestionType.PROJECT_EXPLANATION, 6)
            ));
        } else {
            questions = new ArrayList<>(List.of(
                    question("Explain the strongest technical skill you bring to this role.", QuestionType.THEORY, 1),
                    question("Describe a project where you solved a difficult implementation problem.", QuestionType.PROJECT_EXPLANATION, 2),
                    question("How would you approach debugging a production issue?", QuestionType.DEBUGGING, 3),
                    question("Tell us about a time you had to learn a new tool quickly.", QuestionType.BEHAVIORAL, 4),
                    question("How do you communicate blockers to a manager or team?", QuestionType.BEHAVIORAL, 5)
            ));
        }

        if (screeningResult != null) {
            int nextOrder = questions.size() + 1;
            String strongestSkill = firstLine(screeningResult.getExtractedSkills(), "your strongest listed skill");
            String weakArea = firstLine(screeningResult.getWeakAreas(), "one improvement area from your resume");

            questions.add(question(
                    "Your resume highlights " + strongestSkill + ". Explain a concrete project where you used it and what you personally contributed.",
                    QuestionType.PROJECT_EXPLANATION,
                    nextOrder
            ));
            questions.add(question(
                    "The screening noted " + weakArea + ". How are you working to improve this area?",
                    QuestionType.BEHAVIORAL,
                    nextOrder + 1
            ));
        }

        return questions;
    }

    @Override
    public AnswerEvaluation evaluateAnswer(String answerText, InterviewQuestion question) {
        int score = scoreAnswer(answerText, question);
        return new AnswerEvaluation(score, feedbackForScore(score));
    }

    @Override
    public InterviewEvaluationResponse evaluateInterview(Interview interview) {
        List<InterviewAnswer> answers = interview.getAnswers();
        int averageAnswerScore = (int) Math.round(answers
                .stream()
                .mapToInt(answer -> answer.getScore() == null ? 0 : answer.getScore())
                .average()
                .orElse(0));

        int answeredQuestions = answers.size();
        int totalQuestions = Math.max(interview.getQuestions().size(), 1);
        int completionScore = Math.min(100, (int) Math.round((answeredQuestions * 100.0) / totalQuestions));

        int technicalScore = averageScoreForTypes(answers, QuestionType.THEORY, QuestionType.DEBUGGING, QuestionType.PROJECT_EXPLANATION, averageAnswerScore);
        int communicationScore = clamp((averageAnswerScore + completionScore + longAnswerRatioScore(answers)) / 3);
        int problemSolvingScore = averageScoreForTypes(answers, QuestionType.SCENARIO, QuestionType.DEBUGGING, QuestionType.PROJECT_EXPLANATION, averageAnswerScore);
        int confidenceScore = clamp(completionScore - 5);
        int finalScore = clamp((technicalScore + communicationScore + problemSolvingScore + confidenceScore) / 4);

        return InterviewEvaluationResponse.builder()
                .technicalScore(technicalScore)
                .communicationScore(communicationScore)
                .problemSolvingScore(problemSolvingScore)
                .confidenceScore(confidenceScore)
                .finalScore(finalScore)
                .strengths(strengthsForScore(finalScore))
                .weaknesses(weaknessesForScore(finalScore))
                .recommendation(recommendationForScore(finalScore))
                .aiSummary("Mock AI interview evaluation generated from answer depth, technical signals, clarity, problem-solving evidence, completion, and role readiness.")
                .provider(name())
                .mockResult(true)
                .build();
    }

    private AiQuestionResponse question(String text, QuestionType type, int orderNumber) {
        return AiQuestionResponse.builder()
                .questionText(text)
                .questionType(type)
                .orderNumber(orderNumber)
                .provider(name())
                .mockResult(true)
                .build();
    }

    private List<String> extractSkills(Candidate candidate, String text) {
        Map<String, String> skillMap = new HashMap<>();
        List<String> commonSkills = List.of(
                "Java", "Spring Boot", "React", "JavaScript", "TypeScript", "HTML", "CSS", "SQL",
                "MySQL", "PostgreSQL", "Git", "REST API", "Node.js", "Python", "Docker", "AWS"
        );

        for (String skill : commonSkills) {
            if (text.contains(skill.toLowerCase(Locale.ROOT))) {
                skillMap.put(skill.toLowerCase(Locale.ROOT), skill);
            }
        }

        if (candidate.getSkills() != null) {
            for (String skill : candidate.getSkills().split(",")) {
                String cleaned = skill.trim();
                if (!cleaned.isBlank()) {
                    skillMap.put(cleaned.toLowerCase(Locale.ROOT), cleaned);
                }
            }
        }

        if (skillMap.isEmpty()) {
            skillMap.put("communication", "Communication");
        }

        return new ArrayList<>(skillMap.values());
    }

    private int roleMatchBonus(Candidate candidate, List<String> skills) {
        String role = candidate.getAppliedRole() == null ? "" : candidate.getAppliedRole().toLowerCase(Locale.ROOT);
        String joinedSkills = String.join(" ", skills).toLowerCase(Locale.ROOT);

        if (role.contains("react") && joinedSkills.contains("react")) return 15;
        if (role.contains("java") && joinedSkills.contains("java")) return 15;
        if (role.contains("spring") && joinedSkills.contains("spring")) return 15;
        return 6;
    }

    private int scoreAnswer(String answerText, InterviewQuestion question) {
        String answer = answerText == null ? "" : answerText.trim();
        String normalizedAnswer = answer.toLowerCase(Locale.ROOT);
        int length = answer.length();
        int score = 25;

        if (length >= 500) score += 28;
        else if (length >= 250) score += 23;
        else if (length >= 120) score += 17;
        else if (length >= 50) score += 9;

        if (containsAny(normalizedAnswer, "because", "for example", "tradeoff", "impact", "result")) score += 12;
        if (containsAny(normalizedAnswer, "debug", "test", "api", "state", "component", "database", "algorithm", "architecture")) score += 14;
        if (containsAny(normalizedAnswer, "i built", "i implemented", "i handled", "my role", "i improved")) score += 10;
        if (containsAny(normalizedAnswer, "error", "edge case", "performance", "security", "validation")) score += 8;

        if (question.getQuestionType() == QuestionType.DEBUGGING && containsAny(normalizedAnswer, "reproduce", "logs", "inspect", "fix", "root cause")) score += 8;
        if (question.getQuestionType() == QuestionType.BEHAVIORAL && containsAny(normalizedAnswer, "communicate", "learned", "feedback", "team")) score += 8;

        return clamp(score);
    }

    private String feedbackForScore(int score) {
        if (score >= 85) {
            return "Strong answer with useful detail and clear reasoning.";
        }

        if (score >= 70) {
            return "Good answer. Add more depth, examples, and tradeoffs for a stronger response.";
        }

        if (score >= 50) {
            return "Basic answer. Needs clearer structure and more technical detail.";
        }

        return "Answer is too brief. Include concepts, examples, and reasoning.";
    }

    private int averageScoreForTypes(List<InterviewAnswer> answers, QuestionType first, QuestionType second, QuestionType third, int fallback) {
        return (int) Math.round(answers.stream()
                .filter(answer -> answer.getQuestion() != null
                        && (answer.getQuestion().getQuestionType() == first
                        || answer.getQuestion().getQuestionType() == second
                        || answer.getQuestion().getQuestionType() == third))
                .mapToInt(answer -> answer.getScore() == null ? 0 : answer.getScore())
                .average()
                .orElse(fallback));
    }

    private int longAnswerRatioScore(List<InterviewAnswer> answers) {
        if (answers.isEmpty()) {
            return 0;
        }

        long structuredAnswers = answers.stream()
                .filter(answer -> answer.getAnswerText() != null && answer.getAnswerText().trim().length() >= 120)
                .count();

        return (int) Math.round((structuredAnswers * 100.0) / answers.size());
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private String strengthsForScore(int score) {
        if (score >= 80) {
            return "Clear communication, good technical framing, and strong readiness for the role.";
        }

        if (score >= 60) {
            return "Understands the basics and can improve with more structured examples.";
        }

        return "Shows initial familiarity but needs more depth and confidence.";
    }

    private String weaknessesForScore(int score) {
        if (score >= 80) {
            return "Can still add more measurable examples and edge-case thinking.";
        }

        if (score >= 60) {
            return "Needs stronger technical depth, examples, and clearer explanation of tradeoffs.";
        }

        return "Answers were short and lacked enough technical reasoning.";
    }

    private String recommendationForScore(int score) {
        if (score >= 80) {
            return "RECOMMENDED";
        }

        if (score >= 60) {
            return "CONSIDER_WITH_FOLLOW_UP";
        }

        return "NOT_RECOMMENDED";
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private String firstLine(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .findFirst()
                .orElse(fallback);
    }
}
