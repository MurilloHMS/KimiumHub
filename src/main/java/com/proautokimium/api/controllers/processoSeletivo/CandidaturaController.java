package com.proautokimium.api.controllers.processoSeletivo;

import com.proautokimium.api.Application.DTOs.processoSeletivo.candidaturas.CreateCandidaturaDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.candidaturas.ResponseCandidaturaDTO;
import com.proautokimium.api.Infrastructure.services.processoSeletivo.CandidaturaService;
import com.proautokimium.api.domain.entities.processoSeletivo.Candidatura;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/candidatura")
public class CandidaturaController {

    private final CandidaturaService candidaturaService;

    public CandidaturaController(CandidaturaService candidaturaService) {
        this.candidaturaService = candidaturaService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCandidaturasByVagaId(@PathVariable UUID id){
        List<ResponseCandidaturaDTO> result = candidaturaService.getCandidaturaByVagaId(id);
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<?> createCandidatura(@RequestBody CreateCandidaturaDTO dto){
        candidaturaService.create(dto);
        return ResponseEntity.ok("Candidatura realizada com sucesso");
    }

    @PutMapping("/{id}/avancar")
    public ResponseEntity<?> avancarEtapa(@PathVariable UUID id){
        candidaturaService.avancarEtapa(id);
        return ResponseEntity.ok("Etapa avançada com sucesso");
    }

    @PutMapping("/{id}/aprovar")
    public ResponseEntity<?> aprovarCandidato(@PathVariable UUID id){
        candidaturaService.aprovarCandidatura(id);
        return ResponseEntity.ok("Candidatura aprovada com sucesso");
    }

    @PutMapping("/{id}/reprovar")
    public ResponseEntity<?> reprovarCandidato(@PathVariable UUID id){
        candidaturaService.reprovarCandidatura(id);
        return ResponseEntity.ok("Candidatura reprovada com sucesso");
    }

    @PutMapping("/{id}/encerrar")
    public ResponseEntity<?> encerrarCandidato(@PathVariable UUID id){
        candidaturaService.encerrarCandidatura(id);
        return ResponseEntity.ok("Candidatura encerrada com sucesso");
    }
}
