package com.proautokimium.api.controllers.processoSeletivo;

import com.proautokimium.api.Application.DTOs.processoSeletivo.talentBank.AccessLinkRequestDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.talentBank.CreateTalentBankEntryDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.talentBank.TalentBankEntryDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.talentBank.UpdateTalentBankEntryDTO;
import com.proautokimium.api.Infrastructure.services.processoSeletivo.TalentBankService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * O lado público do banco de talentos.
 *
 * <p><b>Controller separado do interno de propósito.</b> O nome do arquivo
 * dizer de que lado você está é a guarda estrutural mais barata que existe
 * contra anotar — ou esquecer de anotar — o método errado. Nada aqui leva
 * {@code @PreAuthorize}: estes caminhos estão no {@code SecurityPaths}, e o
 * {@code PublicPathsHaveNoPreAuthorizeTest} quebra o build se levarem.
 *
 * <p><b>A regra que decide quase todas as respostas deste arquivo:</b> nenhuma
 * delas pode revelar se um e-mail está na base. O dado — "esta pessoa procura
 * emprego" — é sensível, e a rota é pública e sem rate limiting.
 *
 * <p>E é por isso que <b>não existe um {@code GET .../exists?email=}</b>. É o
 * desenho óbvio, é o que alguém vai propor daqui a seis meses, e é exatamente
 * o oráculo: público, grátis, sem upload, uma requisição por endereço.
 */
@RestController
@RequestMapping("api/talent-bank/public")
public class TalentBankPublicController {

    private final TalentBankService talentBankService;

    public TalentBankPublicController(TalentBankService talentBankService) {
        this.talentBankService = talentBankService;
    }

    /**
     * Inscrição espontânea: currículo sem vaga aberta.
     *
     * <p>A resposta é <b>202 e idêntica</b> para e-mail novo e para e-mail que
     * já existe. Novo, cria tudo e manda o link; existente, não altera nada,
     * descarta o upload, e manda o mesmo link — quem quiser trocar o currículo
     * troca por lá.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> inscrever(
            @Valid @RequestPart("dados") CreateTalentBankEntryDTO dados,
            @RequestPart("curriculo") MultipartFile curriculo) throws IOException {

        talentBankService.inscrever(dados, curriculo);

        return ResponseEntity.accepted()
                .body("Recebemos seus dados. Enviamos um e-mail para você com os próximos passos.");
    }

    /**
     * Pede o link para ver e corrigir o que foi enviado.
     *
     * <p><b>202 sempre</b>, com o mesmo corpo, exista ou não o endereço. E o
     * e-mail vai para a fila, nunca imediato: SMTP inline no caminho conhecido
     * e resposta instantânea no desconhecido seriam um oráculo por latência —
     * que é o mesmo vazamento, só que pelo relógio.
     */
    @PostMapping("/access-link")
    public ResponseEntity<String> pedirLink(@Valid @RequestBody AccessLinkRequestDTO dados) {
        talentBankService.pedirLinkDeAcesso(dados.email());

        return ResponseEntity.accepted()
                .body("Se este e-mail estiver no nosso banco de talentos, você receberá um link em instantes.");
    }

    @GetMapping("/{token}")
    public ResponseEntity<TalentBankEntryDTO> ver(@PathVariable String token) {
        return ResponseEntity.ok(talentBankService.verPorToken(token));
    }

    /** Sem isto, "ver o que enviei" não significa muita coisa. */
    @GetMapping("/{token}/curriculo")
    public ResponseEntity<Resource> baixarCurriculo(@PathVariable String token) throws IOException {
        var caminho = talentBankService.curriculoPorToken(token);
        return CurriculoResponse.de(caminho, caminho.getFileName().toString());
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, path = "/{token}")
    public ResponseEntity<TalentBankEntryDTO> atualizar(
            @PathVariable String token,
            @Valid @RequestPart("dados") UpdateTalentBankEntryDTO dados,
            @RequestPart(value = "curriculo", required = false) MultipartFile curriculo) throws IOException {

        return ResponseEntity.ok(talentBankService.atualizarPorToken(token, dados, curriculo));
    }

    /**
     * "Apaguem meus dados."
     *
     * <p>Sem candidatura a linha sai inteira; com candidatura ela é
     * anonimizada — um {@code DELETE} duro estouraria a FK, ou levaria junto o
     * histórico de uma contratação real.
     */
    @DeleteMapping("/{token}")
    public ResponseEntity<Void> excluir(@PathVariable String token) throws IOException {
        talentBankService.excluirPorToken(token);
        return ResponseEntity.noContent().build();
    }
}
