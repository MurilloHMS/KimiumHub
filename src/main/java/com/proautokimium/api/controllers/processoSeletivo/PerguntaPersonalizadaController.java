package com.proautokimium.api.controllers.processoSeletivo;

import com.proautokimium.api.Application.DTOs.processoSeletivo.perguntas.CreatePerguntaDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.perguntas.UpdatePerguntaDTO;
import com.proautokimium.api.Infrastructure.services.processoSeletivo.PerguntaPersonalizadaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api/pergunta")
public class PerguntaPersonalizadaController {

    private final PerguntaPersonalizadaService perguntaService;

    public PerguntaPersonalizadaController(PerguntaPersonalizadaService perguntaService){
        this.perguntaService = perguntaService;
    }

    @PostMapping("/{id}")
    public ResponseEntity<?> create(@RequestBody CreatePerguntaDTO dto, @PathVariable UUID id){
        perguntaService.create(dto, id);
        return ResponseEntity.ok("Pergunta criada com sucesso");
    }

    @PutMapping
    public ResponseEntity<?> update(@RequestBody UpdatePerguntaDTO dto){
        perguntaService.update(dto);
        return ResponseEntity.ok("Pergunta atualizada com sucesso");
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable UUID id){
        return ResponseEntity.ok(perguntaService.listarPerguntasPorVaga(id));
    }
}
