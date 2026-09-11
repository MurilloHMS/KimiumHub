package com.proautokimium.api.domain.entities.processoSeletivo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * O link que deixa o candidato voltar para ver e corrigir o que enviou.
 *
 * <p><b>Tabela própria, e não {@code FirstAccessToken}</b>: aquela tem FK para
 * {@code parceiros}, e candidato não é {@code Partner}. Anular a FK e somar
 * {@code candidato_id} criaria uma tabela com duas chaves exclusivas e um
 * "qual das duas é?" em todo caminho de leitura.
 *
 * <p><b>Guarda o hash, não o token.</b> Este link abre o dossiê pessoal
 * completo de alguém; guardado em claro, um dump da tabela ou uma linha de log
 * vira chave-mestra de todos os candidatos. É o que {@code public_secrets} já
 * faz — e o oposto do {@code first_access_token}, que é o precedente pior.
 *
 * <p><b>{@code revokedAt}, e não {@code used}.</b> A pessoa abre o link, lê,
 * procura o arquivo, sobe, salva, e volta para corrigir o telefone: são várias
 * requisições. Uso único morreria no primeiro {@code GET} e o {@code PUT} de
 * salvar responderia 410 — um erro que só aparece com gente de verdade, porque
 * quem testa lê e fecha.
 */
@Entity
@Table(name = "talent_bank_access_token")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TalentBankAccessToken extends com.proautokimium.api.domain.abstractions.Entity {

    @Column(name = "token_hash", length = 64, nullable = false, unique = true)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidato_id", nullable = false)
    private Candidato candidato;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    /**
     * Recebe o {@code agora} em vez de chamar {@code LocalDateTime.now()}:
     * mantém a entidade testável em JUnit puro, e é a assinatura que o
     * {@code FirstAccessToken} já usa.
     */
    public boolean isValid(LocalDateTime agora) {
        return revokedAt == null && expiresAt.isAfter(agora);
    }

    /** Idempotente: revogar de novo não move a data da primeira revogação. */
    public void revoke(LocalDateTime agora) {
        if (revokedAt == null) {
            this.revokedAt = agora;
        }
    }
}
