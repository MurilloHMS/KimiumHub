package com.proautokimium.api.Application.DTOs.events;

import com.proautokimium.api.Application.DTOs.address.AddressDTO;
import com.proautokimium.api.domain.enums.events.EventLocationType;
import com.proautokimium.api.domain.enums.events.TalkLocationType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Os DTOs dos eventos, juntos num arquivo: são pequenos, só fazem sentido uns com
 * os outros, e espalhados em nove arquivos a leitura do contrato pede nove abas.
 */
public final class EventDTOs {

    private EventDTOs() {
    }

    // ─── Local ───────────────────────────────────────────────────────────────

    /**
     * Onde algo acontece, <b>já resolvido</b>.
     *
     * <p>O site não precisa saber de onde veio o endereço para mostrar o mapa: a
     * palestra "no local do evento" chega com o endereço do evento, e a empresa
     * do grupo chega com o endereço do cadastro dela.
     *
     * @param source   {@code EVENT}, {@code COMPANY} ou {@code ADDRESS}
     * @param address  nulo quando não há endereço usável (empresa sem endereço,
     *                 rascunho sem local)
     */
    public record LocationDTO(String source, UUID companyId, String name, AddressDTO address) {
    }

    // ─── Palestrante ─────────────────────────────────────────────────────────

    public record SpeakerRequestDTO(
            @NotBlank(message = "Informe o nome do palestrante.")
            @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
            String name,

            @Size(max = 120, message = "O cargo deve ter no máximo 120 caracteres.")
            String role,

            @Size(max = 120, message = "A empresa deve ter no máximo 120 caracteres.")
            String companyName,

            @Size(max = 100, message = "O Instagram deve ter no máximo 100 caracteres.")
            String instagram,

            @Size(max = 100, message = "O LinkedIn deve ter no máximo 100 caracteres.")
            String linkedin,

            @Size(max = 255, message = "O site deve ter no máximo 255 caracteres.")
            String website,

            /** Tira a foto atual sem mandar outra. */
            boolean removePhoto
    ) {
    }

    /** @param talkCount em quantas palestras, de qualquer evento, a pessoa está */
    public record SpeakerDTO(
            UUID id, String name, String role, String companyName, String photoUrl,
            String instagram, String linkedin, String website,
            long talkCount, LocalDateTime updatedAt, String updatedBy) {
    }

    // ─── Evento ──────────────────────────────────────────────────────────────

    /**
     * @param locationType nulo para "ainda sem local"
     * @param companyId    obrigatório quando {@code COMPANY}
     * @param placeName    obrigatório quando {@code ADDRESS}
     * @param address      obrigatório (rua e cidade) quando {@code ADDRESS}
     * @param removeCover  tira a capa atual sem mandar outra
     */
    public record EventRequestDTO(
            @NotBlank(message = "Informe o nome do evento.")
            @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
            String name,

            String description,

            @NotNull(message = "Informe o primeiro dia.")
            LocalDate startDate,

            @NotNull(message = "Informe o último dia.")
            LocalDate endDate,

            EventLocationType locationType,
            UUID companyId,

            @Size(max = 150, message = "O nome do lugar deve ter no máximo 150 caracteres.")
            String placeName,

            @Valid AddressDTO address,

            boolean removeCover
    ) {
    }

    /** Um card da lista. */
    public record EventSummaryDTO(
            UUID id, String name, LocalDate startDate, LocalDate endDate, String coverUrl,
            LocationDTO location, int talkCount, int awayTalkCount,
            LocalDateTime publishedAt, LocalDateTime updatedAt, String updatedBy) {
    }

    /** O evento aberto, com a programação inteira em ordem de dia e horário. */
    public record EventDetailDTO(
            UUID id, String name, String description, LocalDate startDate, LocalDate endDate,
            String coverUrl, EventLocationType locationType, LocationDTO location,
            LocalDateTime publishedAt, LocalDateTime updatedAt, String updatedBy,
            List<TalkDTO> talks) {
    }

    // ─── Palestra ────────────────────────────────────────────────────────────

    /**
     * @param locationType nulo vale como {@code EVENT}
     * @param speakerIds   na ordem em que devem aparecer; vazio para intervalo
     */
    public record TalkRequestDTO(
            @NotBlank(message = "Informe o título.")
            @Size(max = 200, message = "O título deve ter no máximo 200 caracteres.")
            String title,

            String description,

            @NotNull(message = "Informe o dia.")
            LocalDate date,

            @NotNull(message = "Informe o horário de início.")
            LocalTime startTime,

            @NotNull(message = "Informe o horário de fim.")
            LocalTime endTime,

            @Size(max = 100, message = "A sala deve ter no máximo 100 caracteres.")
            String room,

            TalkLocationType locationType,
            UUID companyId,

            @Size(max = 150, message = "O nome do lugar deve ter no máximo 150 caracteres.")
            String placeName,

            @Valid AddressDTO address,

            List<UUID> speakerIds
    ) {
    }

    /**
     * @param locationType como foi cadastrada — o formulário precisa disto para
     *                     reabrir no rádio certo
     * @param location     onde acontece de fato, já resolvido
     */
    public record TalkDTO(
            UUID id, String title, String description, LocalDate date,
            LocalTime startTime, LocalTime endTime, String room,
            TalkLocationType locationType, LocationDTO location,
            List<SpeakerDTO> speakers) {
    }
}
