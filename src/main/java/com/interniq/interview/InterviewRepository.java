package com.interniq.interview;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    List<Interview> findAllByOrderByScheduledAtDesc();

    List<Interview> findByIntern_User_IdOrderByScheduledAtDesc(Long userId);

    List<Interview> findByIntern_Manager_IdOrderByScheduledAtDesc(Long managerId);
}
