package com.proautokimium.api.Application.DTOs.sales;

import jakarta.validation.constraints.Size;

/** Motivo, observação ou justificativa de uma ação. A obrigatoriedade é da entidade. */
public record ChecklistNotesDTO(@Size(max = 500) String notes) {}
