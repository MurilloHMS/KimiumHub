package com.proautokimium.api.Infrastructure.services.email;

import com.proautokimium.api.Infrastructure.services.email.smtp.SmtpService;
import com.proautokimium.api.Infrastructure.services.storage.EmployeeDocumentStorageService;
import com.proautokimium.api.domain.entities.email.EmailAttachment;
import com.proautokimium.api.domain.entities.email.EmailQueue;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Entrega uma linha da fila ao SMTP, com os anexos lidos do disco. É o mesmo
 * caminho para o agendador e para o envio na hora: o erro sobe do mesmo jeito,
 * e quem chamou grava o motivo.
 */
@Component
public class EmailDispatcher {

    private final SmtpService smtp;
    private final EmployeeDocumentStorageService storage;

    public EmailDispatcher(SmtpService smtp, EmployeeDocumentStorageService storage) {
        this.smtp = smtp;
        this.storage = storage;
    }

    public void send(EmailQueue email) {
        List<SmtpService.Attachment> files = new ArrayList<>();
        for (EmailAttachment a : email.getAttachments()) {
            try {
                files.add(new SmtpService.Attachment(a.getFilename(), Files.readAllBytes(storage.resolve(a.getStoragePath())),
                        a.getContentType()));
            } catch (IOException e) {
                // Arquivo que sumiu do disco: falha deste envio, com o motivo, e não um e-mail sem o anexo.
                throw new UncheckedIOException("Anexo não encontrado no disco: " + a.getFilename(), e);
            }
        }
        smtp.send(email, files);
    }
}
