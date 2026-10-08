package com.proautokimium.api.Application.DTOs.humanResources.DocumentRequest;

import java.util.List;

/** A confirmação do envio: quantos recebem pelo portal e quem o RH vai registrar. */
public record AudiencePreviewDTO(int total, int withAccess, List<PersonOptionDTO> withoutAccess) {
}
