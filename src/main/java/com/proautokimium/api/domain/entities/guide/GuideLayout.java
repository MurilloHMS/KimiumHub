package com.proautokimium.api.domain.entities.guide;

import com.proautokimium.api.domain.abstractions.Entity;
import com.proautokimium.api.domain.enums.guide.GuideLayoutStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Uma versão do layout do Guia de Utilização.
 *
 * O ciclo é rascunho → publicado → arquivado, e as guardas moram aqui. O banco
 * garante o resto: no máximo um rascunho e no máximo um publicado, por índice
 * parcial — vale também para quem escrever direto no Postgres.
 *
 * A entidade guarda o documento como texto e não o interpreta: quem lê o JSON
 * é o validador e o montador do relatório.
 */
@jakarta.persistence.Entity
@Table(name = "guide_layouts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuideLayout extends Entity {

    @Column(name = "document", nullable = false, columnDefinition = "TEXT")
    private String document;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private GuideLayoutStatus status;

    /** Nulo enquanto é rascunho; o número nasce na publicação. */
    @Column(name = "version")
    private Integer version;

    /** O que mudou, escrito por quem publicou. */
    @Column(name = "note", length = 300)
    private String note;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Login de quem salvou por último. Nulo só no que a migration semeou. */
    @Column(name = "updated_by", length = 120)
    private String updatedBy;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "published_by", length = 120)
    private String publishedBy;

    public static GuideLayout draft(String document, String login, LocalDateTime now) {
        GuideLayout layout = new GuideLayout();
        layout.document = document;
        layout.status = GuideLayoutStatus.DRAFT;
        layout.updatedBy = login;
        layout.updatedAt = now;
        return layout;
    }

    /** Só o rascunho muda de conteúdo: o que foi publicado é registro. */
    public void updateDraft(String document, String login, LocalDateTime now) {
        requireStatus(GuideLayoutStatus.DRAFT, "Só o rascunho pode ser alterado");
        this.document = document;
        this.updatedBy = login;
        this.updatedAt = now;
    }

    public void publish(int version, String note, String login, LocalDateTime now) {
        requireStatus(GuideLayoutStatus.DRAFT, "Só o rascunho pode ser publicado");
        this.status = GuideLayoutStatus.PUBLISHED;
        this.version = version;
        this.note = note;
        this.publishedBy = login;
        this.publishedAt = now;
    }

    public void archive() {
        requireStatus(GuideLayoutStatus.PUBLISHED, "Só a versão publicada pode ser arquivada");
        this.status = GuideLayoutStatus.ARCHIVED;
    }

    private void requireStatus(GuideLayoutStatus expected, String message) {
        if (status != expected) throw new IllegalStateException(message + " (está " + status + ")");
    }
}
