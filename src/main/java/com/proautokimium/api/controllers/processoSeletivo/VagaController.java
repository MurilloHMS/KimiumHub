package com.proautokimium.api.controllers.processoSeletivo;

import com.proautokimium.api.Infrastructure.repositories.processoSeletivo.VagaRepository;
import com.proautokimium.api.Infrastructure.services.processoSeletivo.VagaService;
import com.proautokimium.api.domain.entities.processoSeletivo.Vaga;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/vaga")
public class VagaController {

    @Autowired
    private VagaService vagaService;

    @GetMapping("/publicadas")
    public ResponseEntity<?> getVagasPublicadas(){
        return ResponseEntity.ok(vagaService.listarVagasPublicadas());
    }

    @GetMapping("/arquivadas")
    public ResponseEntity<?> getVagasArquivadas(){
        return ResponseEntity.ok(vagaService.listarVagasArquivadas());
    }

    @GetMapping("/Rascunhos")
    public ResponseEntity<?> getVagasRascunhos(){
        return ResponseEntity.ok(vagaService.listarVagasEmRascunho());
    }

    @GetMapping("/Encerradas")
    public  ResponseEntity<?> getVagasEncerradas(){
        return ResponseEntity.ok(vagaService.listarVagasEncerrados());
    }

    @PostMapping
    public ResponseEntity<?> cadastrarVaga(@RequestBody @Valid @NotBlank Vaga vaga){
        vagaService.create(vaga);
        return ResponseEntity.ok("Vaga Cadastrada com sucesso!");
    }

    @PutMapping
    public ResponseEntity<?> atualizarVaga(@RequestBody @Valid Vaga vaga){
        vagaService.update(vaga);
        return ResponseEntity.ok("Vaga Atualizada com sucesso!");
    }

    @PutMapping("/publicar")
    public ResponseEntity<?> publicarVaga(@RequestBody @Valid Vaga vaga){
        vagaService.publicar(vaga);
        return ResponseEntity.ok("Vaga Publicada com sucesso!");
    }

    @PutMapping("/arquivar")
    public ResponseEntity<?> arquivarVaga(@RequestBody @Valid Vaga vaga){
        vagaService.arquivar(vaga);
        return ResponseEntity.ok("Vaga Arquiva com sucesso!");
    }

    @PutMapping("/rascunho")
    public ResponseEntity<?> salvarVaga(@RequestBody @Valid Vaga vaga){
        vagaService.rascunho(vaga);
        return ResponseEntity.ok("Vaga Salva em rascunho com sucesso!");
    }

    @PutMapping("/encerrar")
    public ResponseEntity<?> encerrarVaga(@RequestBody @Valid Vaga vaga){
        vagaService.encerrar(vaga);
        return ResponseEntity.ok("Vaga Encerrada com sucesso!");
    }
}
