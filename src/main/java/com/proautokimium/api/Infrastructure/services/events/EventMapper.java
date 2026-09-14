package com.proautokimium.api.Infrastructure.services.events;

import com.proautokimium.api.Application.DTOs.address.AddressDTO;
import com.proautokimium.api.Application.DTOs.events.EventDTOs.EventDetailDTO;
import com.proautokimium.api.Application.DTOs.events.EventDTOs.EventSummaryDTO;
import com.proautokimium.api.Application.DTOs.events.EventDTOs.LocationDTO;
import com.proautokimium.api.Application.DTOs.events.EventDTOs.SpeakerDTO;
import com.proautokimium.api.Application.DTOs.events.EventDTOs.TalkDTO;
import com.proautokimium.api.domain.entities.events.CompanyEvent;
import com.proautokimium.api.domain.entities.events.EventTalk;
import com.proautokimium.api.domain.entities.events.Speaker;
import com.proautokimium.api.domain.entities.humanResources.Company;
import com.proautokimium.api.domain.enums.events.EventLocationType;
import com.proautokimium.api.domain.enums.events.TalkLocationType;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Entidade → DTO, e a resolução do local.
 *
 * <p><b>O local chega resolvido no site.</b> A palestra "no local do evento" vem
 * com o endereço do evento; a empresa do grupo vem com o endereço do cadastro
 * dela. Deixar isso para o site repetiria a mesma regra em três telas.
 */
final class EventMapper {

    private EventMapper() {
    }

    static LocationDTO locationOf(CompanyEvent event) {
        if (event.getLocationType() == null) {
            return null;
        }
        if (event.getLocationType() == EventLocationType.COMPANY) {
            return companyLocation(event.getCompany());
        }
        return new LocationDTO("ADDRESS", null, event.getPlaceName(), usable(AddressDTO.from(event.getAddress())));
    }

    static LocationDTO locationOf(EventTalk talk) {
        TalkLocationType type = talk.getLocationType() == null ? TalkLocationType.EVENT : talk.getLocationType();
        return switch (type) {
            case EVENT -> {
                LocationDTO doEvento = locationOf(talk.getEvent());
                yield doEvento == null ? null
                        : new LocationDTO("EVENT", doEvento.companyId(), doEvento.name(), doEvento.address());
            }
            case COMPANY -> companyLocation(talk.getCompany());
            case ADDRESS -> new LocationDTO("ADDRESS", null, talk.getPlaceName(), usable(AddressDTO.from(talk.getAddress())));
        };
    }

    private static LocationDTO companyLocation(Company company) {
        if (company == null) {
            return null;
        }
        return new LocationDTO("COMPANY", company.getId(), company.getName(),
                usable(AddressDTO.from(company.getAddress())));
    }

    /** Endereço sem rua ou sem cidade não vai para o mapa: abriria em lugar nenhum. */
    private static AddressDTO usable(AddressDTO address) {
        if (address == null || address.street() == null || address.city() == null) {
            return null;
        }
        return address;
    }

    static SpeakerDTO speaker(Speaker s, Map<UUID, Long> talkCounts) {
        return new SpeakerDTO(s.getId(), s.getName(), s.getRole(), s.getCompanyName(), s.getPhotoUrl(),
                s.getInstagram(), s.getLinkedin(), s.getWebsite(),
                talkCounts.getOrDefault(s.getId(), 0L), s.getUpdatedAt(), s.getUpdatedBy());
    }

    static TalkDTO talk(EventTalk t) {
        List<SpeakerDTO> speakers = t.getSpeakers().stream()
                .map(s -> speaker(s, Map.of()))
                .toList();
        return new TalkDTO(t.getId(), t.getTitle(), t.getDescription(), t.getDate(),
                t.getStartTime(), t.getEndTime(), t.getRoom(),
                t.getLocationType() == null ? TalkLocationType.EVENT : t.getLocationType(),
                locationOf(t), speakers);
    }

    static EventSummaryDTO summary(CompanyEvent e) {
        int away = (int) e.getTalks().stream()
                .filter(t -> t.getLocationType() != null && t.getLocationType() != TalkLocationType.EVENT)
                .count();
        return new EventSummaryDTO(e.getId(), e.getName(), e.getStartDate(), e.getEndDate(), e.getCoverUrl(),
                locationOf(e), e.getTalks().size(), away,
                e.getPublishedAt(), e.getUpdatedAt(), e.getUpdatedBy());
    }

    /**
     * A programação sai ordenada aqui, e não confiando só no {@code @OrderBy}:
     * palestra recém-criada ou com horário mudado continua na posição antiga da
     * lista em memória até a próxima leitura do banco.
     */
    static EventDetailDTO detail(CompanyEvent e) {
        List<TalkDTO> talks = e.getTalks().stream()
                .sorted(Comparator.comparing(EventTalk::getDate)
                        .thenComparing(EventTalk::getStartTime)
                        .thenComparing(EventTalk::getEndTime))
                .map(EventMapper::talk)
                .toList();
        return new EventDetailDTO(e.getId(), e.getName(), e.getDescription(), e.getStartDate(), e.getEndDate(),
                e.getCoverUrl(), e.getLocationType(), locationOf(e),
                e.getPublishedAt(), e.getUpdatedAt(), e.getUpdatedBy(), talks);
    }
}
