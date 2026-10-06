package com.proautokimium.api.Application.DTOs.humanResources.DocumentRequest;

import java.util.Map;

/** As respostas dos campos que não são arquivo. Os arquivos sobem antes, um por campo. */
public record SubmitAnswersDTO(Map<String, Object> answers) {
}
