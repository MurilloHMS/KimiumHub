package com.proautokimium.api.domain.entities.email;

import com.proautokimium.api.domain.enums.EmailStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_queue")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailQueue extends com.proautokimium.api.domain.abstractions.Entity{
    @Column(name = "to_email", nullable = false)
    private String toEmail;
    @Column(name = "reply_to")
    private String replyTo;
    @Column(name = "from_email", nullable = false)
    private String fromEmail;
    @Column(name = "subject", nullable = false)
    private String subject;
    @Column(name = "body" ,columnDefinition = "TEXT", nullable = false)
    private String body;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EmailStatus status;
    @Column(name = "attempts")
    private int attempts;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
}