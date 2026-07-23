package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Calendar.CalendarEventDTO;
import com.proautokimium.api.Infrastructure.services.humanResources.CalendarService;
import com.proautokimium.api.domain.enums.humanResources.VacationRequestStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hr/calendar")
@Tag(name = "Calendário RH", description = "Eventos agendados (férias etc.) num período, com filtros")
public class CalendarController {

    private final CalendarService service;

    public CalendarController(CalendarService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RH')")
    @Operation(summary = "Eventos do calendário", description = "Lista eventos que se sobrepõem ao período, filtráveis por setor/empresa/status")
    public ResponseEntity<List<CalendarEventDTO>> get(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) UUID teamId,
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) VacationRequestStatus status
    ) {
        return ResponseEntity.ok(service.getEvents(start, end, teamId, companyId, status));
    }
}
