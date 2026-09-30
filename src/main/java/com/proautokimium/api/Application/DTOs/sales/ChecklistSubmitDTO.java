package com.proautokimium.api.Application.DTOs.sales;

import com.proautokimium.api.domain.valueObjects.sales.ChecklistContent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Um envio vindo do celular.
 *
 * @param revision a versão que este envio cria: 1 no primeiro, e a atual + 1
 *                 no reenvio. É o que torna o envio repetível: se a internet
 *                 caiu depois de o servidor gravar, o celular manda de novo o
 *                 mesmo {@code revision}, e o servidor devolve o que já tem.
 */
public record ChecklistSubmitDTO(
        @Min(1) int revision,
        @NotNull ChecklistContent content,
        boolean filledOffline,
        LocalDateTime deviceStartedAt
) {}
