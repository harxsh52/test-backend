package com.interniq.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientUser_IdOrderByCreatedAtDesc(Long recipientUserId);

    Optional<Notification> findByIdAndRecipientUser_Id(Long id, Long recipientUserId);

    long countByRecipientUser_IdAndStatus(Long recipientUserId, NotificationStatus status);

    List<Notification> findByRecipientUser_IdAndStatusOrderByCreatedAtDesc(Long recipientUserId, NotificationStatus status);

    List<Notification> findAllByOrderByCreatedAtDesc();
}
