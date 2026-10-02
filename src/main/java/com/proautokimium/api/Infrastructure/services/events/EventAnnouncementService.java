package com.proautokimium.api.Infrastructure.services.events;

import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.repositories.events.CompanyEventRepository;
import com.proautokimium.api.Infrastructure.services.notification.NotificationService;
import com.proautokimium.api.domain.entities.Partner;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.entities.events.CompanyEvent;
import com.proautokimium.api.domain.enums.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Os avisos que não dependem de resposta: "Novo evento", na publicação, e
 * "Começou agora", na hora da live.
 *
 * <p>O lembrete diário é outra coisa ({@link EventReminderService}): ele vai só
 * para quem ainda não respondeu. Estes vão para todos os convidados — a ideia
 * é que todo mundo fique sabendo.
 *
 * <p>Todo aviso leva para {@code /convites?evento=}: é lá que está o botão
 * Assistir. Mandar o link externo direto dependeria de como cada celular trata
 * o toque numa push, e o colaborador perderia o "Estou ciente" no caminho.
 */
@Slf4j
@Service
public class EventAnnouncementService {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final CompanyEventRepository events;
    private final EmployeeRepository employees;
    private final UserRepository users;
    private final NotificationService notifications;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public EventAnnouncementService(CompanyEventRepository events, EmployeeRepository employees, UserRepository users,
                                    NotificationService notifications, PlatformTransactionManager transactionManager,
                                    Clock clock) {
        this.events = events;
        this.employees = employees;
        this.users = users;
        this.notifications = notifications;
        this.transaction = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    /**
     * Avisa os convidados de que o evento foi publicado — uma vez na vida do
     * evento. Despublicar e publicar de novo não avisa outra vez: quem recebeu
     * o primeiro aviso receberia o mesmo evento duas vezes.
     *
     * <p>Roda dentro da transação de quem publica, e o evento já precisa estar
     * gravado: os convidados saem de uma consulta no banco. As notificações só
     * são entregues depois do commit ({@code NotificationService}).
     *
     * @return quantas pessoas foram avisadas
     */
    public int announcePublished(CompanyEvent event) {
        if (!event.isPublished() || !event.isAnnounceOnPublish() || event.getAnnouncedAt() != null) {
            return 0;
        }
        event.setAnnouncedAt(LocalDateTime.now(clock));

        String message = event.isOnline()
                ? EventReminderService.period(event) + ", das " + time(event.getStartTime()) + " às "
                    + time(event.getEndTime()) + ", online. Toque para ver."
                : EventReminderService.period(event) + ". Toque para ver e responder.";
        return notifyInvitees(event.getId(), "Novo evento: " + event.getName(), message);
    }

    /** @return quantas pessoas foram avisadas, somando as lives */
    public int runLiveStarts() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<UUID> candidates = transaction.execute(s ->
                events.findLiveStartCandidates(now.toLocalDate()).stream().map(CompanyEvent::getId).toList());

        int notified = 0;
        for (UUID eventId : candidates == null ? List.<UUID>of() : candidates) {
            try {
                Integer sent = transaction.execute(s -> startLive(eventId, now));
                notified += sent == null ? 0 : sent;
            } catch (RuntimeException e) {
                log.error("Falha no aviso de início da live {}", eventId, e);
            }
        }
        return notified;
    }

    private int startLive(UUID eventId, LocalDateTime now) {
        CompanyEvent event = events.findById(eventId).orElse(null);
        if (event == null || !event.isPublished() || !event.isOnline() || !event.isNotifyLiveStart()) {
            return 0;
        }
        // Antes do início, ainda não; depois do fim, "começou agora" seria mentira.
        if (now.isBefore(event.startsAt()) || !now.isBefore(event.endsAt())) {
            return 0;
        }
        // A trava vem antes do aviso: quem não ganhar a marcação não avisa.
        if (events.claimLiveStartNotice(eventId, now) == 0) {
            return 0;
        }
        return notifyInvitees(eventId, "Começou agora: " + event.getName(),
                "A transmissão já começou. Toque para assistir.");
    }

    private int notifyInvitees(UUID eventId, String title, String message) {
        List<UUID> invitees = employees.findEventInvitees(eventId).stream().map(Partner::getId).toList();
        List<String> logins = invitees.isEmpty() ? List.of()
                : users.findActiveByEmployeeIds(invitees).stream().map(User::getLogin).distinct().toList();
        String link = "/convites?evento=" + eventId;
        for (String login : logins) {
            notifications.notify(login, NotificationType.EVENTO, title, message, link);
        }
        return logins.size();
    }

    private static String time(java.time.LocalTime t) {
        return t == null ? "" : t.format(HORA);
    }
}
