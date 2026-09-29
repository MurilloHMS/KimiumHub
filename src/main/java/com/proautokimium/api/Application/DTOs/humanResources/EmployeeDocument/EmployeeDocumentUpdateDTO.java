package com.proautokimium.api.Application.DTOs.humanResources.EmployeeDocument;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/** O que o RH pode corrigir num documento já vinculado — o arquivo não: para isso, substitui. */
public record EmployeeDocumentUpdateDTO(
        @NotBlank(message = "Informe o título do documento.") @Size(max = 200) String title,
        UUID typeId,
        LocalDate dueDate
) {
}