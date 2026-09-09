package com.proautokimium.api.Infrastructure.services.newsletter;

import com.proautokimium.api.Application.DTOs.newsletter.ResumoDoMesDTO;
import com.proautokimium.api.Infrastructure.repositories.NewsletterPreviaRepository;
import com.proautokimium.api.Infrastructure.repositories.NewsletterRepository;
import com.proautokimium.api.domain.entities.Newsletter;
import com.proautokimium.api.domain.entities.NewsletterPrevia;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A fila de envio vista por mês.
 *
 * A tela de envio mostra um cartão por mês com o que já saiu e o que falta,
 * porque é assim que se fala da newsletter — "a de junho já saiu?". Com a lista
 * crua de clientes essa pergunta exige ler linha por linha.
 */
@Service
public class NewsletterResumoService {

    private static final Locale BRASIL = Locale.forLanguageTag("pt-BR");

    private final NewsletterRepository newsletterRepository;
    private final NewsletterPreviaRepository previaRepository;

    public NewsletterResumoService(NewsletterRepository newsletterRepository,
                                   NewsletterPreviaRepository previaRepository) {
        this.newsletterRepository = newsletterRepository;
        this.previaRepository = previaRepository;
    }

    @Transactional(readOnly = true)
    public List<ResumoDoMesDTO> porMes() {
        // `LinkedHashMap` preserva a ordem que veio do banco — mês mais recente
        // primeiro, que é onde está o trabalho.
        Map<String, ResumoEmMontagem> meses = new LinkedHashMap<>();

        for (Object[] linha : newsletterRepository.contarPorMesEStatus()) {
            int ano = ((Number) linha[0]).intValue();
            int mes = ((Number) linha[1]).intValue();
            String status = String.valueOf(linha[2]);
            long quantidade = ((Number) linha[3]).longValue();

            meses.computeIfAbsent(ano + "-" + mes, k -> new ResumoEmMontagem(mes, ano))
                    .somar(status, quantidade);
        }

        List<ResumoDoMesDTO> resultado = new ArrayList<>(meses.size());

        for (ResumoEmMontagem m : meses.values()) {
            // A data de confirmação vem da prévia, quando o mês passou por ela.
            // Os meses antigos vieram por planilha e não têm — e `null` diz isso
            // melhor que uma data inventada.
            LocalDateTime confirmadoEm = previaRepository.findByMesAndAno(m.mes, m.ano)
                    .map(NewsletterPrevia::getConfirmadoEm)
                    .orElse(null);

            resultado.add(new ResumoDoMesDTO(m.mes, m.ano, nomeDoMes(m.mes),
                    m.total(), m.porStatus, confirmadoEm));
        }

        return resultado;
    }

    /** As linhas de um mês, da maior para a menor — onde um erro custa caro. */
    @Transactional(readOnly = true)
    public List<Newsletter> doMes(int mes, int ano) {
        LocalDate de = LocalDate.of(ano, mes, 1);
        LocalDate ate = de.withDayOfMonth(de.lengthOfMonth());

        return newsletterRepository.findByDataBetweenOrderByFaturamentoTotalDesc(de, ate);
    }

    private String nomeDoMes(int mes) {
        String nome = Month.of(mes).getDisplayName(TextStyle.FULL, BRASIL);
        return nome.substring(0, 1).toUpperCase(BRASIL) + nome.substring(1);
    }

    private static final class ResumoEmMontagem {
        final int mes;
        final int ano;
        final Map<String, Long> porStatus = new LinkedHashMap<>();

        ResumoEmMontagem(int mes, int ano) {
            this.mes = mes;
            this.ano = ano;
        }

        void somar(String status, long quantidade) {
            porStatus.merge(status, quantidade, Long::sum);
        }

        int total() {
            return porStatus.values().stream().mapToInt(Long::intValue).sum();
        }
    }
}
