package com.proautokimium.api.Infrastructure.services.email.smtp;

import com.proautokimium.api.domain.entities.email.EmailQueue;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.ByteArrayOutputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * O anexo chega à mensagem MIME que vai para o servidor — lido de volta da
 * mensagem, não de um mock que devolve o que o teste manda.
 */
class SmtpServiceAttachmentTest {

    private final JavaMailSender sender = mock(JavaMailSender.class);
    private final SmtpService smtp = new SmtpService(sender);

    private EmailQueue email() {
        return new EmailQueue("rh@proautokimium.com.br", "no-reply@envios.proautokimium.com.br",
                "Comprovante", "<p>Segue</p>");
    }

    @Test
    @DisplayName("o PDF vai anexado, com o nome e o conteúdo")
    void anexoChegaNaMensagem() throws Exception {
        when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        byte[] pdf = {'%', 'P', 'D', 'F', '-', '1'};

        smtp.sendWithAttachment(email(), new SmtpService.Attachment("comprovante.pdf", pdf, "application/pdf"));

        ArgumentCaptor<MimeMessage> enviada = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(enviada.capture());
        BodyPart anexo = anexoDe((Multipart) enviada.getValue().getContent());
        assertThat(anexo).as("a mensagem tem uma parte de anexo").isNotNull();
        assertThat(anexo.getFileName()).isEqualTo("comprovante.pdf");
        ByteArrayOutputStream lido = new ByteArrayOutputStream();
        anexo.getDataHandler().writeTo(lido);
        assertThat(lido.toByteArray()).isEqualTo(pdf);
    }

    /**
     * Diferente do outro envio com anexo do SmtpService, que só loga: aqui a
     * falha precisa subir, senão a tela diria "enviado" sem ter enviado.
     */
    @Test
    @DisplayName("falha do servidor de e-mail sobe, não fica só no log")
    void falhaSobe() {
        when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new MailSendException("smtp fora")).when(sender).send(any(MimeMessage.class));

        assertThrows(RuntimeException.class, () -> smtp.sendWithAttachment(email(),
                new SmtpService.Attachment("c.pdf", new byte[]{1}, "application/pdf")));
    }

    private static BodyPart anexoDe(Multipart multipart) throws Exception {
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) return part;
            if (part.getContent() instanceof Multipart inner) {
                BodyPart found = anexoDe(inner);
                if (found != null) return found;
            }
        }
        return null;
    }
}
