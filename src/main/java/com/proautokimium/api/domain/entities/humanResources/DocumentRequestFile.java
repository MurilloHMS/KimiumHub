package com.proautokimium.api.domain.entities.humanResources;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_request_files")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentRequestFile extends com.proautokimium.api.domain.abstractions.Entity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private DocumentRequestRecipient documentRequestRecipient;

    @Column(name = "field_key", length = 60, nullable = false)
    private String fieldKey;

    @Column(name = "original_filename", length = 255, nullable = false)
    private String originalFilename;

    @Column(name = "storage_path", length = 500, nullable = false)
    private String storagePath;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "replaced_at")
    private LocalDateTime replacedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_document_id")
    private EmployeeDocument employeeDocument;
}
