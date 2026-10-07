package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Reimbursement.ReportEmailResultDTO;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.NoReportRecipientException;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.ReportEmailFailedException;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.ReportTooLargeForEmailException;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.services.email.EmailQueueService;
import com.proautokimium.api.Infrastructure.services.email.EmailRenderer;
import com.proautokimium.api.Infrastructure.services.email.smtp.SmtpService;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.enums.humanResources.ReimbursementStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.proautokimium.api.Infrastructure.utils.BrazilianFormat.date;
import static com.proautokimium.api.Infrastructure.utils.BrazilianFormat.dateTime;

/**
 * Manda o comprovante de reembolsos para a caixa do RH.
 *
 * Decidido em 2026-09-28: botão manual (sem agendamento), o mesmo PDF do
 * Jasper que se baixa, e os destinatários cadastrados na tela.
 *
 * {@code sendNow}, e não a fila: é uma pessoa esperando o resultado de um
 * clique, e ela precisa saber na hora se saiu. A fila avisaria em até 60s,
 * depois de a tela já ter dito "enviado".
 */
@Service
public class ReimbursementReportEmailService {

    private static final Logger log = LoggerFactory.getLogger(ReimbursementReportEmailService.class);

    private static final String TEMPLATE = "html/hr-report";

    /**
     * 15 MB. O limite comum de SMTP é 20–25 MB na mensagem, e o anexo cresce
     * cerca de um terço ao virar base64 — 15 MB de PDF já chega perto de 20.
     */
    static final long MAX_ATTACHMENT_BYTES = 15L * 1024 * 1024;

    private static final List<ReimbursementStatus> LIFECYCLE = List.of(
            ReimbursementStatus.PENDING, ReimbursementStatus.APPROVED,
            ReimbursementStatus.PAID, ReimbursementStatus.REJECTED);

    private final HrReportRecipientService recipients;
    private final ReimbursementReportService reportService;
    private final EmailQueueService emailQueueService;
    private final EmployeeRepository employeeRepository;
    private final EmailRenderer renderer;
    private final Clock clock;
    private final String from;

    public ReimbursementReportEmailService(HrReportRecipientService recipients,
                                           ReimbursementReportService reportService,
                                           EmailQueueService emailQueueService,
                                           EmployeeRepository employeeRepository, EmailRenderer renderer,
                                           Clock clock,
                                           @Value("${mail.from}") String from) {
        this.recipients = recipients;
        this.reportService = reportService;
        this.emailQueueService = emailQueueService;
        this.employeeRepository = employeeRepository;
        this.renderer = renderer;
        this.clock = clock;
        this.from = from;
    }

    public ReportEmailResultDTO sendToHr(LocalDate periodStart, LocalDate periodEnd,
                                         List<ReimbursementStatus> statuses, UUID employeeId, String login) {
        // Antes de gerar: montar um PDF com anexos para descobrir que não há
        // para quem mandar seria trabalho jogado fora.
        List<String> to = recipients.emails();
        if (to.isEmpty()) {
            throw new NoReportRecipientException();
        }

        // As validações do período e dos status moram aqui dentro.
        byte[] pdf = reportService.generate(periodStart, periodEnd, statuses, employeeId, login);
        if (pdf.length > MAX_ATTACHMENT_BYTES) {
            throw new ReportTooLargeForEmailException(Math.ceilDiv(pdf.length, 1024 * 1024),
                    MAX_ATTACHMENT_BYTES / (1024 * 1024));
        }

        String period = date(periodStart) + " a " + date(periodEnd);
        String employeeName = employeeId == null ? null
                : employeeRepository.findById(employeeId).map(Employee::getName).orElse(null);
        String fileName = ReimbursementReportService.fileName(periodStart, periodEnd);
        String subject = "Comprovante de reembolsos · " + period
                + (employeeName != null ? " · " + employeeName : "");
        String html = body(period, employeeName, statuses, login, fileName);
        SmtpService.Attachment attachment = new SmtpService.Attachment(fileName, pdf, "application/pdf");

        // Um e-mail por destinatário: um endereço quebrado não derruba os outros,
        // e ninguém vê para quem mais foi.
        List<String> sent = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        for (String recipient : to) {
            try {
                emailQueueService.sendNow(recipient, from, subject, html, attachment);
                sent.add(recipient);
            } catch (RuntimeException e) {
                log.warn("Comprovante de reembolsos não saiu para {}", recipient, e);
                failed.add(recipient);
            }
        }
        if (sent.isEmpty()) {
            throw new ReportEmailFailedException("Nenhum destinatário recebeu o comprovante de reembolsos");
        }
        return new ReportEmailResultDTO(fileName, sent, failed);
    }

    private String body(String period, String employeeName, List<ReimbursementStatus> statuses,
                        String login, String fileName) {
        return renderer.render(TEMPLATE, Map.of(
                "periodo", period,
                "escopo", employeeName != null ? "de " + employeeName : "de todos os funcionários",
                "status", LIFECYCLE.stream().filter(statuses::contains).map(ReimbursementReportService::statusLabel).collect(Collectors.joining(", ")),
                "emitidoPor", reportService.issuerName(login),
                "emitidoEm", dateTime(LocalDateTime.now(clock)),
                "arquivo", fileName
        ));
    }
}
