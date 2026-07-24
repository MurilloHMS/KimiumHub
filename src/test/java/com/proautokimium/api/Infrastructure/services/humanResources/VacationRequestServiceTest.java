package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.VacationRequest.CreateVacationRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.VacationRequest.EmployeeVacationOverviewDTO;
import com.proautokimium.api.Application.DTOs.humanResources.VacationRequest.ReviewVacationRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.VacationRequest.VacationRequestResponseDTO;
import com.proautokimium.api.domain.exceptions.partners.EmployeeNotFoundException;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.InsufficientVacationBalanceException;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.OverlappingVacationRequestException;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.VacationRequestRepository;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.entities.humanResources.Team;
import com.proautokimium.api.domain.entities.humanResources.VacationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VacationRequestServiceTest {

    @Mock private VacationRequestRepository vacationRequestRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private UserRepository userRepository;

    private VacationRequestService service;

    private static final String LOGIN = "murillo.login";
    private Employee employee;
    private Team team;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(LocalDateTime.of(2026, 7, 23, 10, 0).atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        service = new VacationRequestService(vacationRequestRepository, employeeRepository, userRepository, clock);

        employee = new Employee();
        employee.setVacationBalanceDays(12);

        team = new Team();
        employee.setTeam(team);
    }

    private void mockAuthenticatedEmployee() {
        User user = mock(User.class);
        when(user.getEmployee()).thenReturn(employee);
        when(userRepository.findByLoginWithEmployee(LOGIN)).thenReturn(Optional.of(user));
    }

    @Test
    @DisplayName("Deve criar solicitação quando há saldo e não há sobreposição no setor")
    void deveCriarSolicitacaoComSaldoESemSobreposicao() {
        mockAuthenticatedEmployee();
        when(vacationRequestRepository.findOverlappingInTeam(eq(team), eq(employee), any(), any()))
                .thenReturn(List.of());
        when(vacationRequestRepository.save(any(VacationRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateVacationRequestDTO dto = new CreateVacationRequestDTO(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10), null
        );

        VacationRequestResponseDTO response = service.create(dto, LOGIN);

        assertThat(response.daysRequested()).isEqualTo(10);
        assertThat(response.status().name()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("Não deve criar solicitação além do saldo disponível")
    void naoDeveCriarSolicitacaoAlemDoSaldo() {
        employee.setVacationBalanceDays(5);
        mockAuthenticatedEmployee();

        CreateVacationRequestDTO dto = new CreateVacationRequestDTO(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10), null
        );

        assertThrows(InsufficientVacationBalanceException.class, () -> service.create(dto, LOGIN));
        verify(vacationRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Não deve criar solicitação sobreposta a outro funcionário do mesmo setor")
    void naoDeveCriarSolicitacaoSobrepostaNoSetor() {
        mockAuthenticatedEmployee();
        when(vacationRequestRepository.findOverlappingInTeam(eq(team), eq(employee), any(), any()))
                .thenReturn(List.of(mock(VacationRequest.class)));

        CreateVacationRequestDTO dto = new CreateVacationRequestDTO(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10), null
        );

        assertThrows(OverlappingVacationRequestException.class, () -> service.create(dto, LOGIN));
        verify(vacationRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Aprovar desconta os dias do saldo do funcionário")
    void aprovarDevecontarDiasDoSaldo() {
        VacationRequest request = VacationRequest.request(
                employee, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10),
                null, LocalDateTime.of(2026, 7, 20, 9, 0)
        );
        UUID requestId = UUID.randomUUID();

        Employee reviewer = new Employee();
        String reviewerLogin = "reviewer.login";

        when(vacationRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(userRepository.findByLoginWithEmployee(reviewerLogin)).thenReturn(Optional.empty());
        when(employeeRepository.findByUsername(reviewerLogin)).thenReturn(Optional.of(reviewer));
        when(vacationRequestRepository.save(any(VacationRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        service.approve(requestId, new ReviewVacationRequestDTO("Aprovado"), reviewerLogin);

        assertThat(employee.getVacationBalanceDays()).isEqualTo(2); // 12 - 10
        verify(employeeRepository).save(employee);
    }

    @Test
    @DisplayName("Reprovar não mexe no saldo do funcionário")
    void reprovarNaoMexeNoSaldo() {
        VacationRequest request = VacationRequest.request(
                employee, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10),
                null, LocalDateTime.of(2026, 7, 20, 9, 0)
        );
        UUID requestId = UUID.randomUUID();
        Employee reviewer = new Employee();
        String reviewerLogin = "reviewer.login";

        when(vacationRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(userRepository.findByLoginWithEmployee(reviewerLogin)).thenReturn(Optional.empty());
        when(employeeRepository.findByUsername(reviewerLogin)).thenReturn(Optional.of(reviewer));
        when(vacationRequestRepository.save(any(VacationRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        service.reject(requestId, new ReviewVacationRequestDTO("Conflito de setor"), reviewerLogin);

        assertThat(employee.getVacationBalanceDays()).isEqualTo(12);
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("getMyOverview traz o saldo atual junto com o histórico de solicitações")
    void getMyOverviewTrazSaldoEHistorico() {
        VacationRequest request = VacationRequest.request(
                employee, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10),
                null, LocalDateTime.of(2026, 7, 20, 9, 0)
        );
        mockAuthenticatedEmployee();
        when(vacationRequestRepository.findByEmployeeOrderByRequestedAtDesc(employee))
                .thenReturn(List.of(request));

        EmployeeVacationOverviewDTO overview = service.getMyOverview(LOGIN);

        assertThat(overview.vacationBalanceDays()).isEqualTo(12);
        assertThat(overview.requests()).hasSize(1);
    }

    @Test
    @DisplayName("listAll sem status busca tudo, não filtra")
    void listAllSemStatusBuscaTudo() {
        VacationRequest pendente = VacationRequest.request(
                employee, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5), null, LocalDateTime.of(2026, 7, 1, 9, 0)
        );
        when(vacationRequestRepository.findAllByOrderByRequestedAtDesc()).thenReturn(List.of(pendente));

        assertThat(service.listAll(null)).hasSize(1);
        verify(vacationRequestRepository, never()).findByStatusOrderByRequestedAtDesc(any());
    }

    @Test
    @DisplayName("listAll com status repassa o filtro pro repositório")
    void listAllComStatusFiltraNoRepositorio() {
        VacationRequest pendente = VacationRequest.request(
                employee, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5), null, LocalDateTime.of(2026, 7, 1, 9, 0)
        );
        when(vacationRequestRepository.findByStatusOrderByRequestedAtDesc(
                com.proautokimium.api.domain.enums.humanResources.VacationRequestStatus.PENDING))
                .thenReturn(List.of(pendente));

        assertThat(service.listAll(com.proautokimium.api.domain.enums.humanResources.VacationRequestStatus.PENDING)).hasSize(1);
        verify(vacationRequestRepository, never()).findAllByOrderByRequestedAtDesc();
    }

    @Test
    @DisplayName("getMyOverview lança exceção se o login não corresponde a nenhum funcionário")
    void getMyOverviewSemFuncionarioVinculado() {
        when(userRepository.findByLoginWithEmployee("sem-vinculo")).thenReturn(Optional.empty());
        when(employeeRepository.findByUsername("sem-vinculo")).thenReturn(Optional.empty());

        assertThrows(EmployeeNotFoundException.class, () -> service.getMyOverview("sem-vinculo"));
    }
}
