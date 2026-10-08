package com.proautokimium.api.Application.DTOs.partners;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Uma linha do relatório dos funcionários que ainda não entraram no site.
 *
 * Classe com getters, e não record: o JRBeanCollectionDataSource do PDF lê os
 * campos por getter de JavaBean ({@code getX()}) e não acha o acessor de record
 * ({@code x()}). O mesmo motivo da linha do comprovante de reembolsos.
 *
 * Tudo já em texto: o Excel e o PDF mostram exatamente o mesmo, e a regra de
 * como escrever cada coisa fica num lugar só.
 */
@Getter
@AllArgsConstructor
public class PendingSiteAccessRowDTO {
    private final String code;
    private final String name;
    private final String email;
    private final String company;
    private final String department;
    private final String position;
    /** "nunca entrou" ou "pediu o código em 03/10/2026, não concluiu". */
    private final String detail;
}
