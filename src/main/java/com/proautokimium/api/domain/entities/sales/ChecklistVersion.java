package com.proautokimium.api.domain.entities.sales;

import com.proautokimium.api.domain.valueObjects.sales.ChecklistContent;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/** Um envio aceito, congelado. É de onde o histórico tira o "antes". */
@jakarta.persistence.Entity
@Table(name = "checklist_versions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChecklistVersion extends com.proautokimium.api.domain.abstractions.Entity {

    @Column(name = "checklist_id", nullable = false)
    private UUID checklistId;

    @Column(name = "version", nullable = false)
    private int version;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private ChecklistContent content;

    @Column(name = "submitted_by", nullable = false, length = 100)
    private String submittedBy;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    public static ChecklistVersion of(Checklist checklist, String login, LocalDateTime now) {
        ChecklistVersion v = new ChecklistVersion();
        v.checklistId = checklist.getId();
        v.version = checklist.getVersion();
        v.content = checklist.getContent();
        v.submittedBy = login;
        v.submittedAt = now;
        return v;
    }
}
