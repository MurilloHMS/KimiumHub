package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.smtp.SmtpMail;
import com.proautokimium.api.Infrastructure.repositories.SmtpEmailRepository;
import com.proautokimium.api.Infrastructure.services.email.EmailQueueService;
import com.proautokimium.api.domain.entities.EmailEntity;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidRequestDataException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/**
 * O envio manual da tela de Comunicação. Desde 2026-10-07 passa pela fila, como
 * todo e-mail do ERP: um por destinatário, com os anexos guardados no disco, e
 * a falha aparece na tela Fila de e-mails. Antes, mandava direto ao SMTP e
 * respondia "enviado" mesmo quando não saía.
 */
@RestController
@RequestMapping("api/smtp")
public class SmtpController {

	private final EmailQueueService emailQueue;
	private final SmtpEmailRepository senders;

	public SmtpController(EmailQueueService emailQueue, SmtpEmailRepository senders) {
		this.emailQueue = emailQueue;
		this.senders = senders;
	}

	@PreAuthorize("hasAuthority('settings/admin:CONFIGURAR')")
	@PostMapping(value = "send", consumes = "multipart/form-data")
	public ResponseEntity<?> sendEmail(
			@RequestPart("data") SmtpMail request,
			@RequestPart(value = "attachments", required = false) MultipartFile[] attachments) {

		if (request.recipients() == null || request.recipients().isEmpty()) {
			throw new InvalidRequestDataException("Informe pelo menos um destinatário.");
		}
		EmailEntity sender = senders.findAll().stream()
				.filter(s -> s.getEmail() != null && s.getEmail().getAddress().equalsIgnoreCase(request.sender()))
				.filter(EmailEntity::isActive)
				.findFirst()
				.orElseThrow(() -> new InvalidRequestDataException("Escolha um remetente ativo entre os e-mails da empresa."));

		List<EmailQueueService.OutgoingAttachment> files = new ArrayList<>();
		if (attachments != null) {
			for (MultipartFile f : attachments) {
				try {
					files.add(new EmailQueueService.OutgoingAttachment(f.getOriginalFilename(), f.getBytes(), f.getContentType()));
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
		}
		String name = sender.getDisplayName() == null || sender.getDisplayName().isBlank() ? "Proauto Kimium" : sender.getDisplayName();
		for (String to : request.recipients()) {
			emailQueue.enqueueAs(sender.getEmail().getAddress(), name, request.replyTo(), to, request.subject(), request.body(), files);
		}
		return ResponseEntity.accepted().body(request.recipients().size() + " e-mail(s) na fila. Saem no próximo minuto.");
	}
}
