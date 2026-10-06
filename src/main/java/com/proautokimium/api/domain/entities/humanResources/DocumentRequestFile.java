package com.proautokimium.api.domain.entities.humanResources;

import com.proautokimium.api.domain.exceptions.humanResources.InvalidRequestDataException;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidStatusTransitionException;
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

    // Constructor
    private DocumentRequestFile(DocumentRequestRecipient recipient, String fieldKey, String originalFilename,
                                String storagePath, LocalDateTime now){

        this.documentRequestRecipient = recipient;
        this.fieldKey = fieldKey;
        this.originalFilename = originalFilename;
        this.storagePath = storagePath;
        this.uploadedAt = now;
    }

    // methods
    public static DocumentRequestFile create(DocumentRequestRecipient recipient, String fieldKey, String originalFilename, String storagePath, LocalDateTime now){
        if(recipient == null ) throw new InvalidRequestDataException("Informe a resposta");
        if(fieldKey == null || fieldKey.isBlank()) throw new InvalidRequestDataException("Necessário informar campo chave");
        if(originalFilename == null || originalFilename.isBlank()) throw new InvalidRequestDataException("Informe o nome do arquivo");
        if(storagePath == null || storagePath.isBlank()) throw new InvalidRequestDataException("Informe onde o arquivo foi salvo");

        return new DocumentRequestFile(recipient, fieldKey, originalFilename, storagePath, now);
    }

    public void replace(LocalDateTime now){
        // A data diz QUANDO deixou de valer; substituir de novo apagaria essa história.
        if(replacedAt != null) throw new InvalidStatusTransitionException("Este arquivo já foi substituído");
        this.replacedAt = now;
    }

    public void linkTo(EmployeeDocument document){
        if(document == null) throw new InvalidRequestDataException("Necessário informar um documento válido");
        if(employeeDocument != null) throw new InvalidStatusTransitionException("Esse arquivo já foi vinculado a um documento");

        this.employeeDocument = document;
    }
}
