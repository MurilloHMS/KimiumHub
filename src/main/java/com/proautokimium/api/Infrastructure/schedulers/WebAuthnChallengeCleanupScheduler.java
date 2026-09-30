package com.proautokimium.api.Infrastructure.schedulers;

import com.proautokimium.api.Infrastructure.services.authentication.webauthn.WebAuthnChallengeStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Apaga os desafios vencidos da digital. O de login nasce num endpoint
 * público, então a tabela cresce a cada toque em "Entrar com a digital" —
 * inclusive de quem só quer enchê-la. A cada 10 minutos basta: o desafio vale 5.
 */
@Component
public class WebAuthnChallengeCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(WebAuthnChallengeCleanupScheduler.class);

    private final WebAuthnChallengeStore challenges;

    public WebAuthnChallengeCleanupScheduler(WebAuthnChallengeStore challenges) {
        this.challenges = challenges;
    }

    @Scheduled(cron = "0 */10 * * * *")
    public void deleteExpired() {
        int n = challenges.deleteExpired();
        if (n > 0) log.info("[WebAuthn] {} desafios vencidos removidos", n);
    }
}
