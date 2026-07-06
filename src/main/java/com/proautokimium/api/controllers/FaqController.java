package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.faq.FaqCreateDTO;
import com.proautokimium.api.Application.DTOs.faq.FaqUpdateDTO;
import com.proautokimium.api.Infrastructure.services.faq.FaqService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Responsável pelo controle dos cadastros das perguntas e respostas da empresa
 */
@RestController
@RequestMapping("api/faq")
@Tag(name= "Perguntas e Respostas", description = "Gerenciador do FAQ da empresa")
public class FaqController {

    @Autowired
    private FaqService service;

    @GetMapping()
    @Operation(summary = "Obtém todas as perguntas", description = "Retorna lista com perguntas e respostas")
    public ResponseEntity<Object> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("public")
    @Operation(summary = "Obtém as perguntas públicas", description = "Retorna as perguntas visíveis ao público")
    public ResponseEntity<Object> getAllPublic() {
        return ResponseEntity.ok(service.getAllPublic());
    }

    @PostMapping
    @Operation(summary = "Cria uma pergunta", description = "Cria o cadastro com os dados enviados")
    public ResponseEntity<Object> create(@RequestBody FaqCreateDTO dto){
        service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body("Faq criado com sucesso!");
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma pergunta", description = "Atualiza o cadastro com os dados enviados")
    public ResponseEntity<Object> update(@RequestBody FaqUpdateDTO dto, @PathVariable("id") UUID id){
        service.update(id, dto);
        return ResponseEntity.status(HttpStatus.OK).body("Faq atualizado com sucesso!");
    }

    @PutMapping("/{id}/arquivar")
    @Operation(summary = "Arquiva FAQ", description = "Altera o Status para Arquivado")
    public ResponseEntity<Object> arquivar(@PathVariable("id") UUID id){
        service.setArchived(id);
        return ResponseEntity.status(HttpStatus.OK).body("Faq arquivado com sucesso!");
    }

    @PutMapping("/{id}/rascunho")
    @Operation(summary = "Marca FAQ como Rascunho", description = "Altera o Status para Rascunho")
    public ResponseEntity<Object> rascunho(@PathVariable("id") UUID id){
        service.setDraft(id);
        return ResponseEntity.status(HttpStatus.OK).body("Faq marcado como rascunho com sucesso!");
    }

    @PutMapping("/{id}/publicar")
    @Operation(summary = "Publica FAQ", description = "Altera o Status para Publicado")
    public ResponseEntity<Object> publicar(@PathVariable("id") UUID id){
        service.setPublished(id);
        return ResponseEntity.status(HttpStatus.OK).body("Faq publicado com sucesso!");
    }
}
