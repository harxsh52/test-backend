package com.interniq.candidate.dto;

import com.interniq.candidate.CandidateStatus;
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
public class CandidateResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private String appliedRole;
    private String role;
    private String skills;
    private String resumeFileName;
    private CandidateStatus status;
    private String stage;
    private Integer aiScore;
    private String aiRecommendation;
    private LocalDateTime createdAt;
}
