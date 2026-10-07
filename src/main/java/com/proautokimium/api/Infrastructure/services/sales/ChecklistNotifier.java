package com.proautokimium.api.Infrastructure.services.sales;

import com.proautokimium.api.domain.enums.email.EmailOrigin;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.services.email.EmailQueueService;
import com.proautokimium.api.Infrastructure.services.email.EmailRenderer;
import com.proautokimium.api.Infrastructure.services.notification.NotificationService;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.entities.sales.Checklist;
import com.proautokimium.api.domain.enums.NotificationType;
import com.proautokimium.api.domain.enums.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Quem fica sabendo do quê.
 *
 * <ul>
 *   <li>Chegou checklist, ou pedido de alteração: sino e e-mail para quem tem a
 *       role CONTRATOS (a Controladoria).</li>
 *   <li>A Controladoria respondeu: sino para o vendedor.</li>
 * </ul>
 *
 * Aviso é melhor esforço: uma falha aqui não desfaz o checklist, que é o que
 * importa. O sino já espera o commit ({@link NotificationService#notify}); o
 * e-mail vai para a fila, que tem as suas tentativas.
 */
@Component
public class ChecklistNotifier {

    private static final Logger log = LoggerFactory.getLogger(ChecklistNotifier.class);
    static final String REVIEW_LINK = "/vendas/checklists";
    static final String SELLER_LINK = "/vendas/checklist";

    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailQueueService emailQueueService;
    private final String baseUrl;
    private final EmailRenderer renderer;

    public ChecklistNotifier(UserRepository userRepository, NotificationService notificationService,
                             EmailQueueService emailQueueService, @Value("${app.base-url}") String baseUrl, EmailRenderer renderer) {
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.emailQueueService = emailQueueService;
        this.baseUrl = baseUrl;
        this.renderer = renderer;
    }

    public void submitted(Checklist c) {
        String title = c.getVersion() == 1 ? "Novo checklist para conferir" : "Checklist reenviado";
        String message = c.getSellerName() + " enviou o checklist nº " + number(c) + " — " + c.getCustomerName()
                + (c.getVersion() > 1 ? " (versão " + c.getVersion() + ")" : "") + ".";
        toControladoria(c, title, message);
    }

    public void changeRequested(Checklist c) {
        toControladoria(c, "Pedido de alteração em checklist",
                c.getSellerName() + " pediu para alterar o checklist nº " + number(c) + " — "
                        + c.getCustomerName() + ": \"" + c.getChangeReason() + "\"");
    }

    public void answered(Checklist c, String title, String message) {
        safely(() -> notificationService.notify(c.getSellerLogin(), NotificationType.CHECKLIST, title, message,
                SELLER_LINK));
    }

    private void toControladoria(Checklist c, String title, String message) {
        List<User> reviewers = userRepository.findByRolesIn(List.of(UserRole.CONTRATOS));
        for (User reviewer : reviewers) {
            if (!reviewer.isActive() || c.isOwnedBy(reviewer.getLogin())) {
                continue;
            }
            safely(() -> notificationService.notify(reviewer.getLogin(), NotificationType.CHECKLIST, title,
                    message, REVIEW_LINK));
            if (reviewer.getEmail() != null && !reviewer.getEmail().isBlank()) {
                safely(() -> emailQueueService.enqueue(EmailOrigin.CHECKLIST, reviewer.getEmail(),
                        title + " — nº " + number(c), body(title, message)));
            }
        }
    }

    private String body(String title, String message) {
        return renderer.render("html/checklist-controladoria", Map.of(
                "titulo", title,
                "mensagem", message,
                "link", baseUrl + REVIEW_LINK
        ));
    }

    static String number(Checklist c) {
        return c.getNumber() == null ? "—" : String.format("%04d", c.getNumber());
    }

    private static void safely(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            log.warn("Aviso do checklist não enviado: {}", e.getMessage());
        }
    }
}
