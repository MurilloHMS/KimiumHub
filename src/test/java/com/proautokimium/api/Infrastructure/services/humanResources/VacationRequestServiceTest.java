package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.VacationRequest.CreateVacationRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.VacationRequest.ReviewVacationRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.VacationRequest.VacationRequestResponseDTO;
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
        UUID reviewerId = UUID.randomUUID();

        when(vacationRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(employeeRepository.findById(reviewerId)).thenReturn(Optional.of(reviewer));
        when(vacationRequestRepository.save(any(VacationRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        service.approve(requestId, new ReviewVacationRequestDTO(reviewerId, "Aprovado"));

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
        UUID reviewerId = UUID.randomUUID();

        when(vacationRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(employeeRepository.findById(reviewerId)).thenReturn(Optional.of(reviewer));
        when(vacationRequestRepository.save(any(VacationRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        service.reject(requestId, new ReviewVacationRequestDTO(reviewerId, "Conflito de setor"));

        assertThat(employee.getVacationBalanceDays()).isEqualTo(12);
        verify(employeeRepository, never()).save(any());
    }
}
