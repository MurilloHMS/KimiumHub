package com.proautokimium.api.domain.entities;

import com.proautokimium.api.domain.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification extends com.proautokimium.api.domain.abstractions.Entity {

    /** Login do usuário destinatário (= User.login = subject do JWT = principal do STOMP). */
    @Column(name = "recipient_login", nullable = false)
    private String recipientLogin;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 40, nullable = false)
    private NotificationType type;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "message", length = 500, nullable = false)
    private String message;

    /** Rota do frontend para onde a notificação leva (ex.: /documentos/holerites). */
    @Column(name = "link", length = 300)
    private String link;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Notification(String recipientLogin, NotificationType type, String title, String message, String link) {
        this.recipientLogin = recipientLogin;
        this.type = type;
        this.title = title;
        this.message = message;
        this.link = link;
        this.read = false;
        this.createdAt = LocalDateTime.now();
    }
}
