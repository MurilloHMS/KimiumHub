package com.proautokimium.api.Application.DTOs.holerite;

import com.proautokimium.api.domain.enums.humanResources.HoleritePreviewStatus;

import java.util.UUID;

/**
 * Uma página do PDF, do jeito que ela vai ser tratada no envio.
 *
 * O `status` é o que a tela pinta: só ele decide a cor da linha, e por isso é
 * enum e não texto livre — texto livre foi o que tornou o `naoEncontrados`
 * atual impossível de exibir de outro jeito que não uma lista solta.
 */
public record HoleritePreviewItemDTO(
        int pagina,
        String nome,
        String cpf,
        UUID employeeId,
        String employeeNome,
        String codParceiro,
        HoleritePreviewStatus status) { }