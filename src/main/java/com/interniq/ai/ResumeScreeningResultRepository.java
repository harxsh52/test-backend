package com.interniq.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResumeScreeningResultRepository extends JpaRepository<ResumeScreeningResult, Long> {

    Optional<ResumeScreeningResult> findByCandidateId(Long candidateId);
}
