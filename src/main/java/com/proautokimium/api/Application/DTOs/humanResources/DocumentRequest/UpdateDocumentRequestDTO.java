package com.proautokimium.api.Application.DTOs.humanResources.DocumentRequest;

import com.proautokimium.api.domain.valueObjects.humanResources.RequestField;

import java.time.LocalDate;
import java.util.List;

public record UpdateDocumentRequestDTO(String title, String instructions, LocalDate dueDate, List<RequestField> form) {
}
