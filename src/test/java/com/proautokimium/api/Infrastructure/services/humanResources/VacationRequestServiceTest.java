package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.VacationRequest.CreateVacationRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.VacationRequest.CreateVacationByRhDTO;
import com.proautokimium.api.Application.DTOs.humanResources.VacationRequest.EmployeeVacationOverviewDTO;
import com.proautokimium.api.Application.DTOs.humanResources.VacationRequest.ReviewVacationRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.VacationRequest.VacationRequestResponseDTO;
import com.proautokimium.api.domain.exceptions.partners.EmployeeNotFoundException;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.InsufficientVacationBalanceException;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.OverlappingVacationRequestException;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.CareerHistoryRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.VacationRequestRepository;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.entities.humanResources.Team;
import com.proautokimium.api.domain.entities.humanResources.VacationRequest;
import org.hibernate.validator.constraints.ModCheck;
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
    @Mock private CareerHistoryRepository careerHistoryRepository;
    @Mock private BrazilianBusinessDayCalculator brazilianBussinessCalculator;

    private VacationRequestService service;

    private static final String LOGIN = "murillo.login";
    private Employee employee;
    private Team team;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(LocalDateTime.of(2026, 7, 23, 10, 0).atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        service = new VacationRequestService(vacationRequestRepository, employeeRepository, userRepository, careerHistoryRepository, clock, brazilianBussinessCalculator);

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

        when(brazilianBussinessCalculator.countBusinessDays(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10)))
                .thenReturn(6L);

        CreateVacationRequestDTO dto = new CreateVacationRequestDTO(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10), null
        );

        VacationRequestResponseDTO response = service.create(dto, LOGIN);

        assertThat(response.daysRequested()).isEqualTo(6);
        assertThat(response.status().name()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("Não deve criar solicitação além do saldo disponível")
    void naoDeveCriarSolicitacaoAlemDoSaldo() {
        employee.setVacationBalanceDays(5);
        mockAuthenticatedEmployee();
        when(brazilianBussinessCalculator.countBusinessDays(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10)))
                .thenReturn(6L);

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
        when(brazilianBussinessCalculator.countBusinessDays(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10)))
                .thenReturn(6L);

        service.approve(requestId, new ReviewVacationRequestDTO("Aprovado"), reviewerLogin);

        assertThat(employee.getVacationBalanceDays()).isEqualTo(6); // 12 - 6 business days
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

    // ─── O saldo no lançamento do RH ─────────────────────────────────────────

    /** O lançamento do RH nasce aprovado: quem lança não pede a si mesmo. */
    private CreateVacationByRhDTO lancamento(Integer saldoInformado) {
        return new CreateVacationByRhDTO(
                UUID.randomUUID(),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 10),
                saldoInformado,
                "lançado pelo RH");
    }

    private void prepararLancamento() {
        mockAuthenticatedEmployee();
        when(employeeRepository.findById(any())).thenReturn(Optional.of(employee));
        when(vacationRequestRepository.findOverlappingInTeam(eq(team), eq(employee), any(), any()))
                .thenReturn(List.of());
        when(vacationRequestRepository.save(any(VacationRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(brazilianBussinessCalculator.countBusinessDays(any(), any())).thenReturn(8L);
    }

    /**
     * **O saldo informado é o saldo DEPOIS do lançamento.**
     *
     * Quando o RH digita o número, ele está corrigindo o cadastro — "esta
     * pessoa fica com 10 dias" — e não dando uma entrada para descontar.
     *
     * O código fazia as duas coisas: gravava o valor digitado e descontava por
     * cima. Lançar 10 dias informando saldo 10 terminava em 2, e o sintoma era
     * o RH digitar o número certo e ver outro na tela.
     */
    @Test
    @DisplayName("saldo informado é gravado como está, sem desconto por cima")
    void saldoInformadoNaoEhDescontado() {
        prepararLancamento();

        service.createByRh(lancamento(10), LOGIN);

        assertThat(employee.getVacationBalanceDays())
                .as("o RH disse 10, e 10 é o que fica")
                .isEqualTo(10);
    }

    /**
     * **Zero é um saldo, não é "não informado".**
     *
     * É o caso que a checagem por `!= null` protege e uma por `> 0` quebraria:
     * o RH lança as últimas férias da pessoa e diz que ela fica zerada. Se zero
     * caísse no ramo do desconto, o saldo ficaria NEGATIVO — e ninguém olha um
     * campo que já estava em zero.
     */
    @Test
    @DisplayName("saldo informado como zero fica zero")
    void saldoZeroFicaZero() {
        prepararLancamento();

        service.createByRh(lancamento(0), LOGIN);

        assertThat(employee.getVacationBalanceDays()).isZero();
    }

    /**
     * Em branco é o outro caso: usa o que o sistema já sabe e desconta.
     *
     * Sem este par, "não descontar quando informado" viraria "nunca descontar"
     * num refactor, e o saldo pararia de cair sem ninguém notar.
     */
    @Test
    @DisplayName("sem saldo informado, desconta os dias úteis do saldo atual")
    void semSaldoInformadoDesconta() {
        prepararLancamento();

        service.createByRh(lancamento(null), LOGIN);

        assertThat(employee.getVacationBalanceDays())
                .as("12 de saldo menos 8 dias úteis")
                .isEqualTo(4);
    }
}
