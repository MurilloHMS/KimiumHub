package com.proautokimium.api.Infrastructure.services.home;

import com.proautokimium.api.Application.DTOs.home.HomeSummaryDTO;
import com.proautokimium.api.Application.DTOs.home.PendingItemDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Reimbursement.ReimbursementResponseDTO;
import com.proautokimium.api.Application.DTOs.humanResources.VacationRequest.EmployeeVacationOverviewDTO;
import com.proautokimium.api.Application.DTOs.humanResources.VacationRequest.VacationRequestResponseDTO;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.services.holerite.HoleriteService;
import com.proautokimium.api.Infrastructure.services.humanResources.ReimbursementService;
import com.proautokimium.api.Infrastructure.services.humanResources.VacationRequestService;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.enums.home.PendingType;
import com.proautokimium.api.domain.enums.humanResources.ReimbursementStatus;
import com.proautokimium.api.domain.enums.humanResources.VacationRequestStatus;
import com.proautokimium.api.domain.exceptions.partners.EmployeeNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Junta numa resposta só o que está esperando o usuário logado.
 *
 * Não consulta repositório novo: compõe os serviços que já respondem essas
 * perguntas separadamente. O ganho é a home fazer uma chamada em vez de cinco,
 * logo na primeira tela depois do login.
 *
 * Segue o formato do `HrDashboardService`, que já é o agregador do hub de RH —
 * a diferença é que aquele resume a empresa e este resume uma pessoa.
 */
@Slf4j
@Service
public class HomeSummaryService {

    private static final DateTimeFormatter COMPETENCIA = DateTimeFormatter.ofPattern("MM/yyyy");
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");

    private final HoleriteService holeriteService;
    private final VacationRequestService vacationRequestService;
    private final ReimbursementService reimbursementService;
    private final EmployeeRepository employeeRepository;

    public HomeSummaryService(HoleriteService holeriteService,
                              VacationRequestService vacationRequestService,
                              ReimbursementService reimbursementService,
                              EmployeeRepository employeeRepository) {
        this.holeriteService = holeriteService;
        this.vacationRequestService = vacationRequestService;
        this.reimbursementService = reimbursementService;
        this.employeeRepository = employeeRepository;
    }

    /**
     * @param isRh quem decide é o controller, a partir dos papéis do token. A
     *             lista de aprovações **não** pode ser filtrada no front: ela
     *             carrega nome e valor de outras pessoas, e chegaria ao
     *             navegador de quem não deveria vê-la.
     */
    public HomeSummaryDTO getSummary(String login, boolean isRh) {
        List<PendingItemDTO> mine = new ArrayList<>();
        Integer saldoFerias = null;

        // Usuário sem funcionário vinculado é caso real: conta de sistema, ou
        // um ADMIN que não está no cadastro de pessoas. A home é a primeira
        // tela depois do login — ela não pode dar 500 por isso. Sem vínculo,
        // não há pendência pessoal, e o resto da página continua de pé.
        try {
            mine.addAll(holeritesNaoConfirmados(login));

            EmployeeVacationOverviewDTO ferias = vacationRequestService.getMyOverview(login);
            saldoFerias = ferias.vacationBalanceDays();
            mine.addAll(feriasAguardando(ferias));

            mine.addAll(reembolsosAguardando(login));

        } catch (EmployeeNotFoundException e) {
            log.debug("Login {} não tem funcionário vinculado — home sem pendências pessoais", login);
        }

        mine.sort(maisAntigaPrimeiro());

        return new HomeSummaryDTO(mine, isRh ? aprovacoesPendentes() : List.of(), saldoFerias);
    }

    /**
     * Holerite entregue e ainda não confirmado.
     *
     * O critério é `confirmedAt`, não `openedAt`: abrir é ter olhado, confirmar
     * é ter recebido. A auditoria do RH cobra o segundo.
     */
    private List<PendingItemDTO> holeritesNaoConfirmados(String login) {
        return holeriteService.listarDoFuncionario(login).stream()
                .filter(h -> h.confirmedAt() == null)
                .map(h -> new PendingItemDTO(
                        PendingType.HOLERITE_NAO_CONFIRMADO,
                        "Holerite de " + h.competencia().format(COMPETENCIA),
                        "Ainda não confirmado",
                        h.createdAt()))
                .toList();
    }

    private List<PendingItemDTO> feriasAguardando(EmployeeVacationOverviewDTO ferias) {
        return ferias.requests().stream()
                .filter(r -> r.status() == VacationRequestStatus.PENDING)
                .map(r -> new PendingItemDTO(
                        PendingType.FERIAS_AGUARDANDO,
                        "Férias de " + r.startDate().format(DIA_MES) + " a " + r.endDate().format(DIA_MES),
                        "Aguardando aprovação",
                        r.requestedAt()))
                .toList();
    }

    private List<PendingItemDTO> reembolsosAguardando(String login) {
        return reimbursementService.listMine(login).stream()
                .filter(r -> r.status() == ReimbursementStatus.PENDING)
                .map(r -> new PendingItemDTO(
                        PendingType.REEMBOLSO_AGUARDANDO,
                        "Reembolso de " + moeda(r),
                        "Aguardando aprovação",
                        r.requestedAt()))
                .toList();
    }

    /**
     * O que está parado esperando este gestor.
     *
     * Os DTOs de férias e reembolso carregam `employeeId`, não o nome — e um
     * gestor precisa saber de quem é o pedido antes de abrir a tela. Os nomes
     * saem numa consulta só, por `findAllById`, em vez de uma por linha.
     */
    private List<PendingItemDTO> aprovacoesPendentes() {
        List<VacationRequestResponseDTO> ferias =
                vacationRequestService.listAll(VacationRequestStatus.PENDING);
        List<ReimbursementResponseDTO> reembolsos =
                reimbursementService.listAll(ReimbursementStatus.PENDING);

        Map<UUID, String> nomes = nomesPorId(ferias, reembolsos);
        List<PendingItemDTO> itens = new ArrayList<>();

        for (VacationRequestResponseDTO f : ferias) {
            itens.add(new PendingItemDTO(
                    PendingType.APROVACAO_FERIAS,
                    nomes.getOrDefault(f.employeeId(), "Funcionário"),
                    "Férias de " + f.startDate().format(DIA_MES) + " a " + f.endDate().format(DIA_MES),
                    f.requestedAt()));
        }

        for (ReimbursementResponseDTO r : reembolsos) {
            itens.add(new PendingItemDTO(
                    PendingType.APROVACAO_REEMBOLSO,
                    nomes.getOrDefault(r.employeeId(), "Funcionário"),
                    "Reembolso de " + moeda(r),
                    r.requestedAt()));
        }

        itens.sort(maisAntigaPrimeiro());
        return itens;
    }

    private Map<UUID, String> nomesPorId(List<VacationRequestResponseDTO> ferias,
                                         List<ReimbursementResponseDTO> reembolsos) {
        Set<UUID> ids = new HashSet<>();
        ferias.forEach(f -> ids.add(f.employeeId()));
        reembolsos.forEach(r -> ids.add(r.employeeId()));
        ids.remove(null);

        Map<UUID, String> nomes = new HashMap<>();
        if (ids.isEmpty()) return nomes;

        for (Employee e : employeeRepository.findAllById(ids)) {
            nomes.put(e.getId(), e.getName());
        }
        return nomes;
    }

    /**
     * Mais antiga no topo: pendência esquecida há duas semanas incomoda mais do
     * que a de hoje. `nullsLast` porque data nula não pode derrubar a ordenação
     * inteira.
     */
    private Comparator<PendingItemDTO> maisAntigaPrimeiro() {
        return Comparator.comparing(PendingItemDTO::since,
                Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private String moeda(ReimbursementResponseDTO r) {
        return r.amount() == null ? "valor não informado" : "R$ " + r.amount();
    }
}
