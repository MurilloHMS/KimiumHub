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
     * @param source    {@code EVENT}, {@code COMPANY}, {@code ADDRESS} ou {@code ONLINE}
     * @param address   nulo quando não há endereço usável (empresa sem endereço,
     *                  rascunho sem local, evento online)
     * @param onlineUrl o link da transmissão; só em evento online
     */
    public record LocationDTO(String source, UUID companyId, String name, AddressDTO address, String onlineUrl) {
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
     * @param audienceAll  nulo mantém o público como está — o site de antes da
     *                     V113 não manda estes campos, e salvar por ele não pode
     *                     zerar a lista que alguém escolheu
     * @param reminderEnabled nulo mantém o lembrete como está, pelo mesmo motivo
     * @param reminderTime na hora cheia: o lembrete roda de hora em hora
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

            boolean removeCover,

            Boolean audienceAll,
            List<UUID> audienceCompanyIds,
            List<UUID> audienceDepartmentIds,
            List<UUID> audienceEmployeeIds,

            Boolean reminderEnabled,
            LocalTime reminderTime,

            /** Quantos dias antes do primeiro dia o lembrete começa, de 1 a 60. */
            Integer reminderDaysBefore,

            /** O link da transmissão; obrigatório com {@code ONLINE}, só {@code https://}. */
            @Size(max = 500, message = "O link deve ter no máximo 500 caracteres.")
            String onlineUrl,

            /** Horário do evento online; obrigatórios com {@code ONLINE}. */
            LocalTime startTime,
            LocalTime endTime,

            /** Nulo mantém como está: o site de antes da V116 não manda. */
            Boolean announceOnPublish,

            /** "Começou agora" na hora de início; só vale no online. Nulo mantém. */
            Boolean notifyLiveStart
    ) {
    }

    /** Um card da lista. */
    public record EventSummaryDTO(
            UUID id, String name, LocalDate startDate, LocalDate endDate, String coverUrl,
            LocationDTO location, int talkCount, int awayTalkCount,
            LocalDateTime publishedAt, LocalDateTime updatedAt, String updatedBy,
            LocalTime startTime, LocalTime endTime) {
    }

    /**
     * O evento aberto, com a programação inteira em ordem de dia e horário.
     *
     * @param startsAt     quando começa
     * @param settings     público e avisos; só para quem cadastra — os nomes das
     *                     pessoas escolhidas não são da conta de quem só vê
     * @param endsAt       quando acaba: é o fim do "Ao vivo"
     * @param answersUntil até quando dá para responder ao convite
     */
    public record EventDetailDTO(
            UUID id, String name, String description, LocalDate startDate, LocalDate endDate,
            String coverUrl, EventLocationType locationType, LocationDTO location,
            LocalDateTime publishedAt, LocalDateTime updatedAt, String updatedBy,
            List<TalkDTO> talks, LocalDateTime startsAt, EventSettingsDTO settings,
            LocalTime startTime, LocalTime endTime, LocalDateTime endsAt, LocalDateTime answersUntil) {
    }

    /**
     * Quem é convidado e os avisos, como o formulário reabre.
     *
     * @param announcedAt quando o aviso de publicação saiu; nulo se ainda não saiu
     */
    public record EventSettingsDTO(
            boolean audienceAll,
            List<EventAttendanceDTOs.AudienceOptionDTO> companies,
            List<EventAttendanceDTOs.AudienceOptionDTO> departments,
            List<EventAttendanceDTOs.AudienceOptionDTO> employees,
            boolean reminderEnabled, LocalTime reminderTime, Integer reminderDaysBefore,
            boolean announceOnPublish, LocalDateTime announcedAt, boolean notifyLiveStart) {
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
