package com.proautokimium.api.controllers;

import com.proautokimium.api.Infrastructure.services.email.newsletter.NewsletterOrchestratorService;
import com.proautokimium.api.Infrastructure.services.email.newsletter.NewsletterService;
import com.proautokimium.api.domain.entities.Newsletter;

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

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.List;

@RestController
@RequestMapping("api/newsletter")
public class NewsletterController {
    @Autowired
    private NewsletterService newsletterService;
    
    @Autowired
    private NewsletterOrchestratorService newsletterOrchestratorService;

    @PostMapping("send")
    public ResponseEntity<Object> sendNewsletter(@RequestBody Newsletter newsletter) throws MessagingException, UnsupportedEncodingException {
        newsletterService.sendMailWithInline(newsletter);

        return ResponseEntity.ok().build();
    }
    
    @PostMapping("upload")
    public ResponseEntity<Object> includeNewsletters(@RequestParam List<MultipartFile> files, @RequestParam(required = false) boolean isMatriz) throws Exception{
    	if(files.size() > 4) {
    		return ResponseEntity.badRequest().body("Máximo permitido 4 arquivos. Você enviou " + files.size());
    	}
    	
    	newsletterOrchestratorService.includeMonthlyNewsletter(files, isMatriz);
    	return ResponseEntity.ok().build();
    }

    @PostMapping("upload/one-file")
    public ResponseEntity<Object> includeNewsletters(@RequestParam MultipartFile file) {
        newsletterOrchestratorService.includeMonthlyNewsletterByExcel(file);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    
    @GetMapping("pending")
    public ResponseEntity<Object> getPendingEmails(){
    	var pendingEmails = newsletterService.getAllPendingEmails();
    	return pendingEmails != null ?
    			ResponseEntity.status(HttpStatus.OK).body(pendingEmails)
    			: ResponseEntity.status(HttpStatus.NO_CONTENT).body("Não há emails pendentes");
    }
    
    @PostMapping("pending/send")
    public ResponseEntity<Object> sentPendingNewsletter(){
    	newsletterOrchestratorService.executeMonthlyNewsletter();
    	return ResponseEntity.ok("Envio de newsletters iniciado.");
    }
}
