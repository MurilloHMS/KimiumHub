package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.VacationRequest.CreateVacationRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.VacationRequest.CreateVacationByRhDTO;
import com.proautokimium.api.Application.DTOs.humanResources.VacationRequest.EmployeeVacationOverviewDTO;
import com.proautokimium.api.Application.DTOs.humanResources.VacationRequest.ReviewVacationRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.VacationRequest.VacationRequestResponseDTO;
import com.proautokimium.api.domain.exceptions.partners.EmployeeNotFoundException;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.InsufficientVacationBalanceException;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.OverlappingVacationRequestException;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.OwnVacationOverlapException;
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

        when(vacationRequestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));
        lenient().when(employeeRepository.findByIdForUpdate(any())).thenReturn(Optional.of(employee));
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

        when(vacationRequestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));
        lenient().when(employeeRepository.findByIdForUpdate(any())).thenReturn(Optional.of(employee));
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
        // Quem lança é o RH, outra pessoa: o lançamento nasce aprovado, e
        // ninguém aprova as próprias férias.
        when(userRepository.findByLoginWithEmployee(LOGIN)).thenReturn(Optional.empty());
        when(employeeRepository.findByUsername(LOGIN)).thenReturn(Optional.of(new Employee()));
        when(employeeRepository.findByIdForUpdate(any())).thenReturn(Optional.of(employee));
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

    // ─── O saldo não fica negativo ───────────────────────────────────────────
    //
    // O saldo era conferido só ao CRIAR o pedido, e sem contar os outros
    // pendentes. Com 10 de saldo, dois pedidos de 8 passavam na criação, o RH
    // aprovava os dois, e o saldo terminava em -6.
    //
    // Os stubs que dependem da ordem da correção (conferir o saldo antes ou
    // depois de resolver o revisor, de olhar a sobreposição) são `lenient`:
    // o teste afirma o resultado, não o caminho.

    /**
     * **Aprovar confere o saldo de novo.**
     *
     * Dois asserts, como nas máquinas de estado: a exceção, e o estado que
     * ficou intacto. Sem o segundo, uma correção que confere o saldo DEPOIS
     * de `request.approve(...)` passaria — e o pedido ficaria APPROVED na
     * memória, pronto para qualquer save posterior gravar.
     */
    @Test
    @DisplayName("aprovar além do saldo é recusado, e nada muda")
    void aprovarAlemDoSaldoEhRecusado() {
        VacationRequest request = VacationRequest.request(
                employee, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10),
                null, LocalDateTime.of(2026, 7, 20, 9, 0)
        );
        UUID requestId = UUID.randomUUID();
        employee.setVacationBalanceDays(5);

        when(vacationRequestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));
        lenient().when(employeeRepository.findByIdForUpdate(any())).thenReturn(Optional.of(employee));
        lenient().when(userRepository.findByLoginWithEmployee("reviewer.login")).thenReturn(Optional.empty());
        lenient().when(employeeRepository.findByUsername("reviewer.login")).thenReturn(Optional.of(new Employee()));
        // Com o defeito, o código segue e grava — o save precisa responder
        // para o teste falhar pelo motivo certo (nenhuma exceção), e não por NPE.
        lenient().when(vacationRequestRepository.save(any(VacationRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(brazilianBussinessCalculator.countBusinessDays(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10)))
                .thenReturn(6L);

        assertThrows(InsufficientVacationBalanceException.class,
                () -> service.approve(requestId, new ReviewVacationRequestDTO("ok"), "reviewer.login"));

        assertThat(employee.getVacationBalanceDays()).as("o saldo não se mexe").isEqualTo(5);
        assertThat(request.getStatus().name()).as("o pedido continua pendente").isEqualTo("PENDING");
        verify(employeeRepository, never()).save(any());
        verify(vacationRequestRepository, never()).save(any());
    }

    /** O limite é o saldo inteiro: usar tudo é permitido, e termina em zero. */
    @Test
    @DisplayName("aprovar exatamente o saldo é permitido, e zera")
    void aprovarExatamenteOSaldoZera() {
        VacationRequest request = VacationRequest.request(
                employee, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10),
                null, LocalDateTime.of(2026, 7, 20, 9, 0)
        );
        UUID requestId = UUID.randomUUID();
        employee.setVacationBalanceDays(6);

        when(vacationRequestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));
        lenient().when(employeeRepository.findByIdForUpdate(any())).thenReturn(Optional.of(employee));
        lenient().when(userRepository.findByLoginWithEmployee("reviewer.login")).thenReturn(Optional.empty());
        lenient().when(employeeRepository.findByUsername("reviewer.login")).thenReturn(Optional.of(new Employee()));
        when(vacationRequestRepository.save(any(VacationRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(brazilianBussinessCalculator.countBusinessDays(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10)))
                .thenReturn(6L);

        service.approve(requestId, new ReviewVacationRequestDTO("ok"), "reviewer.login");

        assertThat(employee.getVacationBalanceDays()).isZero();
    }

    /**
     * **O lançamento do RH sem saldo informado também não passa do saldo.**
     *
     * Com saldo informado a conta é outra — o RH está corrigindo o cadastro,
     * e o número dele vale (ver `saldoInformadoNaoEhDescontado`). Em branco, o
     * sistema desconta, e descontar além do que existe é o mesmo -6 da
     * aprovação.
     */
    @Test
    @DisplayName("lançamento do RH sem saldo informado não passa do saldo")
    void lancamentoSemSaldoInformadoNaoPassaDoSaldo() {
        employee.setVacationBalanceDays(5);
        lenient().when(userRepository.findByLoginWithEmployee(LOGIN)).thenReturn(Optional.empty());
        lenient().when(employeeRepository.findByUsername(LOGIN)).thenReturn(Optional.of(new Employee()));
        when(employeeRepository.findByIdForUpdate(any())).thenReturn(Optional.of(employee));
        lenient().when(vacationRequestRepository.findOverlappingInTeam(eq(team), eq(employee), any(), any()))
                .thenReturn(List.of());
        lenient().when(vacationRequestRepository.save(any(VacationRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(brazilianBussinessCalculator.countBusinessDays(any(), any())).thenReturn(8L);

        assertThrows(InsufficientVacationBalanceException.class,
                () -> service.createByRh(lancamento(null), LOGIN));

        assertThat(employee.getVacationBalanceDays()).isEqualTo(5);
        verify(employeeRepository, never()).save(any());
        verify(vacationRequestRepository, never()).save(any());
    }

    // ─── Sobreposição com as próprias férias ─────────────────────────────────
    //
    // A consulta do setor exclui o próprio funcionário e só roda para quem tem
    // time. O mesmo pedido podia ser feito duas vezes, e aprovar os dois
    // descontava o saldo duas vezes.
    //
    // Stubs `lenient`: com o defeito, o código não consulta a sobreposição
    // própria e segue até o save — o teste precisa falhar por "nada lançado",
    // e não por stub não usado.

    @Test
    @DisplayName("não cria pedido que cruza férias do próprio funcionário")
    void naoCriaPedidoQueCruzaAsProprias() {
        mockAuthenticatedEmployee();
        lenient().when(vacationRequestRepository.existsOverlapForEmployee(eq(employee), any(), any(), any()))
                .thenReturn(true);
        lenient().when(vacationRequestRepository.findOverlappingInTeam(any(), any(), any(), any())).thenReturn(List.of());
        lenient().when(vacationRequestRepository.save(any(VacationRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(brazilianBussinessCalculator.countBusinessDays(any(), any())).thenReturn(6L);

        CreateVacationRequestDTO dto = new CreateVacationRequestDTO(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10), null);

        assertThrows(OwnVacationOverlapException.class, () -> service.create(dto, LOGIN));
        verify(vacationRequestRepository, never()).save(any());
    }

    /** Quem não tem time nunca passava por verificação nenhuma. */
    @Test
    @DisplayName("sem time, a sobreposição própria é conferida do mesmo jeito")
    void semTimeTambemConfere() {
        employee.setTeam(null);
        mockAuthenticatedEmployee();
        lenient().when(vacationRequestRepository.existsOverlapForEmployee(eq(employee), any(), any(), any()))
                .thenReturn(true);
        lenient().when(vacationRequestRepository.save(any(VacationRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(brazilianBussinessCalculator.countBusinessDays(any(), any())).thenReturn(6L);

        CreateVacationRequestDTO dto = new CreateVacationRequestDTO(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10), null);

        assertThrows(OwnVacationOverlapException.class, () -> service.create(dto, LOGIN));
    }

    @Test
    @DisplayName("o lançamento do RH não cruza férias do próprio funcionário")
    void lancamentoNaoCruzaAsProprias() {
        lenient().when(userRepository.findByLoginWithEmployee(LOGIN)).thenReturn(Optional.empty());
        lenient().when(employeeRepository.findByUsername(LOGIN)).thenReturn(Optional.of(new Employee()));
        when(employeeRepository.findByIdForUpdate(any())).thenReturn(Optional.of(employee));
        lenient().when(vacationRequestRepository.existsOverlapForEmployee(eq(employee), any(), any(), any()))
                .thenReturn(true);
        lenient().when(vacationRequestRepository.findOverlappingInTeam(any(), any(), any(), any())).thenReturn(List.of());
        lenient().when(vacationRequestRepository.save(any(VacationRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(brazilianBussinessCalculator.countBusinessDays(any(), any())).thenReturn(8L);

        assertThrows(OwnVacationOverlapException.class, () -> service.createByRh(lancamento(null), LOGIN));
        assertThat(employee.getVacationBalanceDays()).as("o saldo não se mexe").isEqualTo(12);
        verify(vacationRequestRepository, never()).save(any());
    }

    /**
     * **Aprovar confere só contra as APROVADAS.** Pedidos duplicados feitos
     * antes desta correção continuam pendentes na base; aprovar o segundo
     * descontaria de novo.
     *
     * O stub responde só para `[APPROVED]`: se a correção consultar com
     * PENDING junto, o próprio pedido (pendente) conflitaria consigo mesmo — e
     * se consultar com outra lista, este teste não recebe o `true` e falha.
     */
    @Test
    @DisplayName("aprovar recusa quando já há férias aprovadas no período, e nada muda")
    void aprovarRecusaSeJaHaAprovadasNoPeriodo() {
        VacationRequest request = VacationRequest.request(
                employee, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10),
                null, LocalDateTime.of(2026, 7, 20, 9, 0));
        UUID requestId = UUID.randomUUID();

        when(vacationRequestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));
        lenient().when(employeeRepository.findByIdForUpdate(any())).thenReturn(Optional.of(employee));
        lenient().when(userRepository.findByLoginWithEmployee("reviewer.login")).thenReturn(Optional.empty());
        lenient().when(employeeRepository.findByUsername("reviewer.login")).thenReturn(Optional.of(new Employee()));
        lenient().when(vacationRequestRepository.existsOverlapForEmployee(
                        eq(employee),
                        eq(List.of(com.proautokimium.api.domain.enums.humanResources.VacationRequestStatus.APPROVED)),
                        any(), any()))
                .thenReturn(true);
        lenient().when(vacationRequestRepository.save(any(VacationRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(brazilianBussinessCalculator.countBusinessDays(any(), any())).thenReturn(6L);

        assertThrows(OwnVacationOverlapException.class,
                () -> service.approve(requestId, new ReviewVacationRequestDTO("ok"), "reviewer.login"));

        assertThat(employee.getVacationBalanceDays()).isEqualTo(12);
        assertThat(request.getStatus().name()).isEqualTo("PENDING");
        verify(vacationRequestRepository, never()).save(any());
    }

    // ─── O RH não lança as próprias férias ───────────────────────────────────

    /**
     * O lançamento do RH nasce aprovado — então lançar as próprias férias é
     * aprovar o próprio pedido. Outra pessoa com a permissão lança.
     */
    @Test
    @DisplayName("o RH não lança as próprias férias, e o saldo não se mexe")
    void rhNaoLancaAsProprias() {
        mockAuthenticatedEmployee(); // o login do RH resolve para o próprio funcionário
        when(employeeRepository.findByIdForUpdate(any())).thenReturn(Optional.of(employee));
        lenient().when(vacationRequestRepository.findOverlappingInTeam(any(), any(), any(), any())).thenReturn(List.of());
        lenient().when(vacationRequestRepository.save(any(VacationRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(brazilianBussinessCalculator.countBusinessDays(any(), any())).thenReturn(8L);

        assertThrows(com.proautokimium.api.domain.exceptions.humanResources.SelfReviewException.class,
                () -> service.createByRh(lancamento(null), LOGIN));

        assertThat(employee.getVacationBalanceDays()).isEqualTo(12);
        verify(vacationRequestRepository, never()).save(any());
    }
}
