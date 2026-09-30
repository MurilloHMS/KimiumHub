package com.proautokimium.api.domain.entities.sales;

import com.proautokimium.api.domain.enums.sales.ChecklistEventType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/** Um fato da linha do tempo: quem fez o quê, quando, e por quê. */
@jakarta.persistence.Entity
@Table(name = "checklist_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChecklistEvent extends com.proautokimium.api.domain.abstractions.Entity {

    @Column(name = "checklist_id", nullable = false)
    private UUID checklistId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private ChecklistEventType type;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "actor_login", nullable = false, length = 100)
    private String actorLogin;

    @Column(name = "actor_name", nullable = false, length = 150)
    private String actorName;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static ChecklistEvent of(Checklist checklist, ChecklistEventType type, String actorLogin,
                                    String actorName, String notes, LocalDateTime now) {
        ChecklistEvent e = new ChecklistEvent();
        e.checklistId = checklist.getId();
        e.type = type;
        e.version = checklist.getVersion();
        e.actorLogin = actorLogin;
        e.actorName = actorName;
        e.notes = notes == null || notes.isBlank() ? null : notes.strip();
        e.createdAt = now;
        return e;
    }
}
