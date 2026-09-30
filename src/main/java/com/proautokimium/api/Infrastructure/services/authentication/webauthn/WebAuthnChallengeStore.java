package com.proautokimium.api.Infrastructure.services.authentication.webauthn;

import com.proautokimium.api.Infrastructure.repositories.WebAuthnChallengeRepository;
import com.proautokimium.api.domain.entities.auth.WebAuthnChallenge;
import com.proautokimium.api.domain.enums.WebAuthnChallengePurpose;
import com.proautokimium.api.domain.exceptions.auth.WebAuthnChallengeExpiredException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Emite e consome os desafios.
 *
 * <h2>Por que fora do WebAuthnService</h2>
 *
 * O desafio precisa ficar gasto MESMO quando a verificação falha — senão quem
 * tenta e erra pode tentar de novo com o mesmo desafio. Dentro da transação da
 * verificação, a exceção desfaria a marca de uso. {@link #consume} roda em
 * transação própria ({@code REQUIRES_NEW}) e grava antes de a verificação
 * começar; e só funciona por estar em outro bean — chamada de um método da
 * mesma classe não passa pelo proxy do Spring, e a anotação seria ignorada.
 */
@Component
public class WebAuthnChallengeStore {

    /** 32 bytes: 256 bits de aleatoriedade, bem acima dos 16 que a norma pede. */
    static final int CHALLENGE_BYTES = 32;

    private final WebAuthnChallengeRepository repository;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public WebAuthnChallengeStore(WebAuthnChallengeRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public WebAuthnChallenge issueForRegistration(String userId) {
        return repository.save(WebAuthnChallenge.forRegistration(userId, randomBytes(), now()));
    }

    @Transactional
    public WebAuthnChallenge issueForAuthentication() {
        return repository.save(WebAuthnChallenge.forAuthentication(randomBytes(), now()));
    }

    /**
     * Devolve o desafio já marcado como usado, ou recusa. Mesma recusa para os
     * quatro casos (não existe, venceu, já usado, é de outro pedido): a saída de
     * quem tem o pedido legítimo é a mesma, pedir outro.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WebAuthnChallenge consume(UUID id, WebAuthnChallengePurpose purpose, String userId) {
        LocalDateTime now = now();
        WebAuthnChallenge challenge = repository.findForUpdate(id)
                .filter(c -> c.isFor(purpose, userId) && c.isValid(now))
                .orElseThrow(WebAuthnChallengeExpiredException::new);
        challenge.markUsed(now);
        return repository.save(challenge);
    }

    @Transactional
    public int deleteExpired() {
        return repository.deleteExpired(now());
    }

    private byte[] randomBytes() {
        byte[] bytes = new byte[CHALLENGE_BYTES];
        random.nextBytes(bytes);
        return bytes;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
