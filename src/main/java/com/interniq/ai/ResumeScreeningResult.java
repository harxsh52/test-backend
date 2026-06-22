package com.interniq.ai;

import com.interniq.candidate.Candidate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "resume_screening_results")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeScreeningResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false, unique = true)
    private Candidate candidate;

    @Column(columnDefinition = "TEXT")
    private String extractedSkills;

    @Column(columnDefinition = "TEXT")
    private String strongAreas;

    @Column(columnDefinition = "TEXT")
    private String weakAreas;

    @Column(columnDefinition = "TEXT")
    private String projectQuality;

    @Column(columnDefinition = "TEXT")
    private String experienceSummary;

    private Integer roleMatchScore;

    private Integer communicationScore;

    private Integer finalScore;

    @Column(columnDefinition = "TEXT")
    private String recommendation;

    @Column(columnDefinition = "LONGTEXT")
    private String aiSummary;

    @Column(columnDefinition = "TEXT")
    private String suggestedInterviewQuestions;

    private String provider;

    private Boolean mockResult;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
