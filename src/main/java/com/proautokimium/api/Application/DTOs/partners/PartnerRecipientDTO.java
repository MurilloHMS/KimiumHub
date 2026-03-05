package com.proautokimium.api.Application.DTOs.partners;

import java.util.UUID;

public record PartnerRecipientDTO(UUID id, String name, String email, String type) {
}
