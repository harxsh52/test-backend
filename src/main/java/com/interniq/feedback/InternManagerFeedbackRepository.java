package com.interniq.feedback;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InternManagerFeedbackRepository extends JpaRepository<InternManagerFeedback, Long> {

    List<InternManagerFeedback> findByIntern_IdOrderByCreatedAtDesc(Long internId);
}
