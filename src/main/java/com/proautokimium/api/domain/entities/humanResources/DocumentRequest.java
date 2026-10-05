package com.proautokimium.api.domain.entities.humanResources;

import com.proautokimium.api.domain.enums.humanResources.RequestStatus;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidRequestDataException;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidStatusTransitionException;
import com.proautokimium.api.domain.valueObjects.humanResources.RequestField;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "document_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentRequest extends com.proautokimium.api.domain.abstractions.Entity{

    @Column(name = "title", length = 120, nullable = false)
    private String title;

    @Column(name = "instructions")
    private String instructions;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10, nullable = false)
    private RequestStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "form", columnDefinition = "jsonb", nullable = false)
    private List<RequestField> form = new ArrayList<>();

    @Column(name = "template_filename", length = 255)
    private String templateFilename;

    @Column(name = "template_path", length = 500)
    private String templatePath;

    @Column(name = "created_by", length = 100, nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    // Constructor
    private DocumentRequest(String title, String createdBy, LocalDateTime now){
        this.title = title.strip();
        this.createdBy = createdBy;
        this.createdAt = now;
        this.status = RequestStatus.DRAFT;
    }

    // Methods
    public static DocumentRequest draft(String title, String createdBy, LocalDateTime now){
        if(title == null || title.isBlank()) throw new InvalidRequestDataException("Dê um título para a solicitação.");
        return new DocumentRequest(title, createdBy, now);
    }

    public void send(LocalDateTime now){
        if(status != RequestStatus.DRAFT) throw new InvalidStatusTransitionException("Só um rascunho pode ser enviado.");

        if(form.isEmpty()) throw new InvalidRequestDataException("Adicione pelo menos um campo antes de enviar.");

        this.status = RequestStatus.OPEN;
        this.sentAt = now;
    }

    public void close(LocalDateTime now){
        if(status != RequestStatus.OPEN) throw new InvalidStatusTransitionException("Só uma solicitação aberta pode ser encerrada");
        this.closedAt = now;
        this.status = RequestStatus.CLOSED;
    }
}
