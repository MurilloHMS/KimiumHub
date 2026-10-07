package com.proautokimium.api.Infrastructure.services.email;

import com.proautokimium.api.Application.DTOs.email.EmailQueueDTOs.*;
import com.proautokimium.api.Infrastructure.exceptions.EmailRecordNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.SmtpEmailRepository;
import com.proautokimium.api.Infrastructure.repositories.email.EmailQueueRepository;
import com.proautokimium.api.Infrastructure.repositories.email.EmailRouteRepository;
import com.proautokimium.api.domain.entities.EmailEntity;
import com.proautokimium.api.domain.entities.email.EmailRoute;
import com.proautokimium.api.domain.enums.email.EmailOrigin;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidRequestDataException;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidStatusTransitionException;
import com.proautokimium.api.domain.valueObjects.Email;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Os e-mails da empresa (os únicos remetentes do ERP) e de qual deles sai cada
 * origem. Mudar aqui vale para os próximos e-mails: os da fila guardaram o
 * remetente do momento em que entraram.
 */
@Service
public class EmailSenderAdminService {

    static final String DOMAIN = "@envios.proautokimium.com.br";
    private static final Pattern NAME = Pattern.compile("^[a-z0-9._-]{1,40}$");

    private final SmtpEmailRepository senders;
    private final EmailRouteRepository routes;
    private final EmailQueueRepository queue;
    private final Clock clock;

    public EmailSenderAdminService(SmtpEmailRepository senders, EmailRouteRepository routes, EmailQueueRepository queue,
                                   Clock clock) {
        this.senders = senders;
        this.routes = routes;
        this.queue = queue;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Sender> list() {
        List<EmailRoute> all = routes.findAll();
        return senders.findAllByOrderByNameAsc().stream().map(s -> toDto(s, all)).toList();
    }

    @Transactional
    public Sender create(CreateSender body) {
        String name = body == null || body.name() == null ? "" : body.name().trim().toLowerCase(Locale.ROOT);
        if (!NAME.matcher(name).matches())
            throw new InvalidRequestDataException("Use só letras minúsculas, números, ponto, hífen ou sublinhado antes do @.");
        String display = body.displayName() == null ? "" : body.displayName().trim();
        if (display.isEmpty()) throw new InvalidRequestDataException("Dê o nome que aparece para quem recebe.");
        if (display.length() > 120) throw new InvalidRequestDataException("O nome de exibição passa de 120 caracteres.");
        String address = name + DOMAIN;
        if (senders.existsByEmail_AddressIgnoreCase(address))
            throw new InvalidRequestDataException(address + " já está cadastrado.");

        EmailEntity e = new EmailEntity();
        e.setName(name);
        e.setEmail(new Email(address));
        e.setDisplayName(display);
        e.setActive(true);
        e.setDefault(false);
        return toDto(senders.save(e), routes.findAll());
    }

    @Transactional
    public Sender update(UUID id, UpdateSender body) {
        EmailEntity e = find(id);
        if (body.displayName() != null) {
            String display = body.displayName().trim();
            if (display.isEmpty()) throw new InvalidRequestDataException("Dê o nome que aparece para quem recebe.");
            e.setDisplayName(display);
        }
        if (body.active() != null && !body.active() && e.isActive()) {
            if (e.isDefault()) throw new InvalidStatusTransitionException("O remetente padrão não se desativa: escolha outro padrão antes.");
            if (routes.existsBySender_IdOrReplyTo_Id(id, id))
                throw new InvalidStatusTransitionException("Este remetente está em uso: troque os serviços dele antes de desativar.");
            e.setActive(false);
        } else if (Boolean.TRUE.equals(body.active())) {
            e.setActive(true);
        }
        return toDto(senders.save(e), routes.findAll());
    }

    @Transactional
    public Sender makeDefault(UUID id) {
        EmailEntity e = find(id);
        if (!e.isActive()) throw new InvalidStatusTransitionException("Ative o remetente antes de torná-lo padrão.");
        senders.findAll().stream().filter(EmailEntity::isDefault).filter(s -> !s.getId().equals(id)).forEach(s -> {
            s.setDefault(false);
            senders.save(s);
        });
        e.setDefault(true);
        return toDto(senders.save(e), routes.findAll());
    }

    @Transactional(readOnly = true)
    public List<Route> listRoutes() {
        Map<EmailOrigin, EmailRoute> byOrigin = new EnumMap<>(EmailOrigin.class);
        routes.findAll().forEach(r -> byOrigin.put(r.getOrigin(), r));
        Map<EmailOrigin, Long> last30 = new EnumMap<>(EmailOrigin.class);
        queue.countForRoutes(LocalDate.now(clock).minusDays(29).atStartOfDay())
                .forEach(c -> { if (c.getOrigin() != null) last30.merge(c.getOrigin(), c.getTotal(), Long::sum); });
        return Arrays.stream(EmailOrigin.values()).filter(EmailOrigin::isRoutable)
                .map(o -> toDto(o, byOrigin.get(o), last30.getOrDefault(o, 0L))).toList();
    }

    /** senderId nulo apaga a rota: a origem volta a usar o remetente padrão. */
    @Transactional
    public Route updateRoute(EmailOrigin origin, UpdateRoute body, String login) {
        if (!origin.isRoutable()) {
            throw new InvalidRequestDataException("O envio manual escolhe o remetente a cada envio: não tem rota.");
        }
        EmailEntity sender = body.senderId() == null ? null : active(body.senderId());
        EmailEntity replyTo = body.replyToId() == null ? null : active(body.replyToId());
        Optional<EmailRoute> current = routes.findById(origin);
        if (sender == null) {
            current.ifPresent(routes::delete);
            if (replyTo != null) {
                // Só "responder para", sem remetente próprio: a rota aponta para o padrão de hoje.
                EmailEntity def = senders.findFirstByIsDefaultTrue()
                        .orElseThrow(() -> new InvalidRequestDataException("Defina um remetente padrão antes."));
                routes.save(new EmailRoute(origin, def, replyTo, login, LocalDateTime.now(clock)));
            }
        } else if (current.isPresent()) {
            current.get().change(sender, replyTo, login, LocalDateTime.now(clock));
            routes.save(current.get());
        } else {
            routes.save(new EmailRoute(origin, sender, replyTo, login, LocalDateTime.now(clock)));
        }
        return listRoutes().stream().filter(r -> r.origin() == origin).findFirst().orElseThrow();
    }

    private EmailEntity active(UUID id) {
        EmailEntity e = find(id);
        if (!e.isActive()) throw new InvalidRequestDataException(e.getEmail().getAddress() + " está inativo.");
        return e;
    }

    private EmailEntity find(UUID id) {
        return senders.findById(id).orElseThrow(() -> new EmailRecordNotFoundException("Remetente não encontrado."));
    }

    private static Sender toDto(EmailEntity e, List<EmailRoute> all) {
        Set<EmailOrigin> used = EnumSet.noneOf(EmailOrigin.class);
        for (EmailRoute r : all) {
            if (r.getSender().getId().equals(e.getId()) || (r.getReplyTo() != null && r.getReplyTo().getId().equals(e.getId())))
                used.add(r.getOrigin());
        }
        return new Sender(e.getId(), e.getName(), e.getEmail() == null ? null : e.getEmail().getAddress(),
                e.getDisplayName(), e.isActive(), e.isDefault(), used);
    }

    private static Route toDto(EmailOrigin o, EmailRoute r, long last30) {
        return new Route(o, o.getLabel(), o.getHint(), r == null ? null : r.getSender().getId(),
                r == null || r.getReplyTo() == null ? null : r.getReplyTo().getId(), last30);
    }
}
