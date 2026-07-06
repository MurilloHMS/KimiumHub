package com.proautokimium.api.controllers;

import com.proautokimium.api.Infrastructure.services.email.newsletter.NewsletterOrchestratorService;
import com.proautokimium.api.Infrastructure.services.email.newsletter.NewsletterService;
import com.proautokimium.api.domain.entities.Newsletter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.UnsupportedEncodingException;
import java.util.List;

@RestController
@RequestMapping("api/newsletter")
@Tag(name = "Newsletter", description = "Controle das newsletters")
public class NewsletterController {
    @Autowired
    private NewsletterService newsletterService;
    
    @Autowired
    private NewsletterOrchestratorService newsletterOrchestratorService;

    @PostMapping("send")
    @Operation(summary = "Envia Newsletter", description = "Envio Individual da Newsletter")
    public ResponseEntity<Object> sendNewsletter(@RequestBody Newsletter newsletter) throws MessagingException, UnsupportedEncodingException {
        newsletterService.sendMailWithInline(newsletter);

        return ResponseEntity.ok().build();
    }
    
    @PostMapping("upload")
    @Operation(summary = "Cadastra Newsletters", description = "Recebe arquivos para montar newsletters")
    public ResponseEntity<Object> includeNewsletters(@RequestParam List<MultipartFile> files, @RequestParam(required = false) boolean isMatriz) throws Exception{
    	if(files.size() > 4) {
    		return ResponseEntity.badRequest().body("Máximo permitido 4 arquivos. Você enviou " + files.size());
    	}
    	
    	newsletterOrchestratorService.includeMonthlyNewsletter(files, isMatriz);
    	return ResponseEntity.ok().build();
    }

    @PostMapping("upload/one-file")
    @Operation(summary = "Cadastra Newsletters", description = "Recebe arquivo único para montar newsletter")
    public ResponseEntity<Object> includeNewsletters(@RequestParam MultipartFile file) {
        newsletterOrchestratorService.includeMonthlyNewsletterByExcel(file);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    
    @GetMapping("pending")
    @Operation(summary = "Obtém Newsletters", description = "Retorna Newsletters pendentes de envio")
    public ResponseEntity<Object> getPendingEmails(){
    	var pendingEmails = newsletterService.getAllPendingEmails();
    	return pendingEmails != null ?
    			ResponseEntity.status(HttpStatus.OK).body(pendingEmails)
    			: ResponseEntity.status(HttpStatus.NO_CONTENT).body("Não há emails pendentes");
    }
    
    @PostMapping("pending/send")
    @Operation(summary = "Envia Newsletters Pendentes", description = "Envio das Newsletters pendentes")
    public ResponseEntity<Object> sentPendingNewsletter(){
    	newsletterService.setReadyToSend();
    	return ResponseEntity.ok("Envio de newsletters iniciado.");
    }
}
