package com.proautokimium.api.Application.DTOs.humanResources.Reimbursement;

import java.util.List;

/**
 * O que saiu e o que não saiu. Com três destinatários e um endereço quebrado,
 * dizer só "enviado" esconderia quem não recebeu.
 */
public record ReportEmailResultDTO(String fileName, List<String> sentTo, List<String> failed) {}
