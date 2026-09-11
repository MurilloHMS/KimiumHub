package com.proautokimium.api.controllers.processoSeletivo;

import com.proautokimium.api.Application.DTOs.processoSeletivo.talentBank.TalentBankSummaryDTO;
import com.proautokimium.api.Infrastructure.services.processoSeletivo.TalentBankService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * A aba interna do banco de talentos.
 *
 * <p>Mora em {@code rh/painel-de-vagas}, que já existe no catálogo de telas
 * (V84) e já está nos modelos ADMIN e RH (V85). <b>Fazer aba em vez de rota
 * nova economizou a migration de permissão inteira</b> — as sete authorities
 * dessa tela já estão concedidas.
 */
@RestController
@RequestMapping("api/talent-bank")
public class TalentBankController {

    private final TalentBankService talentBankService;

    public TalentBankController(TalentBankService talentBankService) {
        this.talentBankService = talentBankService;
    }

    @PreAuthorize("hasAuthority('rh/painel-de-vagas:CONSULTAR')")
    @GetMapping
    public ResponseEntity<List<TalentBankSummaryDTO>> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String origem,
            @RequestParam(required = false) String consentimento,
            @RequestParam(required = false) String situacao) {

        return ResponseEntity.ok(talentBankService.listar(q, area, origem, consentimento, situacao));
    }

    /**
     * Download pelo <b>id do candidato</b>, e não pelo nome do arquivo.
     *
     * <p>Quem chama nunca nomeia arquivo, o que elimina o input de travessia
     * inteiro — o endpoint antigo recebe {@code {fileName}} de quem chama, e
     * por isso precisou de guarda na classe base.
     *
     * <p>Aceita as <b>duas</b> authorities de {@code :BAIXAR}. A aba vive na
     * tela de vagas, mas o mesmo arquivo é o currículo que a tela de
     * candidaturas baixa — anotar com uma só deixaria um dos dois lados com o
     * botão visível e 403 no clique.
     */
    @PreAuthorize("hasAnyAuthority('rh/painel-de-vagas:BAIXAR','rh/candidaturas:BAIXAR')")
    @GetMapping("/{id}/curriculo")
    public ResponseEntity<Resource> baixarCurriculo(@PathVariable UUID id) throws IOException {
        return CurriculoResponse.de(
                talentBankService.curriculoDe(id),
                talentBankService.nomeDoArquivoDe(id));
    }

    /**
     * Exclusão pedida por telefone ou e-mail, feita pelo RH.
     *
     * <p>Alguém vai subir currículo de brincadeira na primeira semana, e tem
     * que haver um caminho que não seja SQL. Mesma rotina do "apagar meus
     * dados": sem candidatura apaga, com candidatura anonimiza.
     */
    @PreAuthorize("hasAuthority('rh/painel-de-vagas:EXCLUIR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) throws IOException {
        talentBankService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
