package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByRecipientLoginOrderByCreatedAtDesc(String recipientLogin);

    long countByRecipientLoginAndReadFalse(String recipientLogin);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.recipientLogin = :login AND n.read = false")
    int markAllReadByRecipient(@Param("login") String login);
}
