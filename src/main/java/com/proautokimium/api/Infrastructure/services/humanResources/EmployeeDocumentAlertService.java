package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.EmployeeDocumentAlertSentRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.EmployeeDocumentRepository;
import com.proautokimium.api.Infrastructure.services.email.EmailQueueService;
import com.proautokimium.api.Infrastructure.services.email.EmailRenderer;
import com.proautokimium.api.Infrastructure.services.notification.NotificationService;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.EmployeeDocument;
import com.proautokimium.api.domain.entities.humanResources.EmployeeDocumentAlertSent;
import com.proautokimium.api.domain.enums.NotificationType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Os avisos de vencimento dos documentos — "igual à Programação", com duas
 * diferenças decididas por ele (2026-09-29): os dias e os responsáveis são do
 * TIPO, e o aviso chega pelo sino E por e-mail.
 *
 * **Cada marco sai uma vez só.** A Programação repete o atraso todo dia; aqui
 * não: um documento fica vencido por semanas, e um e-mail diário ensinaria a
 * ignorar o aviso. O vencido continua na tela, no chip vermelho.
 */
@Service
public class EmployeeDocumentAlertService {

    private static final String TEMPLATE = "html/employee-document-alert";
    private static final String FROM = "noreply@envios.proautokimium.com.br";
    private static final DateTimeFormatter DATE_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EmployeeDocumentRepository documentRepository;
    private final EmployeeDocumentAlertSentRepository sentRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailQueueService emailQueueService;
    private final EmailRenderer renderer;
    private final Clock clock;

    @Value("${app.base-url}")
    String websiteBaseUrl;

    public EmployeeDocumentAlertService(EmployeeDocumentRepository documentRepository,
                                        EmployeeDocumentAlertSentRepository sentRepository,
                                        EmployeeRepository employeeRepository,
                                        UserRepository userRepository,
                                        NotificationService notificationService,
                                        EmailQueueService emailQueueService, EmailRenderer renderer,
                                        Clock clock) {
        this.documentRepository = documentRepository;
        this.sentRepository = sentRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.emailQueueService = emailQueueService;
        this.renderer = renderer;
        this.clock = clock;
    }

    /**
     * Confere os documentos de hoje e avisa quem é responsável. Devolve quantos
     * documentos geraram aviso.
     *
     * Rodar duas vezes no mesmo dia não avisa duas vezes: a trava é a tabela de
     * enviados, e é ela que deixa o botão "rodar agora" ser seguro.
     */
    @Transactional
    public int runAlerts() {
        LocalDate today = LocalDate.now(clock);
        int alerted = 0;

        for (EmployeeDocument document : documentRepository.findAlertCandidates()) {
            Integer marker = markerFor(document, today);
            if (marker == null) continue;
            if (sentRepository.alreadySent(document.getId(), document.getDueDate(), marker)) {
                continue;
            }

            // Sem responsável não grava o marco: se o RH puser alguém ainda hoje,
            // o próximo "rodar agora" avisa.
            List<Employee> recipients = employeeRepository.findAllById(document.getType().getRecipientEmployeeIds());
            if (recipients.isEmpty()) continue;

            long daysLeft = document.daysUntilDue(today);
            String body = buildEmail(document, daysLeft);
            for (Employee recipient : recipients) {
                notifyBell(recipient, document, daysLeft);
                sendEmail(recipient, document, daysLeft, body);
            }

            sentRepository.save(new EmployeeDocumentAlertSent(
                    document.getId(), document.getDueDate(), marker, LocalDateTime.now(clock)));
            alerted++;
        }
        return alerted;
    }

    /**
     * O marco de hoje, ou nulo se hoje não é dia de aviso para este documento.
     *
     * Só o dia EXATO conta: "30, 7" avisa a 30 e a 7 dias, não em todos entre
     * eles. Documento vinculado a 10 dias do vencimento com aviso de 30 perde
     * esse marco — e recebe o de 7.
     */
    static Integer markerFor(EmployeeDocument document, LocalDate today) {
        long daysLeft = document.daysUntilDue(today);
        if (daysLeft == 0) {
            return document.getType().isNotifyOnExpiry() ? EmployeeDocumentAlertSent.ON_DUE_DATE : null;
        }
        if (daysLeft > 0 && document.getType().getAlertDaysBefore().contains((int) daysLeft)) {
            return (int) daysLeft;
        }
        return null;
    }

    /** O sino abre a tela do RH já no recorte do aviso: "vence em breve" daquele funcionário. */
    private void notifyBell(Employee recipient, EmployeeDocument document, long daysLeft) {
        String link = "/rh/employee-documents?status=EXPIRING&employeeId=" + document.getEmployee().getId();
        userRepository.findByEmployee_Id(recipient.getId()).ifPresent(user ->
                notificationService.notify(user.getLogin(), NotificationType.DOCUMENTO,
                        headline(daysLeft) + ": " + document.getTitle(),
                        document.getTitle() + " de " + document.getEmployee().getName()
                                + " vence em " + document.getDueDate().format(DATE_BR) + ".",
                        link));
    }

    private void sendEmail(Employee recipient, EmployeeDocument document, long daysLeft, String body) {
        if (recipient.getEmail() == null || recipient.getEmail().getAddress() == null) return;
        String subject = headline(daysLeft) + ": " + document.getTitle() + " — " + document.getEmployee().getName();
        emailQueueService.sendEmail(recipient.getEmail().getAddress(), FROM, subject, body);
    }

    private static String headline(long daysLeft) {
        if (daysLeft == 0) return "Vence hoje";
        if (daysLeft == 1) return "Vence amanhã";
        return "Vence em " + daysLeft + " dias";
    }

    private String buildEmail(EmployeeDocument document, long daysLeft) {
        return renderer.render(TEMPLATE, Map.of(
                "hoje", daysLeft == 0,
                "chamada", headline(daysLeft),
                "funcionario", document.getEmployee().getName(),
                "documento", document.getTitle(),
                "tipo", document.getType().getName(),
                "vencimento", document.getDueDate().format(DATE_BR),
                "link", websiteBaseUrl + "/rh/employee-documents?status=EXPIRING&employeeId=" + document.getEmployee().getId()
        ));
    }
}
