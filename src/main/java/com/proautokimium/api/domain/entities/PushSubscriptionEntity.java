package com.proautokimium.api.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Inscrição de Web Push de um dispositivo/navegador do usuário. */
@Entity
@Table(name = "push_subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PushSubscriptionEntity extends com.proautokimium.api.domain.abstractions.Entity {

    @Column(name = "recipient_login", nullable = false)
    private String recipientLogin;

    @Column(name = "endpoint", length = 500, nullable = false, unique = true)
    private String endpoint;

    @Column(name = "p256dh", length = 255, nullable = false)
    private String p256dh;

    @Column(name = "auth", length = 255, nullable = false)
    private String auth;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public PushSubscriptionEntity(String recipientLogin, String endpoint, String p256dh, String auth) {
        this.recipientLogin = recipientLogin;
        this.endpoint = endpoint;
        this.p256dh = p256dh;
        this.auth = auth;
        this.createdAt = LocalDateTime.now();
    }
}
