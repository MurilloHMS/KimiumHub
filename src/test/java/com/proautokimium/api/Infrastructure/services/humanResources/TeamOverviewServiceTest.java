package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.TeamOverview.TeamOverviewEntryDTO;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.CareerHistoryRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.VacationRequestRepository;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.CareerHistory;
import com.proautokimium.api.domain.entities.humanResources.Company;
import com.proautokimium.api.domain.entities.humanResources.Position;
import com.proautokimium.api.domain.entities.humanResources.PositionLevel;
import com.proautokimium.api.domain.entities.humanResources.Team;
import com.proautokimium.api.domain.entities.humanResources.VacationRequest;
import com.proautokimium.api.domain.enums.humanResources.ContractType;
import com.proautokimium.api.domain.enums.humanResources.VacationRequestStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamOverviewServiceTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private CareerHistoryRepository careerHistoryRepository;
    @Mock private VacationRequestRepository vacationRequestRepository;

    private TeamOverviewService service;
    private final LocalDate today = LocalDate.of(2026, 7, 23);

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(today.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        service = new TeamOverviewService(employeeRepository, careerHistoryRepository, vacationRequestRepository, clock);
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
    @DisplayName("Funcionário sem férias aprovadas fica AVAILABLE")
    void semFeriasFicaDisponivel() throws Exception {
        Employee emp = employee("Murillo");
        when(employeeRepository.findByAtivoTrue()).thenReturn(List.of(emp));
        when(careerHistoryRepository.findLatestPerEmployee()).thenReturn(List.of());
        when(vacationRequestRepository.findByStatus(VacationRequestStatus.APPROVED)).thenReturn(List.of());

        List<TeamOverviewEntryDTO> result = service.getOverview(null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).availabilityStatus().name()).isEqualTo("AVAILABLE");
    }

    @Test
    @DisplayName("Férias começando exatamente hoje conta como ON_VACATION, não agendada")
    void feriasComecandoHojeContaComoEmFerias() throws Exception {
        Employee emp = employee("Lucas");
        VacationRequest vr = approvedVacation(emp, today, today.plusDays(5));

        when(employeeRepository.findByAtivoTrue()).thenReturn(List.of(emp));
        when(careerHistoryRepository.findLatestPerEmployee()).thenReturn(List.of());
        when(vacationRequestRepository.findByStatus(VacationRequestStatus.APPROVED)).thenReturn(List.of(vr));

        List<TeamOverviewEntryDTO> result = service.getOverview(null, null);

        assertThat(result.get(0).availabilityStatus().name()).isEqualTo("ON_VACATION");
    }

    @Test
    @DisplayName("Férias terminando exatamente hoje ainda conta como ON_VACATION")
    void feriasTerminandoHojeAindaContaComoEmFerias() throws Exception {
        Employee emp = employee("Mateus");
        VacationRequest vr = approvedVacation(emp, today.minusDays(9), today);

        when(employeeRepository.findByAtivoTrue()).thenReturn(List.of(emp));
        when(careerHistoryRepository.findLatestPerEmployee()).thenReturn(List.of());
        when(vacationRequestRepository.findByStatus(VacationRequestStatus.APPROVED)).thenReturn(List.of(vr));

        List<TeamOverviewEntryDTO> result = service.getOverview(null, null);

        assertThat(result.get(0).availabilityStatus().name()).isEqualTo("ON_VACATION");
    }

    @Test
    @DisplayName("Férias futuras contam como VACATION_SCHEDULED")
    void feriasFuturasContamComoAgendadas() throws Exception {
        Employee emp = employee("Ana");
        VacationRequest vr = approvedVacation(emp, today.plusDays(10), today.plusDays(20));

        when(employeeRepository.findByAtivoTrue()).thenReturn(List.of(emp));
        when(careerHistoryRepository.findLatestPerEmployee()).thenReturn(List.of());
        when(vacationRequestRepository.findByStatus(VacationRequestStatus.APPROVED)).thenReturn(List.of(vr));

        List<TeamOverviewEntryDTO> result = service.getOverview(null, null);

        assertThat(result.get(0).availabilityStatus().name()).isEqualTo("VACATION_SCHEDULED");
    }

    @Test
    @DisplayName("Quando o funcionário tem férias em andamento E futuras, ON_VACATION tem prioridade")
    void emFeriasTemPrioridadeSobreAgendada() throws Exception {
        Employee emp = employee("Carla");
        VacationRequest atual = approvedVacation(emp, today.minusDays(2), today.plusDays(2));
        VacationRequest futura = approvedVacation(emp, today.plusDays(30), today.plusDays(40));

        when(employeeRepository.findByAtivoTrue()).thenReturn(List.of(emp));
        when(careerHistoryRepository.findLatestPerEmployee()).thenReturn(List.of());
        when(vacationRequestRepository.findByStatus(VacationRequestStatus.APPROVED)).thenReturn(List.of(atual, futura));

        List<TeamOverviewEntryDTO> result = service.getOverview(null, null);

        assertThat(result.get(0).availabilityStatus().name()).isEqualTo("ON_VACATION");
    }

    @Test
    @DisplayName("Contrato (CLT/PJ) vem do CareerHistory mais recente do funcionário")
    void contratoVemDoCareerHistoryMaisRecente() throws Exception {
        Employee emp = employee("Roberto");
        Position position = new Position();
        PositionLevel level = PositionLevel.fixed("Júnior", 1, position, new BigDecimal("1690"));
        CareerHistory history = new CareerHistory(
                emp, position, level, new BigDecimal("1690"), ContractType.PJ,
                com.proautokimium.api.domain.enums.humanResources.CareerChangeReason.HIRING,
                LocalDate.of(2026, 1, 1), null
        );

        when(employeeRepository.findByAtivoTrue()).thenReturn(List.of(emp));
        when(careerHistoryRepository.findLatestPerEmployee()).thenReturn(List.of(history));
        when(vacationRequestRepository.findByStatus(VacationRequestStatus.APPROVED)).thenReturn(List.of());

        List<TeamOverviewEntryDTO> result = service.getOverview(null, null);

        assertThat(result.get(0).contractType()).isEqualTo(ContractType.PJ);
    }

    @Test
    @DisplayName("Funcionário sem CareerHistory aparece com contractType nulo, não quebra")
    void semCareerHistoryContractTypeFicaNulo() throws Exception {
        Employee emp = employee("Sem Histórico");

        when(employeeRepository.findByAtivoTrue()).thenReturn(List.of(emp));
        when(careerHistoryRepository.findLatestPerEmployee()).thenReturn(List.of());
        when(vacationRequestRepository.findByStatus(VacationRequestStatus.APPROVED)).thenReturn(List.of());

        List<TeamOverviewEntryDTO> result = service.getOverview(null, null);

        assertThat(result.get(0).contractType()).isNull();
    }

    @Test
    @DisplayName("Filtro por teamId exclui quem não é do setor")
    void filtroPorTeamExcluiOutrosSetores() throws Exception {
        Team compras = new Team();
        Field teamIdField = com.proautokimium.api.domain.abstractions.Entity.class.getDeclaredField("id");
        teamIdField.setAccessible(true);
        UUID comprasId = UUID.randomUUID();
        teamIdField.set(compras, comprasId);

        Employee doCompras = employee("Do Compras");
        doCompras.setTeam(compras);

        Team rh = new Team();
        teamIdField.set(rh, UUID.randomUUID());
        Employee deOutroSetor = employee("De Outro Setor");
        deOutroSetor.setTeam(rh);

        when(employeeRepository.findByAtivoTrue()).thenReturn(List.of(doCompras, deOutroSetor));
        when(careerHistoryRepository.findLatestPerEmployee()).thenReturn(List.of());
        when(vacationRequestRepository.findByStatus(VacationRequestStatus.APPROVED)).thenReturn(List.of());

        List<TeamOverviewEntryDTO> result = service.getOverview(comprasId, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Do Compras");
    }
}
