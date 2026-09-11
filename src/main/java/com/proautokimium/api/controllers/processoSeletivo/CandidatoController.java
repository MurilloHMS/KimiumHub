package com.proautokimium.api.controllers.processoSeletivo;

import com.proautokimium.api.Application.DTOs.processoSeletivo.candidato.CreateCandidatoDTO;
import com.proautokimium.api.Infrastructure.services.processoSeletivo.CandidatoService;
import com.proautokimium.api.domain.entities.processoSeletivo.Candidato;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/candidato")
public class CandidatoController {

    private final CandidatoService candidatoService;

    public CandidatoController(CandidatoService candidatoService){
        this.candidatoService = candidatoService;
    }

    /**
     * Cadastro manual de candidato.
     *
     * <p><b>Esta rota nao tinha authority nenhuma ate 2026-09-11</b>, e nao
     * estava no {@code SecurityPaths}: caia no {@code anyRequest()}, entao
     * qualquer funcionario logado criava candidato.
     *
     * <p>A varredura do site nao achou chamador, mas a medicao em producao
     * mostrou que as 21 linhas de {@code candidatos} tem {@code criado_em}
     * preenchido — e o unico codigo capaz disso e o converter deste caminho.
     * Varredura de front nao e prova sobre Swagger nem sobre outro cliente,
     * entao a rota fica, com guarda.
     */
    @PreAuthorize("hasAuthority('rh/candidaturas:INCLUIR')")
    @PostMapping
    public ResponseEntity<?> cadastrarCandidato(@RequestBody CreateCandidatoDTO dto){
        candidatoService.create(dto);
        return ResponseEntity.ok("Candidato cadastrado com sucesso");
    }

    @PreAuthorize("hasAuthority('rh/candidaturas:CONSULTAR')")
    @GetMapping
    public ResponseEntity<?> listarCandidatos(){
        return ResponseEntity.ok().body(candidatoService.listarCandidatos());
    }
}
