package com.proautokimium.api.Application.DTOs.smtp;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public record SmtpMail(
	    List<String> recipients,
	    String sender,
		String replyTo,
	    String subject,
	    String body,
	    List<String> cc,
	    List<String> bcc,
	    String imageBase64
	) {}