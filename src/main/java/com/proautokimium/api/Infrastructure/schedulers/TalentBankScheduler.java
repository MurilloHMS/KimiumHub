package com.proautokimium.api.Infrastructure.schedulers;

import com.proautokimium.api.Infrastructure.services.processoSeletivo.TalentBankAccessTokenService;
import com.proautokimium.api.Infrastructure.services.processoSeletivo.TalentBankService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * O que faz a promessa do banco de talentos valer: "guardamos por 24 meses".
 *
 * <p>Até esta classe existir, o prazo estava gravado em {@code expira_em} e
 * nada o cumpria.
 *
 * <p><b>Fino de propósito.</b> A regra mora nos services, que recebem o
 * {@code Clock} e têm teste; aqui só se decide quando rodar e o que fazer com
 * a falha de um item. O {@code SecretCleanupScheduler} ao lado faz a conta dentro
 * do agendador com {@code LocalDateTime.now()}, e por isso não tem teste.
 *
 * <p><b>O fuso está escrito em cada {@code cron}.</b> Sem o {@code zone}, as
 * 3h30 seriam as 3h30 do fuso da JVM, que é o do container — e o expurgo
 * passaria a rodar em horário comercial no dia em que alguém subir a imagem
 * sem o {@code TZ}.
 */
@Component
public class TalentBankScheduler {

    private static final String FUSO = "America/Sao_Paulo";

    private final TalentBankService talentBankService;
    private final TalentBankAccessTokenService tokenService;

    private final Logger logger = LoggerFactory.getLogger(TalentBankScheduler.class);

    public TalentBankScheduler(TalentBankService talentBankService,
                               TalentBankAccessTokenService tokenService) {
        this.talentBankService = talentBankService;
        this.tokenService = tokenService;
    }

    /** De hora em hora. O minuto 17 é só para não somar com quem roda na hora cheia. */
    @Scheduled(cron = "0 17 * * * *", zone = FUSO)
    public void limparTokens() {
        int apagados = tokenService.limparAntigos();
        if (apagados > 0) {
            logger.info("[Banco de talentos] {} link(s) de acesso antigo(s) apagado(s)", apagados);
        }
    }

    /**
     * Toda madrugada: quem venceu sai. Sem candidatura a linha é apagada; com
     * candidatura, anonimizada.
     *
     * <p><b>Uma pessoa que falha não segura as outras.</b> Cada expurgo tem a
     * própria transação; a falha é registrada e a madrugada seguinte tenta de
     * novo, porque a linha continua vencida.
     */
    @Scheduled(cron = "0 30 3 * * *", zone = FUSO)
    public void expurgarVencidos() {
        List<UUID> vencidos = talentBankService.idsVencidos();
        if (vencidos.isEmpty()) {
            return;
        }

        int expurgados = 0;
        int falhas = 0;

        for (UUID id : vencidos) {
            try {
                if (talentBankService.expurgarSeVencido(id)) {
                    expurgados++;
                }
            } catch (Exception e) {
                falhas++;
                logger.error("[Banco de talentos] falha ao expurgar o candidato {}", id, e);
            }
        }

        logger.info("[Banco de talentos] prazo vencido: {} expurgado(s), {} falha(s), de {} encontrado(s)",
                expurgados, falhas, vencidos.size());
    }
}
