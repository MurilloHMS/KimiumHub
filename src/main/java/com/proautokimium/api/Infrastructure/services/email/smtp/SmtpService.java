package com.proautokimium.api.Infrastructure.services.email.smtp;

import com.proautokimium.api.domain.entities.email.EmailQueue;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.proautokimium.api.Application.DTOs.smtp.SmtpMail;
import jakarta.mail.internet.MimeMessage;

import javax.xml.crypto.Data;
import java.util.Objects;

@Service
public class SmtpService {

	@Autowired
	JavaMailSender mailSender;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SmtpService.class);

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

		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

			helper.setFrom("seuemail@empresa.com", "Proauto Kimium");
			helper.setTo(email.getToEmail());
			helper.setSubject(email.getSubject());
			helper.setText(email.getBody(), true);

			if (email.getReplyTo() != null) {
				helper.setReplyTo(email.getReplyTo());
			}

			mailSender.send(message);

		} catch (Exception e) {
			throw new RuntimeException("Erro ao enviar email", e);
		}
	}
}

