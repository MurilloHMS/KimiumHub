package com.proautokimium.api.Infrastructure.services.home;

import com.proautokimium.api.Application.DTOs.holerite.HoleriteResponseDTO;
import com.proautokimium.api.Application.DTOs.home.HomeSummaryDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Reimbursement.ReimbursementResponseDTO;
import com.proautokimium.api.Application.DTOs.humanResources.VacationRequest.EmployeeVacationOverviewDTO;
import com.proautokimium.api.Application.DTOs.humanResources.VacationRequest.VacationRequestResponseDTO;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.services.holerite.HoleriteService;
import com.proautokimium.api.Infrastructure.services.humanResources.ReimbursementService;
import com.proautokimium.api.Infrastructure.services.humanResources.VacationRequestService;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.enums.HoleriteTipo;
import com.proautokimium.api.domain.enums.home.PendingType;
import com.proautokimium.api.domain.enums.humanResources.ReimbursementStatus;
import com.proautokimium.api.domain.enums.humanResources.VacationRequestStatus;
import com.proautokimium.api.domain.exceptions.partners.EmployeeNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * O resumo da home.
 *
 * O que estes testes protegem: o que **não** deve aparecer. Uma home que mostra
 * pendência já resolvida perde a confiança de quem a lê, e a partir daí ninguém
 * olha mais — que é o mesmo que não existir.
 */
@ExtendWith(MockitoExtension.class)
class HomeSummaryServiceTest {

    private static final String LOGIN = "murillo";

    @Mock private HoleriteService holeriteService;
    @Mock private VacationRequestService vacationRequestService;
    @Mock private ReimbursementService reimbursementService;
    @Mock private EmployeeRepository employeeRepository;

    @InjectMocks private HomeSummaryService service;

    // ─── Fábricas ────────────────────────────────────────────────────────────

    private HoleriteResponseDTO holerite(LocalDate competencia, LocalDateTime confirmedAt) {
        return new HoleriteResponseDTO(UUID.randomUUID(), competencia, HoleriteTipo.SALARIO,
                "holerite.pdf", LocalDateTime.of(2026, 8, 1, 9, 0), null, confirmedAt);
    }

    private VacationRequestResponseDTO ferias(VacationRequestStatus status, UUID employeeId) {
        return new VacationRequestResponseDTO(UUID.randomUUID(), employeeId,
                LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 20), 10L, null,
                status, LocalDateTime.of(2026, 8, 5, 10, 0), null, null, null);
    }

    private ReimbursementResponseDTO reembolso(ReimbursementStatus status, UUID employeeId) {
        return new ReimbursementResponseDTO(UUID.randomUUID(), employeeId,
                LocalDate.of(2026, 8, 2), new BigDecimal("120.00"), "Viagem", "Almoço",
                "nota.pdf", status, LocalDateTime.of(2026, 8, 3, 8, 0),
                null, null, null, null, null);
    }

    private void semDadosPessoais() {
        when(holeriteService.listarDoFuncionario(LOGIN)).thenReturn(List.of());
        when(vacationRequestService.getMyOverview(LOGIN))
                .thenReturn(new EmployeeVacationOverviewDTO(null, List.of()));
        when(reimbursementService.listMine(LOGIN)).thenReturn(List.of());
    }

    // ─── Pendências do próprio usuário ───────────────────────────────────────

    /**
     * O critério é `confirmedAt`, não `openedAt`. Abrir é ter olhado, confirmar
     * é ter recebido — e é o segundo que a auditoria do RH cobra.
     */
    @Test
    @DisplayName("Holerite confirmado não é pendência; o não confirmado é")
    void apenasHoleriteNaoConfirmadoEntra() {
        when(holeriteService.listarDoFuncionario(LOGIN)).thenReturn(List.of(
                holerite(LocalDate.of(2026, 7, 1), LocalDateTime.of(2026, 7, 5, 12, 0)),
                holerite(LocalDate.of(2026, 8, 1), null)
        ));
        when(vacationRequestService.getMyOverview(LOGIN))
                .thenReturn(new EmployeeVacationOverviewDTO(30, List.of()));
        when(reimbursementService.listMine(LOGIN)).thenReturn(List.of());

        HomeSummaryDTO resumo = service.getSummary(LOGIN, false);

        assertThat(resumo.mine()).hasSize(1);
        assertThat(resumo.mine().get(0).type()).isEqualTo(PendingType.HOLERITE_NAO_CONFIRMADO);
        assertThat(resumo.mine().get(0).title()).isEqualTo("Holerite de 08/2026");
        assertThat(resumo.vacationBalanceDays()).isEqualTo(30);
    }

    @Test
    @DisplayName("Férias e reembolso só entram quando estão PENDING")
    void apenasPendentesEntram() {
        UUID eu = UUID.randomUUID();
        when(holeriteService.listarDoFuncionario(LOGIN)).thenReturn(List.of());
        when(vacationRequestService.getMyOverview(LOGIN)).thenReturn(
                new EmployeeVacationOverviewDTO(15, List.of(
                        ferias(VacationRequestStatus.APPROVED, eu),
                        ferias(VacationRequestStatus.PENDING, eu))));
        when(reimbursementService.listMine(LOGIN)).thenReturn(List.of(
                reembolso(ReimbursementStatus.PAID, eu),
                reembolso(ReimbursementStatus.PENDING, eu)));

        HomeSummaryDTO resumo = service.getSummary(LOGIN, false);

        assertThat(resumo.mine()).extracting("type").containsExactlyInAnyOrder(
                PendingType.FERIAS_AGUARDANDO, PendingType.REEMBOLSO_AGUARDANDO);
    }

    /**
     * Conta de sistema, ou ADMIN que não está no cadastro de pessoas. A home é
     * a primeira tela depois do login: ela não pode dar 500 por isso.
     */
    @Test
    @DisplayName("Login sem funcionário vinculado devolve resumo vazio, não erro")
    void loginSemFuncionarioNaoDerrubaAHome() {
        when(holeriteService.listarDoFuncionario(LOGIN)).thenThrow(new EmployeeNotFoundException());

        HomeSummaryDTO resumo = service.getSummary(LOGIN, false);

        assertThat(resumo.mine()).isEmpty();
        assertThat(resumo.approvals()).isEmpty();
        assertThat(resumo.vacationBalanceDays()).isNull();
    }

    /** Lista vazia é "não há nada". Nulo obrigaria toda tela a checar antes de iterar. */
    @Test
    @DisplayName("Sem pendência nenhuma, as listas vêm vazias e não nulas")
    void listasNuncaVemNulas() {
        semDadosPessoais();

        HomeSummaryDTO resumo = service.getSummary(LOGIN, false);

        assertThat(resumo.mine()).isNotNull().isEmpty();
        assertThat(resumo.approvals()).isNotNull().isEmpty();
    }

    // ─── Aprovações ──────────────────────────────────────────────────────────

    /**
     * **O teste que fecha o buraco de dados.**
     *
     * A lista de aprovações carrega nome e valor de outras pessoas. Se ela
     * viesse sempre e o front escondesse, os dados chegariam ao navegador de
     * quem não deveria vê-los — bastaria abrir a aba Network.
     */
    @Test
    @DisplayName("Quem não é RH recebe approvals vazia, e nem consulta é feita")
    void naoRhNaoRecebeAprovacoes() {
        semDadosPessoais();

        HomeSummaryDTO resumo = service.getSummary(LOGIN, false);

        assertThat(resumo.approvals()).isEmpty();
        verify(vacationRequestService, never()).listAll(any());
        verify(reimbursementService, never()).listAll(any());
    }

    @Test
    @DisplayName("RH recebe as aprovações com o nome de quem pediu")
    void rhRecebeAprovacoesComNome() {
        UUID outro = UUID.randomUUID();
        semDadosPessoais();

        when(vacationRequestService.listAll(VacationRequestStatus.PENDING))
                .thenReturn(List.of(ferias(VacationRequestStatus.PENDING, outro)));
        when(reimbursementService.listAll(ReimbursementStatus.PENDING))
                .thenReturn(List.of(reembolso(ReimbursementStatus.PENDING, outro)));

        Employee ana = mock(Employee.class);
        when(ana.getId()).thenReturn(outro);
        when(ana.getName()).thenReturn("Ana Souza");
        when(employeeRepository.findAllById(any())).thenReturn(List.of(ana));

        HomeSummaryDTO resumo = service.getSummary(LOGIN, true);

        assertThat(resumo.approvals()).hasSize(2);
        assertThat(resumo.approvals()).allSatisfy(item ->
                assertThat(item.title()).isEqualTo("Ana Souza"));
    }

    /**
     * Funcionário apagado depois do pedido deixa o id sem nome. A linha não
     * pode sumir — o pedido continua parado esperando alguém.
     */
    @Test
    @DisplayName("Pedido de funcionário sem nome resolvido ainda aparece")
    void aprovacaoSobreviveASemNome() {
        semDadosPessoais();

        when(vacationRequestService.listAll(VacationRequestStatus.PENDING))
                .thenReturn(List.of(ferias(VacationRequestStatus.PENDING, UUID.randomUUID())));
        when(reimbursementService.listAll(ReimbursementStatus.PENDING)).thenReturn(List.of());
        when(employeeRepository.findAllById(any())).thenReturn(List.of());

        HomeSummaryDTO resumo = service.getSummary(LOGIN, true);

        assertThat(resumo.approvals()).hasSize(1);
        assertThat(resumo.approvals().get(0).title()).isEqualTo("Funcionário");
    }
}
