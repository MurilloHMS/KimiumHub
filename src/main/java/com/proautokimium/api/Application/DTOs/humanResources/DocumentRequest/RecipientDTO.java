package com.proautokimium.api.Application.DTOs.humanResources.DocumentRequest;

import com.proautokimium.api.domain.enums.humanResources.RecipientStatus;
import com.proautokimium.api.domain.enums.humanResources.RequestStatus;
import com.proautokimium.api.domain.valueObjects.humanResources.RequestField;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Uma resposta, com o que a tela precisa para mostrar e conferir sem outra
 * chamada: o formulário (para saber o rótulo de cada chave) e os arquivos atuais.
 * Serve ao RH (acompanhar e conferir) e ao funcionário ("minhas solicitações").
 */
public record RecipientDTO(
        UUID id,
        UUID requestId,
        String requestTitle,
        String requestInstructions,
        LocalDate requestDueDate,
        RequestStatus requestStatus,
        List<RequestField> form,
        /** O modelo para baixar e preencher, ou null. */
        String requestTemplateFilename,
        UUID employeeId,
        String employeeName,
        RecipientStatus status,
        Map<String, Object> answers,
        LocalDateTime addedAt,
        LocalDateTime submittedAt,
        String reviewedBy,
        LocalDateTime reviewedAt,
        String returnReason,
        List<RequestFileDTO> files,
        /** Tem login ativo: recebe pelo portal. Sem acesso, o RH registra a resposta. */
        boolean hasAccess,
        /** Login do RH que registrou no lugar do funcionário; nulo = ele respondeu pelo portal. */
        String registeredBy
) {
}
