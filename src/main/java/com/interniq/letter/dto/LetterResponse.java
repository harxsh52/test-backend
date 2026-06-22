package com.interniq.letter.dto;

import com.interniq.letter.LetterStatus;
import com.interniq.letter.LetterType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LetterResponse {

    private Long id;
    private String candidateName;
    private String candidateEmail;
    private Long candidateId;
    private Long internId;
    private LetterType letterType;
    private LetterStatus status;
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
    private String subject;
    private String bodyHtml;
    private String bodyText;
    private Long generatedById;
    private String generatedByName;
    private LocalDateTime sentAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime rejectedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
