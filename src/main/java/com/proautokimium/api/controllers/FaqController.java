package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.faq.FaqCreateDTO;
import com.proautokimium.api.Application.DTOs.faq.FaqUpdateDTO;
import com.proautokimium.api.Infrastructure.services.faq.FaqService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api/faq")
public class FaqController {

    @Autowired
    private FaqService service;

    @GetMapping()
    public ResponseEntity<Object> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("public")
    public ResponseEntity<Object> getAllPublic() {
        return ResponseEntity.ok(service.getAllPublic());
    }

    @PostMapping
    public ResponseEntity<Object> create(@RequestBody FaqCreateDTO dto){
        service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body("Faq criado com sucesso!");
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> update(@RequestBody FaqUpdateDTO dto, @RequestParam("id")UUID id){
        service.update(id, dto);
        return ResponseEntity.status(HttpStatus.OK).body("Faq atualizado com sucesso!");
    }

    @PutMapping("/{id}/arquivar")
    public ResponseEntity<Object> arquivar(@RequestParam("id")UUID id){
        service.setArchived(id);
        return ResponseEntity.status(HttpStatus.OK).body("Faq arquivado com sucesso!");
    }

    @PutMapping("/{id}/rascunho")
    public ResponseEntity<Object> rascunho(@RequestParam("id")UUID id){
        service.setDraft(id);
        return ResponseEntity.status(HttpStatus.OK).body("Faq marcado como rascunho com sucesso!");
    }

    @PutMapping("/{id}/publicar")
    public ResponseEntity<Object> publicar(@RequestParam("id")UUID id){
        service.setPublished(id);
        return ResponseEntity.status(HttpStatus.OK).body("Faq publicado com sucesso!");
    }
}
