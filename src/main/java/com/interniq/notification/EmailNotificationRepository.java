package com.interniq.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface EmailNotificationRepository extends JpaRepository<EmailNotification, Long>, JpaSpecificationExecutor<EmailNotification> {

    List<EmailNotification> findAllByOrderByCreatedAtDesc();
}
