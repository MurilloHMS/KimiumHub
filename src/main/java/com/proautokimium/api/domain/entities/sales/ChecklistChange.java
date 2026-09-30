package com.proautokimium.api.domain.entities.sales;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Um campo que mudou de uma versão para a outra. {@code field} já vem legível
 * ("Dados do contrato › CPF de quem assina"), porque é para gente ler.
 */
@jakarta.persistence.Entity
@Table(name = "checklist_changes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChecklistChange extends com.proautokimium.api.domain.abstractions.Entity {

    @Column(name = "checklist_id", nullable = false)
    private UUID checklistId;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "field", nullable = false, length = 300)
    private String field;

    @Column(name = "old_value", columnDefinition = "text")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "text")
    private String newValue;

    @Column(name = "changed_by", nullable = false, length = 100)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    public static ChecklistChange of(UUID checklistId, int version, String field, String oldValue, String newValue,
                                     String changedBy, LocalDateTime changedAt) {
        ChecklistChange c = new ChecklistChange();
        c.checklistId = checklistId;
        c.version = version;
        c.field = field.length() > 300 ? field.substring(0, 300) : field;
        c.oldValue = oldValue;
        c.newValue = newValue;
        c.changedBy = changedBy;
        c.changedAt = changedAt;
        return c;
    }
}
