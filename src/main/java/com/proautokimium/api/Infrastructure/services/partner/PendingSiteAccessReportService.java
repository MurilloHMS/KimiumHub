package com.proautokimium.api.Infrastructure.services.partner;

import com.proautokimium.api.Application.DTOs.partners.EmployeeSiteAccess;
import com.proautokimium.api.Application.DTOs.partners.PendingSiteAccessRowDTO;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.SiteAccessReportException;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.CareerHistoryRepository;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.CareerHistory;
import com.proautokimium.api.domain.enums.SiteAccess;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * O relatório dos funcionários que ainda não entraram no site, em Excel ou PDF.
 *
 * **Só ativos e pendentes.** Desligado não precisa de cadastro, e quem tem a
 * conta bloqueada já entrou um dia. A situação vem do {@link SiteAccessResolver},
 * o mesmo cálculo da tela de Funcionários: se a regra mudar, a tela e o
 * relatório mudam juntos.
 */
@Service
public class PendingSiteAccessReportService {

    /**
     * O template JÁ COMPILADO: o container roda só o JRE, sem {@code javac}, e
     * compilar o {@code .jrxml} em produção dá 503 (aprendido no comprovante de
     * reembolsos). Editou o {@code .jrxml}? Recompile — o comando está no
     * PendingSiteAccessReportTemplateTest.
     */
    static final String TEMPLATE = "/templates/reports/site_access/pending_site_access.jasper";

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final EmployeeRepository employees;
    private final CareerHistoryRepository careerHistories;
    private final SiteAccessResolver siteAccessResolver;
    private final PendingSiteAccessExcelWriter excelWriter;
    private final Clock clock;

    private volatile JasperReport compiled;

    public PendingSiteAccessReportService(EmployeeRepository employees,
                                          CareerHistoryRepository careerHistories,
                                          SiteAccessResolver siteAccessResolver,
                                          PendingSiteAccessExcelWriter excelWriter,
                                          Clock clock) {
        this.employees = employees;
        this.careerHistories = careerHistories;
        this.siteAccessResolver = siteAccessResolver;
        this.excelWriter = excelWriter;
        this.clock = clock;
    }

    /** As linhas do relatório: ativos e pendentes, em ordem de nome. */
    public List<PendingSiteAccessRowDTO> rows() {
        List<Employee> ativos = employees.findAll().stream().filter(Employee::isAtivo).toList();
        Map<UUID, EmployeeSiteAccess> accessByEmployee = siteAccessResolver.resolve(ativos);
        Map<UUID, CareerHistory> latestByEmployee = careerHistories.findLatestPerEmployee().stream()
                .collect(Collectors.toMap(ch -> ch.getEmployee().getId(), ch -> ch, (a, b) -> a));

        return ativos.stream()
                .filter(e -> {
                    EmployeeSiteAccess access = accessByEmployee.get(e.getId());
                    return access != null && access.status() == SiteAccess.PENDING;
                })
                .sorted(Comparator.comparing(e -> Optional.ofNullable(e.getName()).orElse(""), String.CASE_INSENSITIVE_ORDER))
                .map(e -> toRow(e, latestByEmployee.get(e.getId()), accessByEmployee.get(e.getId())))
                .toList();
    }

    public byte[] excel() {
        try {
            return excelWriter.write(rows());
        } catch (Exception e) {
            throw new SiteAccessReportException("Falha ao montar a planilha dos pendentes", e);
        }
    }

    public byte[] pdf() {
        List<PendingSiteAccessRowDTO> rows = rows();
        Map<String, Object> params = new HashMap<>();
        params.put("ISSUED_AT", LocalDateTime.now(clock).format(DATE_TIME));
        params.put("TOTAL_LABEL", rows.size() == 1 ? "1 pessoa" : rows.size() + " pessoas");
        try {
            // Sem linha nenhuma o Jasper não imprime nem o cabeçalho: a página
            // sairia em branco. O texto de "ninguém pendente" vem no resumo.
            return JasperExportManager.exportReportToPdf(
                    JasperFillManager.fillReport(report(), params, new JRBeanCollectionDataSource(rows)));
        } catch (JRException e) {
            throw new SiteAccessReportException("Falha ao montar o PDF dos pendentes", e);
        }
    }

    /** O nome do arquivo, com a data: o RH baixa um por semana e precisa saber qual é qual. */
    public String fileName(String extension) {
        return "funcionarios-sem-acesso-" + LocalDateTime.now(clock).format(DateTimeFormatter.ISO_LOCAL_DATE) + "." + extension;
    }

    private PendingSiteAccessRowDTO toRow(Employee employee, CareerHistory latest, EmployeeSiteAccess access) {
        String department = employee.getTeam() == null ? null
                : employee.getTeam().getDepartment() == null ? employee.getTeam().getName()
                : employee.getTeam().getDepartment().getName() + " / " + employee.getTeam().getName();
        return new PendingSiteAccessRowDTO(
                employee.getCodParceiro(),
                employee.getName(),
                employee.getEmail() != null ? employee.getEmail().getAddress() : null,
                employee.getCompany() != null ? employee.getCompany().getName() : null,
                department,
                latest != null && latest.getPosition() != null ? latest.getPosition().getName() : null,
                detail(access));
    }

    static String detail(EmployeeSiteAccess access) {
        if (access == null || access.firstAccessRequestedAt() == null) return "nunca entrou";
        return "pediu o código em " + access.firstAccessRequestedAt().format(DATE) + ", não concluiu";
    }

    private JasperReport report() throws JRException {
        JasperReport local = compiled;
        if (local == null) {
            synchronized (this) {
                local = compiled;
                if (local == null) {
                    try (InputStream in = getClass().getResourceAsStream(TEMPLATE)) {
                        if (in == null) throw new JRException("Template não encontrado: " + TEMPLATE);
                        local = (JasperReport) JRLoader.loadObject(in);
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
