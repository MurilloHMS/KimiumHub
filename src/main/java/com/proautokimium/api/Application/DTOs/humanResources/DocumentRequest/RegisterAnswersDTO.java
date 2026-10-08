package com.proautokimium.api.Application.DTOs.humanResources.DocumentRequest;

import java.util.Map;

/** O RH registra a resposta no lugar do funcionário; {@code approve} aprova na mesma hora. */
public record RegisterAnswersDTO(Map<String, Object> answers, boolean approve) {
}
