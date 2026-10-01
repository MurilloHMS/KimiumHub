package com.proautokimium.api.Application.DTOs.events;

import com.proautokimium.api.Application.DTOs.events.EventDTOs.EventDetailDTO;
import com.proautokimium.api.Application.DTOs.events.EventDTOs.EventSummaryDTO;
import com.proautokimium.api.domain.entities.events.EventResponse;
import com.proautokimium.api.domain.enums.events.EventAnswer;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Os DTOs da confirmação de presença (V113): o convite visto por quem foi
 * convidado, e o Acompanhamento visto por quem organiza.
 */
public final class EventAttendanceDTOs {

    private EventAttendanceDTOs() {
    }

    // ─── Quem foi convidado ──────────────────────────────────────────────────

    public record RespondRequestDTO(
            @NotNull(message = "Escolha se vai ou não vai.")
            EventAnswer answer,

            @Size(max = EventResponse.NOTE_MAX, message = "A observação pode ter no máximo 500 caracteres.")
            String note
    ) {
    }

    /** A resposta de uma pessoa a um convite, como a tela mostra. */
    public record InvitationAnswerDTO(EventAnswer answer, String note,
                                      LocalDateTime firstAnsweredAt, LocalDateTime answeredAt) {

        public static InvitationAnswerDTO from(EventResponse r) {
            return r == null ? null
                    : new InvitationAnswerDTO(r.getAnswer(), r.getNote(), r.getFirstAnsweredAt(), r.getAnsweredAt());
        }
    }

    /**
     * Um card de "Meus convites".
     *
     * @param startsAt até quando dá para responder
     * @param open     ainda dá para responder (agora é antes de {@code startsAt})
     * @param answer   nulo enquanto a pessoa não respondeu
     */
    public record InvitationDTO(EventSummaryDTO event, LocalDateTime startsAt, boolean open,
                                InvitationAnswerDTO answer) {
    }

    /** O convite aberto: o evento inteiro, com a programação, e a resposta. */
    public record InvitationDetailDTO(EventDetailDTO event, LocalDateTime startsAt, boolean open,
                                      InvitationAnswerDTO answer) {
    }

    // ─── Quem organiza ───────────────────────────────────────────────────────

    /** Uma empresa, um setor ou uma pessoa nos seletores do público. */
    public record AudienceOptionDTO(UUID id, String name, String detail) {
    }

    public record AudienceOptionsDTO(List<AudienceOptionDTO> companies,
                                     List<AudienceOptionDTO> departments,
                                     List<AudienceOptionDTO> employees) {
    }

    /**
     * Uma linha do Acompanhamento.
     *
     * @param invited falso para quem respondeu ou abriu e depois saiu do público
     *                (o organizador mudou a lista): a auditoria não apaga o que
     *                a pessoa fez, mas a linha não entra nos totais
     */
    public record AttendeeDTO(UUID employeeId, String name, String companyName, String departmentName,
                              boolean invited,
                              LocalDateTime firstViewedAt, LocalDateTime lastViewedAt, int viewCount,
                              EventAnswer answer, String note,
                              LocalDateTime firstAnsweredAt, LocalDateTime answeredAt) {
    }

    /**
     * O Acompanhamento de um evento. Os totais contam só quem está no público hoje.
     *
     * @param reminderDays os dias em que o lembrete saiu, com quantos receberam
     */
    public record AttendanceDTO(UUID eventId, String eventName, LocalDate startDate, LocalDate endDate,
                                LocalDateTime startsAt, boolean reminderEnabled, LocalTime reminderTime,
                                Integer reminderDaysBefore,
                                List<ReminderDayDTO> reminderDays,
                                int invited, int going, int notGoing, int noAnswer, int neverViewed,
                                List<AttendeeDTO> attendees) {
    }

    public record ReminderDayDTO(LocalDate day, LocalDateTime sentAt, int recipients) {
    }
}
