package com.proautokimium.api.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.proautokimium.api.Application.DTOs.smtp.SmtpMail;
import com.proautokimium.api.Infrastructure.services.email.smtp.SmtpService;

@RestController
@RequestMapping("api/smtp")
public class SmtpController {
	
	@Autowired
	SmtpService service;
	
	@PostMapping( value = "send", consumes = "multipart/form-data")
	public ResponseEntity<?> sendEmail(
            @RequestPart("data") SmtpMail request,
	        @RequestPart(value = "attachments", required = false) MultipartFile[] attachments) {
	
	    
	    SmtpMail emailData = new SmtpMail(
	            request.recipients(),
	            request.sender(),
	            request.subject(),
	            request.body(),
	            request.cc(),
	            request.bcc(),
	            attachments,
	            request.imageBase64()
	    );
	
	    service.sendEmail(emailData);
	    return ResponseEntity.ok("E-mail enviado com sucesso!");
	}
}
