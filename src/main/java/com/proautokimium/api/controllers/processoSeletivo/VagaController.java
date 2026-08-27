package com.proautokimium.api.controllers.processoSeletivo;

import com.proautokimium.api.Application.DTOs.processoSeletivo.vaga.CreateVagaDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.vaga.UpdateVagaDTO;
import com.proautokimium.api.Infrastructure.services.processoSeletivo.VagaService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api/vaga")
public class VagaController {

    private final VagaService vagaService;

    public VagaController(VagaService vagaService) {
        this.vagaService = vagaService;
    }

    @GetMapping("/publicadas")
    public ResponseEntity<?> getVagasPublicadas(){
        return ResponseEntity.ok(vagaService.listarVagasPublicadas());
    }

    @PreAuthorize("hasAuthority('rh/painel-de-vagas:CONSULTAR')")
    @GetMapping("/arquivadas")
    public ResponseEntity<?> getVagasArquivadas(){
        return ResponseEntity.ok(vagaService.listarVagasArquivadas());
    }

    @PreAuthorize("hasAuthority('rh/painel-de-vagas:CONSULTAR')")
    @GetMapping("/rascunhos")
    public ResponseEntity<?> getVagasRascunhos(){
        return ResponseEntity.ok(vagaService.listarVagasEmRascunho());
    }

    @PreAuthorize("hasAuthority('rh/painel-de-vagas:CONSULTAR')")
    @GetMapping("/encerradas")
    public  ResponseEntity<?> getVagasEncerradas(){
        return ResponseEntity.ok(vagaService.listarVagasEncerrados());
    }

    @PreAuthorize("hasAuthority('rh/painel-de-vagas:INCLUIR')")
    @PostMapping
    public ResponseEntity<?> cadastrarVaga(@RequestBody @Valid CreateVagaDTO dto){
        UUID id = vagaService.create(dto).getId();
        return ResponseEntity.ok(id);
    }

    @PreAuthorize("hasAuthority('rh/painel-de-vagas:ALTERAR')")
    @PutMapping
    public ResponseEntity<?> atualizarVaga(@RequestBody @Valid UpdateVagaDTO vaga){
        vagaService.update(vaga);
        return ResponseEntity.ok("Vaga Atualizada com sucesso!");
    }

    @PreAuthorize("hasAuthority('rh/painel-de-vagas:ALTERAR')")
    @PutMapping("/{id}/publicar")
    public ResponseEntity<?> publicarVaga(@PathVariable UUID id){
        vagaService.publicar(id);
        return ResponseEntity.ok("Vaga Publicada com sucesso!");
    }

    @PreAuthorize("hasAuthority('rh/painel-de-vagas:ALTERAR')")
    @PutMapping("/{id}/arquivar")
    public ResponseEntity<?> arquivarVaga(@PathVariable UUID id){
        vagaService.arquivar(id);
        return ResponseEntity.ok("Vaga Arquiva com sucesso!");
    }

    @PreAuthorize("hasAuthority('rh/painel-de-vagas:ALTERAR')")
    @PutMapping("/{id}/rascunho")
    public ResponseEntity<?> salvarVaga(@PathVariable UUID id){
        vagaService.rascunho(id);
        return ResponseEntity.ok("Vaga Salva em rascunho com sucesso!");
    }

    @PreAuthorize("hasAuthority('rh/painel-de-vagas:ALTERAR')")
    @PutMapping("/{id}/encerrar")
    public ResponseEntity<?> encerrarVaga(@PathVariable UUID id){
        vagaService.encerrar(id);
        return ResponseEntity.ok("Vaga Encerrada com sucesso!");
    }
}
