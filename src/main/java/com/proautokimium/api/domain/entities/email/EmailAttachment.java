package com.proautokimium.api.domain.entities.email;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Um anexo de e-mail da fila: o arquivo fica no disco, aqui só o endereço dele. */
@Entity
@Table(name = "email_attachments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailAttachment extends com.proautokimium.api.domain.abstractions.Entity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "email_id", nullable = false)
    private EmailQueue email;

    @Column(name = "filename", length = 255, nullable = false)
    private String filename;

    @Column(name = "content_type", length = 100, nullable = false)
    private String contentType;

    @Column(name = "storage_path", length = 500, nullable = false)
    private String storagePath;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    EmailAttachment(EmailQueue email, String filename, String contentType, String storagePath, long sizeBytes,
                    LocalDateTime now) {
        this.email = email;
        this.filename = filename;
        this.contentType = contentType;
        this.storagePath = storagePath;
        this.sizeBytes = sizeBytes;
        this.createdAt = now;
    }
}
