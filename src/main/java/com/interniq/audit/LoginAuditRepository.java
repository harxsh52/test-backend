package com.interniq.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoginAuditRepository extends JpaRepository<LoginAuditLog, Long> {

    List<LoginAuditLog> findAllByOrderByCreatedAtDesc();
}
