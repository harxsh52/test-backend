package com.interniq.task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    List<Task> findByAssignedTo_User_IdOrderByCreatedAtDesc(Long userId);

    List<Task> findByAssignedTo_IdOrderByCreatedAtDesc(Long internId);

    List<Task> findByAssignedBy_IdOrderByCreatedAtDesc(Long assignedById);

    List<Task> findAllByOrderByCreatedAtDesc();
}
