package com.proautokimium.api.Infrastructure.services.email.smtp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.proautokimium.api.Application.DTOs.smtp.SmtpMail;
import jakarta.mail.internet.MimeMessage;

@Service
public class SmtpService {

	@Autowired
	JavaMailSender mailSender;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SmtpService.class);
	
	public void sendEmail(SmtpMail request) {
		
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true);
			
			helper.setFrom(request.sender());
			helper.setTo(request.recipients().toArray(new String[0]));
			if(request.cc() != null) helper.setCc(request.cc().toArray(new String[0]));
			if(request.bcc() != null) helper.setBcc(request.bcc().toArray(new String[0]));
			helper.setSubject(request.subject());
			helper.setText(request.body(), true);
			
			if(request.attachments() != null) {
				for(MultipartFile file: request.attachments()) {
					helper.addAttachment(file.getOriginalFilename(), file);
				}
			}
			
			mailSender.send(message);
		}catch (Exception e) {
			LOGGER.error("Erro ao enviar e-mail: " + e.getMessage());
		}
	}
}
