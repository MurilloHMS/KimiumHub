package com.proautokimium.api.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * O layout da assinatura de e-mail.
 *
 * Era um `.jrxml` no repositório, e mudar uma cor exigia redeploy. Agora é uma
 * linha aqui, e quem edita é o Design e o Marketing, pela tela.
 *
 * A entidade não conhece o formato do documento de propósito: ela guarda e
 * devolve texto. O desenho acontece no navegador, e é lá que o formato importa
 * — decompor o JSON em colunas aqui só criaria um segundo lugar para ele mudar,
 * e uma migration a cada campo novo que o designer inventasse.
 */
@Table(name = "email_signature_template")
@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailSignatureTemplate extends com.proautokimium.api.domain.abstractions.Entity {

    /**
     * O documento JSON inteiro: tamanho da tela, a arte de fundo e as caixas de
     * texto com posição, fonte e cor.
     */
    @Column(name = "document", nullable = false, columnDefinition = "TEXT")
    private String document;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Quem salvou por último. Nulo só no que a migration semeou. */
    @Column(name = "updated_by")
    private String updatedBy;

    /**
     * A trava de linha única, e o motivo de ser uma coluna.
     *
     * O id é UUID, então não dá para prender a linha pela chave primária. Esta
     * coluna é `UNIQUE` e o CHECK só aceita `TRUE` — uma segunda linha esbarra
     * no índice. Fica sendo regra de banco, e não de código: vale também para
     * quem escrever direto no Postgres.
     */
    @Column(name = "singleton", nullable = false)
    private Boolean singleton = true;

    /**
     * Grava um documento novo, deixando o rastro de quem e quando.
     *
     * Os três andam juntos porque só fazem sentido juntos: gravar o texto sem o
     * autor e a data é o que faz "a assinatura mudou e ninguém sabe por quê"
     * não ter por onde começar.
     */
    public void alterar(String document, String usuario, LocalDateTime agora) {
        this.document = document;
        this.updatedBy = usuario;
        this.updatedAt = agora;
    }
}
