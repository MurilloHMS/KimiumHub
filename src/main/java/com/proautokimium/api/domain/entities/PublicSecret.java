package com.proautokimium.api.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "public_secrets")
@Getter @Setter @NoArgsConstructor
public class PublicSecret extends com.proautokimium.api.domain.abstractions.Entity{
    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "encrypted_content", nullable = false)
    private byte[] encryptedContent;

    @Column(name = "iv", nullable = false)
    private byte[] iv;

    @Column(name = "auth_tag", nullable = false)
    private byte[] authTag;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
