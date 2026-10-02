package com.proautokimium.api.domain.entities.guide;

import com.proautokimium.api.domain.abstractions.Entity;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Uma imagem que o designer enviou para pôr no cabeçalho ou no rodapé.
 *
 * Fica no banco, e não em disco, porque é pequena (no máximo 2 MB) e é parte
 * do layout: um rascunho restaurado de meses atrás precisa achar a imagem que
 * apontava, e uma pasta pode ter sido limpa nesse meio-tempo. Ninguém apaga:
 * uma versão arquivada pode voltar a usá-la.
 */
@jakarta.persistence.Entity
@Table(name = "guide_layout_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuideLayoutImage extends Entity {

    /** O tamanho só pesa no H2 dos testes; no Postgres a coluna é bytea. */
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "content", nullable = false, length = 2 * 1024 * 1024)
    private byte[] content;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "original_filename", length = 200)
    private String originalFilename;

    @Column(name = "width", nullable = false)
    private int width;

    @Column(name = "height", nullable = false)
    private int height;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 120)
    private String createdBy;

    public GuideLayoutImage(byte[] content, String contentType, String originalFilename,
                            int width, int height, String createdBy, LocalDateTime createdAt) {
        this.content = content;
        this.contentType = contentType;
        this.originalFilename = originalFilename;
        this.width = width;
        this.height = height;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }
}
