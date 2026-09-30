package com.proautokimium.api.domain.entities.sales;

import com.proautokimium.api.domain.enums.sales.ChecklistStatus;
import com.proautokimium.api.domain.exceptions.sales.ChecklistTransitionException;
import com.proautokimium.api.domain.exceptions.sales.InvalidChecklistException;
import com.proautokimium.api.domain.valueObjects.BrazilianDocument;
import com.proautokimium.api.domain.valueObjects.sales.ChecklistContent;
import com.proautokimium.api.domain.valueObjects.sales.ChecklistRules;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * O checklist de vendas: o vendedor preenche (no celular, com ou sem internet),
 * a Controladoria confere. Ver {@link ChecklistStatus} para o fluxo.
 *
 * <p><b>O id vem do celular.</b> O checklist pode ser preenchido sem internet e
 * enviado depois de uma queda; com o id gerado no aparelho, o reenvio cai no
 * mesmo registro. Por isso esta entidade não herda a base {@code Entity}, que
 * gera o id, e implementa {@link Persistable}: sem isso o Spring Data veria um
 * id preenchido e faria {@code merge} — um SELECT a mais em todo envio.
 *
 * <p><b>Quem é quem vai por login</b> (vendedor e revisor), e não por FK de
 * funcionário: nem todo usuário da Controladoria está ligado a um funcionário,
 * e o nome fica gravado junto para a lista não depender de um JOIN.
 */
@jakarta.persistence.Entity
@Table(name = "checklists")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Checklist implements Persistable<UUID> {

    @Id
    private UUID id;

    /** O "Nº 0142" do comprovante. Vem da sequência do banco (BIGSERIAL). */
    @Generated(event = EventType.INSERT)
    @Column(name = "number", insertable = false, updatable = false)
    private Long number;

    @Column(name = "seller_login", nullable = false, length = 100)
    private String sellerLogin;

    @Column(name = "seller_name", nullable = false, length = 150)
    private String sellerName;

    @Column(name = "customer_code")
    private Integer customerCode;

    @Column(name = "customer_name", nullable = false, length = 150)
    private String customerName;

    @Column(name = "customer_document", nullable = false, length = 14)
    private String customerDocument;

    @Column(name = "new_customer", nullable = false)
    private boolean newCustomer;

    @Column(name = "has_order", nullable = false)
    private boolean hasOrder;

    @Column(name = "order_total", precision = 14, scale = 2)
    private BigDecimal orderTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ChecklistStatus status;

    /** Para onde voltar quando a Controladoria nega um pedido de alteração. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status_before_request", length = 30)
    private ChecklistStatus statusBeforeRequest;

    /** Quantos envios foram aceitos. Começa em 1. */
    @Column(name = "version", nullable = false)
    private int version;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private ChecklistContent content;

    @Column(name = "filled_offline", nullable = false)
    private boolean filledOffline;

    @Column(name = "device_started_at")
    private LocalDateTime deviceStartedAt;

    @Column(name = "first_submitted_at", nullable = false)
    private LocalDateTime firstSubmittedAt;

    @Column(name = "last_submitted_at", nullable = false)
    private LocalDateTime lastSubmittedAt;

    @Column(name = "reviewed_by_login", length = 100)
    private String reviewedByLogin;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_notes", length = 500)
    private String reviewNotes;

    @Column(name = "change_reason", length = 500)
    private String changeReason;

    @Column(name = "change_requested_at")
    private LocalDateTime changeRequestedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Transient
    private boolean isNew;

    // ── Envio ────────────────────────────────────────────────────────────────

    public static Checklist submit(UUID id, String sellerLogin, String sellerName, ChecklistContent content,
                                   boolean filledOffline, LocalDateTime deviceStartedAt, LocalDateTime now) {
        if (id == null) {
            throw new InvalidChecklistException("O checklist chegou sem identificador.");
        }
        Checklist checklist = new Checklist();
        checklist.id = id;
        checklist.isNew = true;
        checklist.sellerLogin = sellerLogin;
        checklist.sellerName = sellerName;
        checklist.version = 1;
        checklist.status = ChecklistStatus.SUBMITTED;
        checklist.filledOffline = filledOffline;
        checklist.deviceStartedAt = deviceStartedAt;
        checklist.firstSubmittedAt = now;
        checklist.apply(content, now);
        return checklist;
    }

    /**
     * O vendedor manda de novo, depois de uma devolução ou de uma alteração
     * liberada. Fora disso, mudar o que foi enviado passa pela Controladoria.
     */
    public void resubmit(String login, ChecklistContent content, LocalDateTime now) {
        ensureOwner(login);
        if (!status.editable()) {
            throw new ChecklistTransitionException(status == ChecklistStatus.CHANGE_REQUESTED
                    ? "O pedido de alteração ainda não foi respondido pela Controladoria."
                    : "Este checklist já foi enviado. Para mudar alguma coisa, peça alteração à Controladoria.");
        }
        this.version++;
        this.status = ChecklistStatus.SUBMITTED;
        this.statusBeforeRequest = null;
        this.changeReason = null;
        this.changeRequestedAt = null;
        apply(content, now);
    }

    private void apply(ChecklistContent content, LocalDateTime now) {
        List<String> problems = ChecklistRules.problems(content);
        if (!problems.isEmpty()) {
            throw new InvalidChecklistException(problems);
        }
        ChecklistContent recomputed = content.withRecomputedOrder();
        ChecklistContent.Customer customer = recomputed.customer();
        this.content = recomputed;
        this.customerCode = customer.newCustomer() ? null : customer.code();
        this.customerName = customer.name().trim();
        this.customerDocument = BrazilianDocument.digits(customer.document());
        this.newCustomer = customer.newCustomer() || customer.code() == null;
        this.hasOrder = recomputed.hasOrder();
        this.orderTotal = hasOrder ? recomputed.order().total() : null;
        this.lastSubmittedAt = now;
        this.updatedAt = now;
    }

    // ── Controladoria ────────────────────────────────────────────────────────

    public void approve(String reviewerLogin, String notes, LocalDateTime now) {
        if (status != ChecklistStatus.SUBMITTED) {
            throw new ChecklistTransitionException("Só é possível aprovar um checklist aguardando análise.");
        }
        ensureNotOwn(reviewerLogin);
        this.status = ChecklistStatus.APPROVED;
        review(reviewerLogin, notes, now);
    }

    /** Devolve para o vendedor corrigir. O motivo é obrigatório: é o que ele vai ler. */
    public void returnToSeller(String reviewerLogin, String notes, LocalDateTime now) {
        if (status != ChecklistStatus.SUBMITTED) {
            throw new ChecklistTransitionException("Só é possível devolver um checklist aguardando análise.");
        }
        ensureNotOwn(reviewerLogin);
        requireText(notes, "Explique ao vendedor o que precisa ser corrigido.");
        this.status = ChecklistStatus.RETURNED;
        review(reviewerLogin, notes, now);
    }

    // ── Pedido de alteração ──────────────────────────────────────────────────

    /** O vendedor pede para mudar o que já enviou, dizendo por quê. */
    public void requestChange(String login, String reason, LocalDateTime now) {
        ensureOwner(login);
        if (status != ChecklistStatus.SUBMITTED && status != ChecklistStatus.APPROVED) {
            throw new ChecklistTransitionException(switch (status) {
                case CHANGE_REQUESTED -> "Já existe um pedido de alteração aguardando a Controladoria.";
                case RETURNED, REOPENED -> "Este checklist já está liberado para você alterar.";
                default -> "Não é possível pedir alteração agora.";
            });
        }
        requireText(reason, "Explique o que precisa ser alterado.");
        this.statusBeforeRequest = status;
        this.status = ChecklistStatus.CHANGE_REQUESTED;
        this.changeReason = reason.strip();
        this.changeRequestedAt = now;
        this.updatedAt = now;
    }

    public void grantChange(String reviewerLogin, String notes, LocalDateTime now) {
        requireChangeRequested();
        ensureNotOwn(reviewerLogin);
        this.status = ChecklistStatus.REOPENED;
        review(reviewerLogin, notes, now);
    }

    public void denyChange(String reviewerLogin, String notes, LocalDateTime now) {
        requireChangeRequested();
        ensureNotOwn(reviewerLogin);
        requireText(notes, "Explique ao vendedor por que a alteração foi negada.");
        this.status = statusBeforeRequest == null ? ChecklistStatus.SUBMITTED : statusBeforeRequest;
        this.statusBeforeRequest = null;
        review(reviewerLogin, notes, now);
    }

    // ── Guardas ──────────────────────────────────────────────────────────────

    public boolean isOwnedBy(String login) {
        return Objects.equals(sellerLogin, login);
    }

    private void ensureOwner(String login) {
        if (!isOwnedBy(login)) {
            throw new ChecklistTransitionException("Só quem emitiu o checklist pode fazer isso.");
        }
    }

    /** Quem emitiu não confere o próprio checklist, mesmo tendo a permissão. */
    private void ensureNotOwn(String reviewerLogin) {
        if (isOwnedBy(reviewerLogin)) {
            throw new ChecklistTransitionException("Você não pode analisar um checklist emitido por você.");
        }
    }

    private void requireChangeRequested() {
        if (status != ChecklistStatus.CHANGE_REQUESTED) {
            throw new ChecklistTransitionException("Não há pedido de alteração aguardando resposta.");
        }
    }

    private void review(String reviewerLogin, String notes, LocalDateTime now) {
        this.reviewedByLogin = reviewerLogin;
        this.reviewedAt = now;
        this.reviewNotes = notes == null || notes.isBlank() ? null : notes.strip();
        this.updatedAt = now;
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new InvalidChecklistException(message);
        }
    }

    // ── Persistable ──────────────────────────────────────────────────────────

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Checklist other && Objects.equals(id, other.id));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
