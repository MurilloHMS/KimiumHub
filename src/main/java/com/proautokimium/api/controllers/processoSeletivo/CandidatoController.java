package com.proautokimium.api.controllers.processoSeletivo;

import com.proautokimium.api.Application.DTOs.processoSeletivo.candidato.CreateCandidatoDTO;
import com.proautokimium.api.Infrastructure.services.processoSeletivo.CandidatoService;
import com.proautokimium.api.domain.entities.processoSeletivo.Candidato;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/candidato")
public class CandidatoController {

    private final CandidatoService candidatoService;

    public CandidatoController(CandidatoService candidatoService){
        this.candidatoService = candidatoService;
    }

    @PostMapping
    public ResponseEntity<?> cadastrarCandidato(@RequestBody CreateCandidatoDTO dto){
        candidatoService.create(dto);
        return ResponseEntity.ok("Candidato cadastrado com sucesso");
    }

    @GetMapping
    public ResponseEntity<?> listarCandidatos(){
        return ResponseEntity.ok().body(candidatoService.listarCandidatos());
    }
}
