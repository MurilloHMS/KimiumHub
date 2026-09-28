package com.proautokimium.api.Infrastructure.services.email.smtp;

import com.proautokimium.api.domain.entities.email.EmailQueue;
import jakarta.mail.util.ByteArrayDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.proautokimium.api.Application.DTOs.smtp.SmtpMail;
import jakarta.mail.internet.MimeMessage;

@Service
public class SmtpService {

	private final JavaMailSender mailSender;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SmtpService.class);

    public SmtpService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(SmtpMail request, MultipartFile[] attachments) {
		try {

			for (String recipient : request.recipients()) {

				MimeMessage message = mailSender.createMimeMessage();
				MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

				helper.setFrom(request.sender(), "Proauto Kimium");
				helper.setTo(recipient);
				helper.setSubject(request.subject());
				helper.setText(request.body(), true);

				if (request.replyTo() != null && !request.replyTo().isEmpty()) {
					helper.setReplyTo(request.replyTo());
				}

				if (attachments != null) {
					for (MultipartFile file : attachments) {
						helper.addAttachment(
							file.getOriginalFilename(),
							new ByteArrayDataSource(file.getBytes(), file.getContentType())
						);
					}
				}

				mailSender.send(message);
			}

		} catch (Exception e) {
			LOGGER.error("Erro ao enviar e-mail: {}", e.getMessage(), e);
		}
	}

	public void sendEmail(EmailQueue email) {
		sendWithAttachment(email, null);
	}

	/** Um arquivo anexado a um e-mail — só em memória, não vai para a fila. */
	public record Attachment(String fileName, byte[] content, String contentType) {}

	/**
	 * Envia com anexo. Diferente do {@code sendEmail(SmtpMail, MultipartFile[])},
	 * que engole o erro no log: aqui a falha sobe, para quem chamou poder dizer
	 * à pessoa que o e-mail NÃO saiu.
	 *
	 * Nome próprio, e não mais um {@code sendEmail}: com dois argumentos, ele
	 * ficaria ambíguo com o envio por {@code SmtpMail} em qualquer {@code sendEmail(any(), any())}.
	 */
	public void sendWithAttachment(EmailQueue email, Attachment attachment) {

		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

			helper.setFrom(email.getFromEmail(), "Proauto Kimium");
			helper.setTo(email.getToEmail());
			helper.setSubject(email.getSubject());
			helper.setText(email.getBody(), true);

			if (email.getReplyTo() != null) {
				helper.setReplyTo(email.getReplyTo());
			}

			if (attachment != null) {
				helper.addAttachment(attachment.fileName(),
						new ByteArrayDataSource(attachment.content(), attachment.contentType()));
			}

			mailSender.send(message);

		} catch (Exception e) {
			LOGGER.error("Erro ao enviar e-mail", e);
			throw new RuntimeException(e);
		}
	}
}

