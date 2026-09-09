package com.proautokimium.api.controllers;

import com.proautokimium.api.Infrastructure.services.newsletter.NewsletterOrchestratorService;
import com.proautokimium.api.Infrastructure.services.newsletter.NewsletterResumoService;
import com.proautokimium.api.Infrastructure.services.newsletter.NewsletterService;
import com.proautokimium.api.Application.DTOs.newsletter.ResumoDoMesDTO;
import com.proautokimium.api.domain.entities.Newsletter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @Autowired
    private NewsletterResumoService newsletterResumoService;

    /**
     * A fila contada por mês.
     *
     * A tela de envio abre por aqui: um cartão por mês com o que já saiu e o que
     * falta. `pending` continua existindo e continua servindo para a lista de
     * pendentes — o que ele não responde é "a de junho já saiu?", porque só
     * enxerga o que ainda não saiu.
     */
    @PreAuthorize("hasAuthority('communication/newsletter:CONSULTAR')")
    @GetMapping("resumo")
    @Operation(summary = "Contagem da fila por mês e status")
    public ResponseEntity<List<ResumoDoMesDTO>> resumoPorMes() {
        return ResponseEntity.ok(newsletterResumoService.porMes());
    }

    /** As linhas de um mês, todos os status, do maior faturamento para o menor. */
    @PreAuthorize("hasAuthority('communication/newsletter:CONSULTAR')")
    @GetMapping("mes")
    @Operation(summary = "As newsletters de um mês")
    public ResponseEntity<List<Newsletter>> doMes(@RequestParam int mes, @RequestParam int ano) {
        return ResponseEntity.ok(newsletterResumoService.doMes(mes, ano));
    }

    @PreAuthorize("hasAuthority('communication/newsletter:ENVIAR')")
    @PostMapping("send")
    @Operation(summary = "Envia Newsletter", description = "Envio Individual da Newsletter")
    public ResponseEntity<Object> sendNewsletter(@RequestBody Newsletter newsletter) throws MessagingException, UnsupportedEncodingException {
        newsletterService.sendMailWithInline(newsletter);

        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAuthority('communication/newsletter:INCLUIR')")
    @PostMapping("upload/one-file")
    @Operation(summary = "Cadastra Newsletters", description = "Recebe arquivo único para montar newsletter")
    public ResponseEntity<Object> includeNewsletters(@RequestParam MultipartFile file) {
        newsletterOrchestratorService.includeMonthlyNewsletterByExcel(file);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    
    @PreAuthorize("hasAuthority('communication/newsletter:CONSULTAR')")
    @GetMapping("pending")
    @Operation(summary = "Obtém Newsletters", description = "Retorna Newsletters pendentes de envio")
    public ResponseEntity<Object> getPendingEmails(){
    	var pendingEmails = newsletterService.getAllPendingEmails();
    	return pendingEmails != null ?
    			ResponseEntity.status(HttpStatus.OK).body(pendingEmails)
    			: ResponseEntity.status(HttpStatus.NO_CONTENT).body("Não há emails pendentes");
    }
    
    @PreAuthorize("hasAuthority('communication/newsletter:ENVIAR')")
    @PostMapping("pending/send")
    @Operation(summary = "Envia Newsletters Pendentes", description = "Envio das Newsletters pendentes")
    public ResponseEntity<Object> sentPendingNewsletter(){
    	newsletterService.setReadyToSend();
    	return ResponseEntity.ok("Envio de newsletters iniciado.");
    }
}
