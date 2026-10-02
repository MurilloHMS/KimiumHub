package com.proautokimium.api.Infrastructure.services.events;

import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.repositories.events.CompanyEventRepository;
import com.proautokimium.api.Infrastructure.repositories.events.EventReminderSentRepository;
import com.proautokimium.api.Infrastructure.repositories.events.EventResponseRepository;
import com.proautokimium.api.Infrastructure.services.notification.NotificationService;
import com.proautokimium.api.domain.entities.Partner;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.entities.events.CompanyEvent;
import com.proautokimium.api.domain.entities.events.EventReminderSent;
import com.proautokimium.api.domain.enums.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * O lembrete diário de quem ainda não respondeu ao convite.
 *
 * <p>Roda de hora em hora ({@code EventReminderScheduler}), e cada evento decide
 * se é a hora dele — o mesmo desenho do {@code MachineAlertService}, porque a
 * hora muda em runtime e o cron do {@code @Scheduled} é fixo na subida.
 *
 * <p><b>A linha de {@code event_reminders_sent} é gravada ANTES de notificar.</b>
 * A chave única (evento, dia) da V113 só protege se existir antes do envio: duas
 * rodadas no mesmo dia (um restart às 09:00:30, dois contêineres) fazem a
 * segunda bater na chave e desfazer tudo, e ninguém recebe duas vezes.
 *
 * <p><b>Uma transação por evento.</b> Um evento com problema não pode impedir o
 * lembrete dos outros. E as notificações só são entregues depois do commit
 * ({@code NotificationService.notify}), então uma transação desfeita não
 * deixa push enviado para trás.
 */
@Slf4j
@Service
public class EventReminderService {

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter DIA_SEM_MES = DateTimeFormatter.ofPattern("dd");

    private final CompanyEventRepository events;
    private final EmployeeRepository employees;
    private final UserRepository users;
    private final EventResponseRepository responses;
    private final EventReminderSentRepository remindersSent;
    private final NotificationService notifications;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public EventReminderService(CompanyEventRepository events, EmployeeRepository employees,
                                UserRepository users, EventResponseRepository responses,
                                EventReminderSentRepository remindersSent, NotificationService notifications,
                                PlatformTransactionManager transactionManager, Clock clock) {
        this.events = events;
        this.employees = employees;
        this.users = users;
        this.responses = responses;
        this.remindersSent = remindersSent;
        this.notifications = notifications;
        this.transaction = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    /** @return quantas pessoas foram lembradas, somando os eventos */
    public int runReminders() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<UUID> candidates = transaction.execute(s ->
                events.findReminderCandidates(now.toLocalDate()).stream().map(CompanyEvent::getId).toList());

        int reminded = 0;
        for (UUID eventId : candidates == null ? List.<UUID>of() : candidates) {
            try {
                Integer sent = transaction.execute(s -> remind(eventId, now));
                reminded += sent == null ? 0 : sent;
            } catch (DataIntegrityViolationException e) {
                log.info("Lembrete do evento {} já tinha saído hoje — a chave única segurou", eventId);
            } catch (RuntimeException e) {
                log.error("Falha no lembrete do evento {}", eventId, e);
            }
        }
        return reminded;
    }

    private int remind(UUID eventId, LocalDateTime now) {
        CompanyEvent event = events.findById(eventId).orElse(null);
        if (event == null || !event.isPublished() || !event.isReminderEnabled() || event.getReminderTime() == null) {
            return 0;
        }
        if (event.getReminderTime().getHour() != now.getHour()) {
            return 0;
        }
        // A janela que o organizador escolheu: N dias antes do primeiro dia.
        if (!event.remindsOn(now.toLocalDate())) {
            return 0;
        }
        if (!now.isBefore(event.startsAt())) {
            return 0;
        }
        LocalDate today = now.toLocalDate();
        if (remindersSent.existsByEventIdAndSentOn(eventId, today)) {
            return 0;
        }

        // Respondeu, vai ou não vai, sai da lista: "não vou" também é resposta.
        Set<UUID> answered = responses.findAnsweredEmployeeIds(eventId);
        List<UUID> pending = employees.findEventInvitees(eventId).stream()
                .map(Partner::getId)
                .filter(id -> !answered.contains(id))
                .toList();
        List<String> logins = pending.isEmpty() ? List.of()
                : users.findActiveByEmployeeIds(pending).stream().map(User::getLogin).distinct().toList();

        // Antes de notificar: é esta linha que barra a segunda rodada do dia.
        remindersSent.saveAndFlush(EventReminderSent.of(event, today, logins.size(), now));

        String message = event.isOnline()
                ? event.getName() + ", " + period(event) + ". Confirme que está ciente."
                : event.getName() + ", " + period(event) + ". Vai participar?";
        String title = event.isOnline() ? "Você ainda não confirmou" : "Você ainda não respondeu";
        String link = "/convites?evento=" + eventId;
        for (String login : logins) {
            notifications.notify(login, NotificationType.EVENTO, title, message, link);
        }
        return logins.size();
    }

    /** "em 06/10" ou "de 06 a 08/10" ou "de 30/09 a 02/10". */
    static String period(CompanyEvent event) {
        LocalDate start = event.getStartDate();
        LocalDate end = event.getEndDate();
        if (start.equals(end)) {
            return "em " + start.format(DIA);
        }
        boolean sameMonth = start.getMonth() == end.getMonth() && start.getYear() == end.getYear();
        return "de " + start.format(sameMonth ? DIA_SEM_MES : DIA) + " a " + end.format(DIA);
    }
}
