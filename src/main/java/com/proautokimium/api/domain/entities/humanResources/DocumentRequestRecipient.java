package com.proautokimium.api.domain.entities.humanResources;

import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.enums.humanResources.RecipientStatus;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidRequestDataException;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidStatusTransitionException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "document_request_recipients")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentRequestRecipient extends com.proautokimium.api.domain.abstractions.Entity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private DocumentRequest documentRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "status",length = 10, nullable = false)
    private RecipientStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answers", columnDefinition = "jsonb")
    private Map<String, Object> answers = new HashMap<>();

    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "return_reason", length = 500)
    private String returnReason;

    @Column(name = "registered_by", length = 100)
    private String registeredBy;

    // Constructor
    private DocumentRequestRecipient(DocumentRequest request, Employee employee, LocalDateTime now){
        this.documentRequest = request;
        this.employee = employee;
        this.addedAt = now;
        this.status = RecipientStatus.PENDING;
    }

    // Methods
    public static DocumentRequestRecipient create(DocumentRequest request, Employee employee, LocalDateTime now){
        if(request == null) throw new InvalidRequestDataException("Deve informar um documento");
        if(employee == null) throw new InvalidRequestDataException("Deve informar um funcionário");

        return new DocumentRequestRecipient(request, employee, now);
    }

    public void submit(Map<String, Object> answers, LocalDateTime now){
        if(status != RecipientStatus.PENDING && status != RecipientStatus.RETURNED) throw new InvalidStatusTransitionException("Só pendente ou retornado pode ser respondido");

        this.answers = new HashMap<>(answers);
        this.submittedAt = now;
        this.status = RecipientStatus.SUBMITTED;
        // Quem responde pelo portal é o próprio funcionário: um registro antigo
        // do RH (antes de uma devolução) deixa de valer.
        this.registeredBy = null;
    }

    /**
     * O RH responde no lugar do funcionário (quem não tem acesso ao portal, ou
     * entregou em papel). Mesma regra do {@link #submit}, mais quem registrou.
     */
    public void registerOnBehalf(Map<String, Object> answers, String registrar, LocalDateTime now){
        // Faltar quem registrou é dado inválido (400), não estado errado (409).
        if(registrar == null || registrar.isBlank()) throw new InvalidRequestDataException("Informe quem registrou a resposta.");

        // A guarda de status mora num lugar só. Gravar quem registrou DEPOIS:
        // o submit limpa o campo.
        submit(answers, now);
        this.registeredBy = registrar;
    }

    public void approve(String reviewer, LocalDateTime now){
        if(status != RecipientStatus.SUBMITTED) throw new InvalidStatusTransitionException("Só enviado pode ser aprovado");

        this.reviewedBy = reviewer;
        this.reviewedAt = now;
        this.status = RecipientStatus.APPROVED;
    }

    public void giveBack(String reviewer, String reason, LocalDateTime now){
        if(status != RecipientStatus.SUBMITTED) throw new InvalidStatusTransitionException("Só enviado pode retornar");
        if(reason == null || reason.isBlank()) throw new InvalidRequestDataException("É necessário preencher o motivo");

        this.reviewedBy = reviewer;
        this.reviewedAt = now;
        this.returnReason = reason;
        this.status = RecipientStatus.RETURNED;
    }


}
