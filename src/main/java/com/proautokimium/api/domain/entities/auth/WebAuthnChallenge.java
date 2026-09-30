package com.proautokimium.api.domain.entities.auth;

import com.proautokimium.api.domain.enums.WebAuthnChallengePurpose;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * O número aleatório que o servidor manda e o aparelho assina. Vale uma vez e
 * por pouco tempo: é o que impede reaproveitar uma resposta capturada.
 *
 * <p>Sem setters: nasce por uma das duas fábricas, e a única mudança possível
 * depois é {@link #markUsed}. O desafio de login não tem como receber usuário,
 * e o de cadastro não tem como ficar sem — a mesma regra da constraint
 * {@code ck_webauthn_challenges_registration_has_user}, agora antes do banco.
 */
@Entity
@Table(name = "webauthn_challenges")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WebAuthnChallenge extends com.proautokimium.api.domain.abstractions.Entity {

    /** Tempo para a pessoa encostar o dedo. */
    public static final Duration TTL = Duration.ofMinutes(5);

    @Column(name = "challenge", nullable = false)
    private byte[] challenge;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 20)
    private WebAuthnChallengePurpose purpose;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    private WebAuthnChallenge(byte[] challenge, WebAuthnChallengePurpose purpose, String userId, LocalDateTime now) {
        this.challenge = challenge;
        this.purpose = purpose;
        this.userId = userId;
        this.createdAt = now;
        this.expiresAt = now.plus(TTL);
    }

    /** Cadastro: a pessoa já entrou com a senha, e o desafio é dela. */
    public static WebAuthnChallenge forRegistration(String userId, byte[] challenge, LocalDateTime now) {
        return new WebAuthnChallenge(challenge, WebAuthnChallengePurpose.REGISTRATION, userId, now);
    }

    /** Login: ninguém sabe ainda quem é; quem diz é a assinatura, na volta. */
    public static WebAuthnChallenge forAuthentication(byte[] challenge, LocalDateTime now) {
        return new WebAuthnChallenge(challenge, WebAuthnChallengePurpose.AUTHENTICATION, null, now);
    }

    /** No instante exato de {@code expiresAt} já não vale: {@code isAfter} é estrito. */
    public boolean isValid(LocalDateTime now) {
        return usedAt == null && expiresAt.isAfter(now);
    }

    /** Serve para este uso, desta pessoa? No login o dono é desconhecido: {@code userId} nulo. */
    public boolean isFor(WebAuthnChallengePurpose purpose, String userId) {
        return this.purpose == purpose && java.util.Objects.equals(this.userId, userId);
    }

    public void markUsed(LocalDateTime now) {
        this.usedAt = now;
    }
}
