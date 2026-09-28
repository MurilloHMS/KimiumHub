package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Reimbursement.ReimbursementReportRowDTO;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.EmptyReimbursementReportException;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.ReimbursementReportException;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.ReimbursementRepository;
import com.proautokimium.api.Infrastructure.services.humanResources.ReimbursementReceiptAnnexes.ReceiptAnnex;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.Reimbursement;
import com.proautokimium.api.domain.enums.Department;
import com.proautokimium.api.domain.enums.humanResources.ReimbursementStatus;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidRequestDataException;
import com.proautokimium.api.domain.exceptions.partners.EmployeeNotFoundException;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.proautokimium.api.Infrastructure.utils.BrazilianFormat.date;
import static com.proautokimium.api.Infrastructure.utils.BrazilianFormat.dateTime;
import static com.proautokimium.api.Infrastructure.utils.BrazilianFormat.money;

/**
 * O comprovante de solicitações de reembolso que o RH leva à diretoria.
 *
 * Decisões de 2026-09-28, com o desenho aprovado:
 * <ul>
 *   <li>o período é pela <b>data da despesa</b> — "o que se gastou em setembro";</li>
 *   <li>os status são escolhidos na tela, e o documento diz quais ficaram DE FORA,
 *   para ninguém ler um total filtrado como o total;</li>
 *   <li>os comprovantes vão anexados só quando é um funcionário por vez;</li>
 *   <li>assinaturas: "Elaborado por" com quem emitiu, "Aprovado pela diretoria" em branco.</li>
 * </ul>
 */
@Service
public class ReimbursementReportService {

    private static final String TEMPLATE = "/templates/reports/reimbursements/comprovante_reembolsos.jrxml";

    /** Um ano: acima disso o PDF vira livro, e o pedido provavelmente foi engano. */
    static final long MAX_DAYS = 366;

    /** Ordem do ciclo de vida, para listar os status escolhidos. */
    private static final List<ReimbursementStatus> LIFECYCLE = List.of(
            ReimbursementStatus.PENDING, ReimbursementStatus.APPROVED,
            ReimbursementStatus.PAID, ReimbursementStatus.REJECTED);

    /** Ordem do resumo: o que já saiu do caixa primeiro. */
    private static final List<ReimbursementStatus> SUMMARY_ORDER = List.of(
            ReimbursementStatus.PAID, ReimbursementStatus.APPROVED,
            ReimbursementStatus.PENDING, ReimbursementStatus.REJECTED);

    private final ReimbursementRepository repository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final ReimbursementReceiptAnnexes annexes;
    private final Clock clock;

    /** Compilar é a parte cara: uma vez só, na primeira emissão. */
    private volatile JasperReport compiled;

    public ReimbursementReportService(ReimbursementRepository repository, EmployeeRepository employeeRepository,
                                      UserRepository userRepository, ReimbursementReceiptAnnexes annexes,
                                      Clock clock) {
        this.repository = repository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.annexes = annexes;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public byte[] generate(LocalDate from, LocalDate to, List<ReimbursementStatus> statuses,
                           UUID employeeId, String issuerLogin) {
        validate(from, to, statuses);

        Employee employee = employeeId == null ? null
                : employeeRepository.findById(employeeId).orElseThrow(EmployeeNotFoundException::new);
        List<ReimbursementStatus> chosen = LIFECYCLE.stream().filter(statuses::contains).toList();

        List<Reimbursement> items = employee == null
                ? repository.findForReport(from, to, chosen)
                : repository.findForReportByEmployee(employee, from, to, chosen);
        if (items.isEmpty()) {
            throw new EmptyReimbursementReportException();
        }

        boolean withAnnexes = employee != null;
        List<ReimbursementReportRowDTO> rows = new ArrayList<>();
        List<ReceiptAnnex> annexList = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            Reimbursement r = items.get(i);
            String code = withAnnexes ? "A-" + (i + 1) : null;
            rows.add(toRow(r, code));
            if (withAnnexes) {
                annexList.add(new ReceiptAnnex(code, r.getEmployee().getName(), employeeInfo(r.getEmployee()),
                        date(r.getExpenseDate()), r.getCategory(), statusLabel(r.getStatus()),
                        money(r.getAmount()), r.getReceiptOriginalFilename(), r.getReceiptStoragePath()));
            }
        }

        String period = date(from) + " a " + date(to);
        String footer = "KimiumHub · Comprovante de reembolsos · "
                + (employee != null ? employee.getName() + " · " : "") + period;
        Map<String, Object> params = params(items, chosen, employee, period, footer, issuerName(issuerLogin));

        try {
            byte[] pdf = fill(params, rows);
            return withAnnexes ? annexes.append(pdf, annexList, footer) : pdf;
        } catch (JRException | IOException e) {
            throw new ReimbursementReportException("Falha ao gerar o comprovante de reembolsos", e);
        }
    }

    /** "comprovante-reembolsos_2026-09-01_2026-09-30.pdf" */
    public static String fileName(LocalDate from, LocalDate to) {
        return "comprovante-reembolsos_" + from + "_" + to + ".pdf";
    }

    // ── validação ────────────────────────────────────────────────────────────

    private static void validate(LocalDate from, LocalDate to, List<ReimbursementStatus> statuses) {
        if (from == null || to == null) {
            throw new InvalidRequestDataException("Informe o início e o fim do período");
        }
        if (to.isBefore(from)) {
            throw new InvalidRequestDataException("O fim do período não pode ser antes do início");
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_DAYS) {
            throw new InvalidRequestDataException("O período pode ter no máximo um ano");
        }
        if (statuses == null || statuses.isEmpty()) {
            throw new InvalidRequestDataException("Escolha pelo menos um status");
        }
    }

    // ── linhas e parâmetros ──────────────────────────────────────────────────

    private ReimbursementReportRowDTO toRow(Reimbursement r, String annexCode) {
        Employee e = r.getEmployee();
        String reviewDetail = null;
        if (r.getReviewedAt() != null) {
            reviewDetail = date(r.getReviewedAt());
            if (r.getReviewNotes() != null && !r.getReviewNotes().isBlank()) {
                reviewDetail += " · \"" + r.getReviewNotes().strip() + "\"";
            }
        }
        return new ReimbursementReportRowDTO(
                e.getId() != null ? e.getId().toString() : e.getName(),
                e.getName(),
                employeeInfo(e),
                date(r.getExpenseDate()),
                r.getCategory(),
                r.getReason(),
                date(r.getRequestedAt()),
                r.getReviewedBy() != null ? r.getReviewedBy().getName() : "—",
                reviewDetail,
                r.getStatus().name(),
                statusLabel(r.getStatus()),
                r.getPaymentDate() != null ? date(r.getPaymentDate()) : "—",
                annexCode,
                r.getAmount(),
                money(r.getAmount()));
    }

    private Map<String, Object> params(List<Reimbursement> items, List<ReimbursementStatus> chosen,
                                       Employee employee, String period, String footer, String issuer) {
        Map<String, Object> p = new HashMap<>();
        p.put("PERIOD_LABEL", period);
        p.put("WITH_ANNEXES", employee != null);
        p.put("FOOTER_LABEL", footer);
        p.put("ISSUED_AT", dateTime(LocalDateTime.now(clock)));
        p.put("ISSUED_BY", issuer);

        long people = items.stream().map(r -> r.getEmployee().getId()).distinct().count();
        if (employee == null) {
            p.put("SCOPE_TITLE", "ABRANGÊNCIA");
            p.put("SCOPE_LABEL", "Todos os funcionários");
            p.put("SCOPE_DETAIL", people + (people == 1 ? " funcionário" : " funcionários") + " com solicitações");
        } else {
            p.put("SCOPE_TITLE", "FUNCIONÁRIO");
            p.put("SCOPE_LABEL", employee.getName());
            p.put("SCOPE_DETAIL", employeeInfo(employee));
        }

        p.put("STATUS_LABEL", chosen.stream().map(ReimbursementReportService::statusLabel)
                .collect(Collectors.joining(", ")));
        List<String> excluded = LIFECYCLE.stream().filter(s -> !chosen.contains(s))
                .map(ReimbursementReportService::statusLabel).toList();
        p.put("EXCLUDED_LABEL", excluded.isEmpty() ? "Todos os status"
                : (excluded.size() == 1 ? "Não incluído: " : "Não incluídos: ") + String.join(", ", excluded));

        // Resumo por status: todos os ESCOLHIDOS, inclusive com zero — um zero
        // escolhido é informação ("nada pendente"), e sumir com ele confundiria.
        List<String> names = new ArrayList<>(), counts = new ArrayList<>(), amounts = new ArrayList<>();
        for (ReimbursementStatus s : SUMMARY_ORDER) {
            if (!chosen.contains(s)) continue;
            List<Reimbursement> ofStatus = items.stream().filter(r -> r.getStatus() == s).toList();
            names.add(summaryLabel(s));
            counts.add(String.valueOf(ofStatus.size()));
            amounts.add(money(sum(ofStatus)));
        }
        p.put("SUMMARY_STATUS_NAMES", String.join("\n", names));
        p.put("SUMMARY_STATUS_COUNTS", String.join("\n", counts));
        p.put("SUMMARY_STATUS_AMOUNTS", String.join("\n", amounts));

        // Resumo por categoria, do maior valor para o menor.
        Map<String, BigDecimal> byCategory = items.stream().collect(Collectors.groupingBy(
                Reimbursement::getCategory, LinkedHashMap::new,
                Collectors.reducing(BigDecimal.ZERO, Reimbursement::getAmount, BigDecimal::add)));
        List<Map.Entry<String, BigDecimal>> sorted = byCategory.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue(Comparator.reverseOrder())).toList();
        p.put("SUMMARY_CATEGORY_NAMES", sorted.stream().map(Map.Entry::getKey).collect(Collectors.joining("\n")));
        p.put("SUMMARY_CATEGORY_AMOUNTS", sorted.stream().map(en -> money(en.getValue()))
                .collect(Collectors.joining("\n")));

        String count = items.size() + (items.size() == 1 ? " solicitação" : " solicitações");
        p.put("TOTAL_COUNT", String.valueOf(items.size()));
        p.put("TOTAL_AMOUNT", money(sum(items)));
        p.put("GRAND_TOTAL_LABEL", employee != null ? "Total · " + count
                : "Total geral do período · " + count + " · " + people
                  + (people == 1 ? " funcionário" : " funcionários"));
        return p;
    }

    private static BigDecimal sum(List<Reimbursement> items) {
        return items.stream().map(Reimbursement::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static String statusLabel(ReimbursementStatus s) {
        return switch (s) {
            case PENDING -> "Pendente";
            case APPROVED -> "Aprovado";
            case PAID -> "Pago";
            case REJECTED -> "Recusado";
        };
    }

    private static String summaryLabel(ReimbursementStatus s) {
        return switch (s) {
            case PAID -> "Pago";
            case APPROVED -> "Aprovado, a pagar";
            case PENDING -> "Pendente de análise";
            case REJECTED -> "Recusado";
        };
    }

    /** "Cód. 1042 · Administrativo" */
    static String employeeInfo(Employee e) {
        String info = "Cód. " + e.getCodParceiro();
        Department d = e.getDepartment();
        if (d != null && d != Department.SEM_DEPARTAMENTO) {
            String name = d.name().replace('_', ' ').toLowerCase();
            info += " · " + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
        return info;
    }

    private String issuerName(String login) {
        Employee viaLink = userRepository.findByLoginWithEmployee(login).map(u -> u.getEmployee()).orElse(null);
        Employee issuer = viaLink != null ? viaLink : employeeRepository.findByUsername(login).orElse(null);
        return issuer != null && issuer.getName() != null ? issuer.getName() : login;
    }

    // ── Jasper ───────────────────────────────────────────────────────────────

    private byte[] fill(Map<String, Object> params, List<ReimbursementReportRowDTO> rows) throws JRException {
        JasperPrint print = JasperFillManager.fillReport(report(), params, new JRBeanCollectionDataSource(rows));
        return JasperExportManager.exportReportToPdf(print);
    }

    private JasperReport report() throws JRException {
        JasperReport local = compiled;
        if (local == null) {
            synchronized (this) {
                local = compiled;
                if (local == null) {
                    try (InputStream in = getClass().getResourceAsStream(TEMPLATE)) {
                        if (in == null) {
                            throw new JRException("Template não encontrado: " + TEMPLATE);
                        }
                        local = JasperCompileManager.compileReport(in);
                        compiled = local;
                    } catch (IOException e) {
                        throw new JRException("Template ilegível: " + TEMPLATE, e);
                    }
                }
            }
        }
        return local;
    }
}
