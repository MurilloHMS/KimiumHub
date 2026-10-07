package com.proautokimium.api.Infrastructure.services.email;

import com.proautokimium.api.Infrastructure.repositories.email.EmailQueueRepository;
import com.proautokimium.api.Infrastructure.services.storage.EmployeeDocumentStorageService;
import com.proautokimium.api.domain.entities.email.EmailQueue;
import com.proautokimium.api.domain.enums.email.EmailOrigin;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A única porta de saída de e-mail do ERP (2026-10-07). Todo e-mail vira uma
 * linha da fila, com a origem, o remetente da configuração e os anexos no disco.
 *
 * Dois jeitos de sair:
 * - {@link #enqueue}: o agendador envia no próximo minuto e tenta até 5 vezes;
 * - {@link #sendNow}: sai na hora e a linha registra o resultado. Para o que não
 *   pode esperar (código de acesso), com uma tentativa só: o código expira antes
 *   da próxima. A falha sobe, para quem chamou dizer à pessoa que não saiu.
 */
@Service
public class EmailQueueService {

    /** A pasta dos anexos, dentro do armazenamento de documentos. */
    public static final String ATTACHMENT_FOLDER = "email-anexos";

    private final EmailQueueRepository repository;
    private final EmailSenderResolver senders;
    private final EmailDispatcher dispatcher;
    private final EmployeeDocumentStorageService storage;
    private final Clock clock;

    public EmailQueueService(EmailQueueRepository repository, EmailSenderResolver senders, EmailDispatcher dispatcher,
                             EmployeeDocumentStorageService storage, Clock clock) {
        this.repository = repository;
        this.senders = senders;
        this.dispatcher = dispatcher;
        this.storage = storage;
        this.clock = clock;
    }

    /** Um arquivo que vai junto: guardado no disco antes de o e-mail entrar na fila. */
    public record OutgoingAttachment(String filename, byte[] content, String contentType) {}

    @Transactional
    public EmailQueue enqueue(EmailOrigin origin, String to, String subject, String html) {
        return enqueue(origin, to, subject, html, List.of());
    }

    @Transactional
    public EmailQueue enqueue(EmailOrigin origin, String to, String subject, String html,
                              List<OutgoingAttachment> attachments) {
        EmailQueue email = prepare(origin, to, subject, html, attachments);
        email.markSchedule();
        return repository.save(email);
    }

    /**
     * Envio manual: quem escolhe o remetente é a pessoa, na tela de Comunicação,
     * entre os e-mails da empresa. O endereço precisa ser um remetente ativo.
     */
    @Transactional
    public EmailQueue enqueueAs(String fromAddress, String fromName, String replyTo, String to, String subject,
                                String html, List<OutgoingAttachment> attachments) {
        EmailQueue email = prepare(EmailOrigin.MANUAL, to, subject, html, attachments);
        email.assignSender(fromAddress, fromName, replyTo == null || replyTo.isBlank() ? null : replyTo);
        email.markSchedule();
        return repository.save(email);
    }

    /** Para quem já montou o e-mail com a origem (EmailFactory): falta o remetente. */
    @Transactional
    public EmailQueue create(EmailQueue email) {
        EmailSenderResolver.Sender sender = senders.resolve(email.getOrigin());
        email.assignSender(sender.address(), sender.name(), sender.replyTo());
        email.markSchedule();
        return repository.save(email);
    }

    public void sendNow(EmailOrigin origin, String to, String subject, String html) {
        sendNow(origin, to, subject, html, List.of());
    }

    /** SENT no sucesso; FAILED com o motivo na falha, e a falha sobe. A linha é gravada sempre. */
    public void sendNow(EmailOrigin origin, String to, String subject, String html,
                        List<OutgoingAttachment> attachments) {
        EmailQueue email = prepare(origin, to, subject, html, attachments);
        try {
            dispatcher.send(email);
            email.markSent(LocalDateTime.now(clock));
        } catch (RuntimeException e) {
            email.recordImmediateFailure(describe(e), LocalDateTime.now(clock));
            throw e;
        } finally {
            repository.save(email);
        }
    }

    private EmailQueue prepare(EmailOrigin origin, String to, String subject, String html,
                               List<OutgoingAttachment> attachments) {
        LocalDateTime now = LocalDateTime.now(clock);
        EmailQueue email = EmailQueue.of(origin, to, subject, html, now);
        EmailSenderResolver.Sender sender = senders.resolve(origin);
        email.assignSender(sender.address(), sender.name(), sender.replyTo());
        for (OutgoingAttachment a : attachments == null ? List.<OutgoingAttachment>of() : attachments) {
            try {
                String path = storage.save(a.content(), ATTACHMENT_FOLDER, a.filename());
                email.addAttachment(a.filename(), a.contentType() == null ? "application/octet-stream" : a.contentType(),
                        path, a.content().length, now);
            } catch (IOException e) {
                throw new UncheckedIOException("Não foi possível guardar o anexo " + a.filename(), e);
            }
        }
        return email;
    }

    /**
     * O motivo para guardar: a mensagem de cada exceção da cadeia. O SMTP
     * costuma vir embrulhado ("MailSendException ← SMTPAddressFailedException:
     * 550 5.1.1 …"), e o código útil está lá dentro.
     */
    public static String describe(Throwable e) {
        List<String> parts = new ArrayList<>();
        for (Throwable t = e; t != null && parts.size() < 5; t = t.getCause() == t ? null : t.getCause()) {
            String m = t.getMessage();
            parts.add(t.getClass().getSimpleName() + (m == null ? "" : ": " + m));
        }
        return String.join(" ← ", parts);
    }
}
