package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Calendar.CalendarEventDTO;
import com.proautokimium.api.Infrastructure.repositories.humanResources.VacationRequestRepository;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.Team;
import com.proautokimium.api.domain.entities.humanResources.VacationRequest;
import com.proautokimium.api.domain.enums.humanResources.VacationRequestStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarServiceTest {

    @Mock private VacationRequestRepository vacationRequestRepository;

    private CalendarService service;

    @BeforeEach
    void setUp() {
        service = new CalendarService(vacationRequestRepository);
    }

    private Employee employee(String name) throws Exception {
        Employee e = new Employee();
        e.setName(name);
        Field field = com.proautokimium.api.domain.abstractions.Entity.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(e, UUID.randomUUID());
        return e;
    }

    private VacationRequest approvedVacation(Employee employee, LocalDate start, LocalDate end) {
        VacationRequest vr = VacationRequest.request(employee, start, end, null, LocalDateTime.of(2026, 1, 1, 9, 0));
        vr.approve(employee, "ok", LocalDateTime.of(2026, 1, 2, 9, 0));
        return vr;
    }

    @Test
    @DisplayName("Sem status informado, usa APPROVED como padrão")
    void semStatusUsaApprovedComoPadrao() throws Exception {
        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 31);

        when(vacationRequestRepository.findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                any(), any(), any())).thenReturn(List.of());

        service.getEvents(start, end, null, null, null);

        verify(vacationRequestRepository).findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                VacationRequestStatus.APPROVED, end, start);
    }

    @Test
    @DisplayName("Evento parcialmente dentro do período aparece (sobreposição, não contenção total)")
    void eventoParcialmenteDentroDoPeriodoAparece() throws Exception {
        Employee emp = employee("Ana");
        LocalDate rangeStart = LocalDate.of(2026, 8, 1);
        LocalDate rangeEnd = LocalDate.of(2026, 8, 31);
        // começa antes do período e termina dentro dele
        VacationRequest vr = approvedVacation(emp, LocalDate.of(2026, 7, 25), LocalDate.of(2026, 8, 5));

        when(vacationRequestRepository.findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                eq(VacationRequestStatus.APPROVED), eq(rangeEnd), eq(rangeStart))).thenReturn(List.of(vr));

        List<CalendarEventDTO> events = service.getEvents(rangeStart, rangeEnd, null, null, null);

        assertThat(events).hasSize(1);
        assertThat(events.get(0).employeeName()).isEqualTo("Ana");
    }

    @Test
    @DisplayName("Filtro por status explícito é repassado ao repositório, não o default")
    void filtroPorStatusExplicitoSubstituiPadrao() {
        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 31);

        when(vacationRequestRepository.findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                any(), any(), any())).thenReturn(List.of());

        service.getEvents(start, end, null, null, VacationRequestStatus.PENDING);

        verify(vacationRequestRepository).findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                VacationRequestStatus.PENDING, end, start);
    }

    @Test
    @DisplayName("Filtro por teamId exclui eventos de outros setores")
    void filtroPorTeamExcluiOutrosSetores() throws Exception {
        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 31);

        Team compras = new Team();
        Field teamIdField = com.proautokimium.api.domain.abstractions.Entity.class.getDeclaredField("id");
        teamIdField.setAccessible(true);
        UUID comprasId = UUID.randomUUID();
        teamIdField.set(compras, comprasId);

        Team rh = new Team();
        teamIdField.set(rh, UUID.randomUUID());

        Employee doCompras = employee("Do Compras");
        doCompras.setTeam(compras);
        Employee doRh = employee("Do RH");
        doRh.setTeam(rh);

        VacationRequest vrCompras = approvedVacation(doCompras, start, start.plusDays(5));
        VacationRequest vrRh = approvedVacation(doRh, start, start.plusDays(5));

        when(vacationRequestRepository.findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                any(), any(), any())).thenReturn(List.of(vrCompras, vrRh));

        List<CalendarEventDTO> events = service.getEvents(start, end, comprasId, null, null);

        assertThat(events).hasSize(1);
        assertThat(events.get(0).employeeName()).isEqualTo("Do Compras");
    }
}
