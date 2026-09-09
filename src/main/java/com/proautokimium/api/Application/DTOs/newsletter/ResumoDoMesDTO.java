package com.proautokimium.api.Application.DTOs.newsletter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Um mês da fila de envio, contado por status.
 *
 * **A newsletter é mensal, e é assim que se fala dela** — "a de junho já saiu?".
 * Com a lista crua de clientes essa pergunta só se responde lendo linha por
 * linha; com a contagem, ela se responde de relance.
 *
 * `porStatus` é mapa e não campo por campo: `EmailStatus` tem sete valores hoje
 * e o dia em que ganhar o oitavo, a tela mostra sozinha em vez de esconder.
 */
public record ResumoDoMesDTO(
        int mes,
        int ano,
        String nomeDoMes,
        int total,
        Map<String, Long> porStatus,
        /** Quando a prévia deste mês foi confirmada; `null` para os meses que vieram por planilha. */
        LocalDateTime confirmadoEm
) {
}
