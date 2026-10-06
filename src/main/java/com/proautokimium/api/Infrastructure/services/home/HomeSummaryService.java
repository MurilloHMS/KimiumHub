package com.proautokimium.api.Infrastructure.services.home;

import com.proautokimium.api.Application.DTOs.humanResources.DocumentRequest.RecipientDTO;
import com.proautokimium.api.Infrastructure.services.humanResources.DocumentRequestService;
import com.proautokimium.api.domain.enums.humanResources.RecipientStatus;
import com.proautokimium.api.domain.enums.humanResources.RequestStatus;
import com.proautokimium.api.Application.DTOs.home.HomeSummaryDTO;
import com.proautokimium.api.Application.DTOs.home.PendingItemDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Reimbursement.ReimbursementResponseDTO;
import com.proautokimium.api.Application.DTOs.humanResources.VacationRequest.EmployeeVacationOverviewDTO;
import com.proautokimium.api.Application.DTOs.humanResources.VacationRequest.VacationRequestResponseDTO;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.services.events.EventAttendanceService;
import com.proautokimium.api.Infrastructure.services.holerite.HoleriteService;
import com.proautokimium.api.Infrastructure.services.humanResources.MedicalCertificateService;
import com.proautokimium.api.Infrastructure.services.humanResources.ReimbursementService;
import com.proautokimium.api.Application.DTOs.humanResources.MedicalCertificate.MedicalCertificateResponseDTO;
import com.proautokimium.api.domain.enums.humanResources.MedicalCertificateStatus;
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
    private final EventAttendanceService eventAttendanceService;
    private final MedicalCertificateService medicalCertificateService;
    private final DocumentRequestService documentRequestService;

    public HomeSummaryService(HoleriteService holeriteService,
                              VacationRequestService vacationRequestService,
                              ReimbursementService reimbursementService,
                              EmployeeRepository employeeRepository,
                              EventAttendanceService eventAttendanceService,
                              MedicalCertificateService medicalCertificateService,
                              DocumentRequestService documentRequestService) {
        this.holeriteService = holeriteService;
        this.vacationRequestService = vacationRequestService;
        this.reimbursementService = reimbursementService;
        this.employeeRepository = employeeRepository;
        this.eventAttendanceService = eventAttendanceService;
        this.medicalCertificateService = medicalCertificateService;
        this.documentRequestService = documentRequestService;
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

            mine.addAll(atestados(login));

            mine.addAll(solicitacoes(login));

        } catch (EmployeeNotFoundException e) {
            log.debug("Login {} não tem funcionário vinculado — home sem pendências pessoais", login);
        }

        // Fora do try: sem funcionário vinculado a lista já volta vazia, e um
        // convite não pode sumir porque o holerite falhou antes dele.
        mine.addAll(convitesSemResposta(login));

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
     * Atestado em conferência, e o recusado que ainda dá para reenviar. O
     * recusado fora do prazo some: não há mais o que a pessoa possa fazer.
     */
    private List<PendingItemDTO> atestados(String login) {
        List<PendingItemDTO> itens = new ArrayList<>();
        for (MedicalCertificateResponseDTO c : medicalCertificateService.listMine(login)) {
            String titulo = "Atestado de " + periodo(c);
            if (c.status() == MedicalCertificateStatus.PENDING) {
                itens.add(new PendingItemDTO(PendingType.ATESTADO_AGUARDANDO, titulo,
                        "Aguardando o RH confirmar", ultimoEnvio(c)));
            } else if (c.status() == MedicalCertificateStatus.REJECTED && c.resubmitDeadline() != null) {
                itens.add(new PendingItemDTO(PendingType.ATESTADO_RECUSADO, titulo,
                        "Recusado — envie outro arquivo até " + c.resubmitDeadline().format(DIA_MES),
                        c.reviewedAt()));
            }
        }
        return itens;
    }

    private static String periodo(MedicalCertificateResponseDTO c) {
        return c.startDate().equals(c.endDate())
                ? c.startDate().format(DIA_MES)
                : c.startDate().format(DIA_MES) + " a " + c.endDate().format(DIA_MES);
    }

    private static java.time.LocalDateTime ultimoEnvio(MedicalCertificateResponseDTO c) {
        return c.resubmittedAt() != null ? c.resubmittedAt() : c.submittedAt();
    }

    /**
     * Convite aberto e sem resposta. Quem respondeu "não vou" também respondeu:
     * some daqui, como some do lembrete. `since` é a publicação — o convite
     * nasceu ali.
     */
    /** Live de comunicado: a pendência pede o "Estou ciente", e não presença. */
    private static boolean isOnline(com.proautokimium.api.Application.DTOs.events.EventDTOs.EventSummaryDTO e) {
        return e.location() != null && "ONLINE".equals(e.location().source());
    }

    private List<PendingItemDTO> convitesSemResposta(String login) {
        return eventAttendanceService.pendingInvitations(login).stream()
                .map(i -> new PendingItemDTO(
                        PendingType.EVENT_RSVP,
                        isOnline(i.event()) ? "Confirme que está ciente" : "Confirme sua presença",
                        i.event().name() + " · " + i.event().startDate().format(DIA_MES),
                        i.event().publishedAt(),
                        i.event().id()))
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

        // O DTO do atestado já traz o nome: não entra no nomesPorId.
        for (MedicalCertificateResponseDTO c : medicalCertificateService.listAll(MedicalCertificateStatus.PENDING)) {
            itens.add(new PendingItemDTO(
                    PendingType.CONFERENCIA_ATESTADO,
                    c.employeeName() != null ? c.employeeName() : "Funcionário",
                    (c.resubmittedAt() != null ? "Atestado reenviado · " : "Atestado de ") + periodo(c),
                    ultimoEnvio(c)));
        }

        for (RecipientDTO r : documentRequestService.listAwaitingReview()) {
            itens.add(new PendingItemDTO(
                    PendingType.CONFERENCIA_SOLICITACAO,
                    r.employeeName() != null ? r.employeeName() : "Funcionário",
                    "Respondeu: " + r.requestTitle(),
                    r.submittedAt(),
                    r.id()));
        }

        itens.sort(maisAntigaPrimeiro());
        return itens;
    }

    /**
     * Solicitação aberta ainda sem resposta, e a devolvida para corrigir. A de
     * solicitação encerrada some: não há mais o que responder.
     */
    private List<PendingItemDTO> solicitacoes(String login) {
        List<PendingItemDTO> itens = new ArrayList<>();
        for (RecipientDTO r : documentRequestService.listMine(login)) {
            if (r.requestStatus() != RequestStatus.OPEN) continue;
            String prazo = r.requestDueDate() != null ? " até " + r.requestDueDate().format(DIA_MES) : "";
            if (r.status() == RecipientStatus.PENDING) {
                itens.add(new PendingItemDTO(PendingType.SOLICITACAO_PENDENTE, r.requestTitle(),
                        "Responda" + prazo, r.addedAt(), r.id()));
            } else if (r.status() == RecipientStatus.RETURNED) {
                itens.add(new PendingItemDTO(PendingType.SOLICITACAO_DEVOLVIDA, r.requestTitle(),
                        "Devolvida: " + r.returnReason(), r.reviewedAt(), r.id()));
            }
        }
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
