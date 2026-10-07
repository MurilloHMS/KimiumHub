package com.proautokimium.api.Infrastructure.services.email.smtp;

import com.proautokimium.api.domain.entities.email.EmailQueue;
import jakarta.mail.util.ByteArrayDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.util.List;

@Service
public class SmtpService {

	private final JavaMailSender mailSender;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SmtpService.class);

    public SmtpService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

	/** Um arquivo anexado a um e-mail, em memória: o disco é assunto da fila. */
	public record Attachment(String fileName, byte[] content, String contentType) {}

	/**
	 * Entrega ao servidor, com o nome de exibição do remetente e os anexos. A
	 * falha sobe: quem chamou grava o motivo na fila.
	 */
	public void send(EmailQueue email, List<Attachment> attachments) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

			String name = email.getFromName() == null || email.getFromName().isBlank() ? "Proauto Kimium" : email.getFromName();
			helper.setFrom(email.getFromEmail(), name);
			helper.setTo(email.getToEmail());
			helper.setSubject(email.getSubject());
			helper.setText(email.getBody(), true);
			if (email.getReplyTo() != null && !email.getReplyTo().isBlank()) {
				helper.setReplyTo(email.getReplyTo());
			}
			for (Attachment a : attachments) {
				helper.addAttachment(a.fileName(), new ByteArrayDataSource(a.content(), a.contentType()));
			}
			mailSender.send(message);
		} catch (jakarta.mail.MessagingException | java.io.UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}
}

