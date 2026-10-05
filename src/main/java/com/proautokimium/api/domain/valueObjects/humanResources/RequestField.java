package com.proautokimium.api.domain.valueObjects.humanResources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RequestField(
        String key,
        String label,
        String help,
        String type,
        boolean required,
        List<String> options,
        UUID documentTypeId
) {
}
