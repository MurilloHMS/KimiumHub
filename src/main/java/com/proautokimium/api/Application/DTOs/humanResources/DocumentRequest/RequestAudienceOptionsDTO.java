package com.proautokimium.api.Application.DTOs.humanResources.DocumentRequest;

import com.proautokimium.api.Application.DTOs.events.EventAttendanceDTOs.AudienceOptionDTO;

import java.util.List;

/** As opções do seletor de público das Solicitações: empresas e setores dos Eventos, e todos os ativos. */
public record RequestAudienceOptionsDTO(List<AudienceOptionDTO> companies,
                                        List<AudienceOptionDTO> departments,
                                        List<PersonOptionDTO> employees) {
}
