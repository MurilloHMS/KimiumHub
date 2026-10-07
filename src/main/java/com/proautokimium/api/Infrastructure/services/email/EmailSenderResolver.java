package com.proautokimium.api.Infrastructure.services.email;

import com.proautokimium.api.Infrastructure.repositories.SmtpEmailRepository;
import com.proautokimium.api.Infrastructure.repositories.email.EmailRouteRepository;
import com.proautokimium.api.domain.entities.EmailEntity;
import com.proautokimium.api.domain.entities.email.EmailRoute;
import com.proautokimium.api.domain.enums.email.EmailOrigin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * De qual e-mail da empresa sai cada origem. Ninguém mais escreve remetente no
 * código: quem envia diz a origem, e a configuração (tela Remetentes) decide.
 *
 * Ordem: a rota da origem (se o remetente estiver ativo) → o remetente padrão
 * → o EMAIL_FROM do ambiente, que era o remetente de antes da configuração.
 */
@Component
public class EmailSenderResolver {

    /** O nome que aparecia em todos os e-mails antes da configuração. */
    static final String DEFAULT_NAME = "Proauto Kimium";

    private final EmailRouteRepository routes;
    private final SmtpEmailRepository senders;
    private final String fallbackFrom;

    public EmailSenderResolver(EmailRouteRepository routes, SmtpEmailRepository senders,
                               @Value("${mail.from}") String fallbackFrom) {
        this.routes = routes;
        this.senders = senders;
        this.fallbackFrom = fallbackFrom;
    }

    public record Sender(String address, String name, String replyTo) {}

    public Sender resolve(EmailOrigin origin) {
        EmailRoute route = origin == null ? null : routes.findById(origin).orElse(null);
        String replyTo = route != null && route.getReplyTo() != null && route.getReplyTo().isActive()
                ? route.getReplyTo().getEmail().getAddress() : null;
        if (route != null && route.getSender().isActive()) {
            return of(route.getSender(), replyTo);
        }
        return senders.findFirstByIsDefaultTrue()
                .filter(EmailEntity::isActive)
                .map(s -> of(s, replyTo))
                .orElse(new Sender(fallbackFrom, DEFAULT_NAME, replyTo));
    }

    private static Sender of(EmailEntity e, String replyTo) {
        String name = e.getDisplayName() == null || e.getDisplayName().isBlank() ? DEFAULT_NAME : e.getDisplayName();
        return new Sender(e.getEmail().getAddress(), name, replyTo);
    }
}
