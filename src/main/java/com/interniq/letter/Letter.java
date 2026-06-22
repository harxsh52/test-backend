package com.interniq.letter;

import com.interniq.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "letters")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Letter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String candidateName;

    @Column(nullable = false)
    private String candidateEmail;

    private Long candidateId;

    private Long internId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private LetterType letterType;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private LetterStatus status = LetterStatus.GENERATED;

    private String roleName;

    private String department;

    private LocalDate joiningDate;

    private LocalDate internshipStartDate;

    private LocalDate internshipEndDate;

    private BigDecimal stipend;

    private String workLocation;

    private String reportingManager;

    private String companyName;

    private String hrName;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String bodyHtml;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String bodyText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by_id")
    private User generatedBy;

    private LocalDateTime sentAt;

    private LocalDateTime acceptedAt;

    private LocalDateTime rejectedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        normalize();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        normalize();
    }

    private void normalize() {
        if (candidateEmail != null) {
            candidateEmail = candidateEmail.trim().toLowerCase();
        }
    }
}
