package com.interniq.resume;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResumeFileRepository extends JpaRepository<ResumeFile, Long> {

    List<ResumeFile> findByCandidateIdOrderByUploadedAtDesc(Long candidateId);
}
