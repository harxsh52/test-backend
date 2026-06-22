package com.interniq.feedback;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findByIntern_User_IdOrderByCreatedAtDesc(Long userId);

    List<Feedback> findByIntern_IdOrderByCreatedAtDesc(Long internId);
}
