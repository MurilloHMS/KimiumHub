package com.proautokimium.api.Infrastructure.services.processoSeletivo;

import com.proautokimium.api.Application.DTOs.processoSeletivo.talentBank.CreateTalentBankEntryDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.talentBank.UpdateTalentBankEntryDTO;
import com.proautokimium.api.Infrastructure.exceptions.processoSeletivo.CurriculoInvalidoException;
import com.proautokimium.api.Infrastructure.exceptions.processoSeletivo.LinkDeAcessoExpiradoException;
import com.proautokimium.api.Infrastructure.exceptions.processoSeletivo.LinkDeAcessoInvalidoException;
import com.proautokimium.api.Infrastructure.repositories.processoSeletivo.CandidaturaRepository;
import com.proautokimium.api.Infrastructure.repositories.processoSeletivo.CandidatoRepository;
import com.proautokimium.api.Infrastructure.services.email.TalentBankEmailService;
import com.proautokimium.api.Infrastructure.services.storage.StorageService;
import com.proautokimium.api.domain.abstractions.Entity;
import com.proautokimium.api.domain.entities.processoSeletivo.Candidato;
import com.proautokimium.api.domain.entities.processoSeletivo.TalentBankAccessToken;
import com.proautokimium.api.domain.valueObjects.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * O banco de talentos.
 *
 * <p><b>Metade destes testes existe por causa de uma regra só:</b> nenhuma
 * resposta pode revelar se um e-mail está na base. O dado vazado — "esta
 * pessoa procura emprego" — é sensível, e a rota é pública, grátis e sem rate
 * limiting.
 *
 * <p>Eles são o que impede a "melhoria" bem-intencionada que alguém vai propor
 * daqui a seis meses: devolver 409 quando o e-mail já existe, ou 404 quando não
 * existe. As duas parecem melhores e as duas são o oráculo.
 */
@ExtendWith(MockitoExtension.class)
class TalentBankServiceTest {

    @Mock CandidatoRepository candidatoRepository;
    @Mock CandidaturaRepository candidaturaRepository;
    @Mock StorageService storageService;
    @Mock TalentBankAccessTokenService tokenService;
    @Mock TalentBankEmailService emailService;

    private static final Clock RELOGIO =
            Clock.fixed(Instant.parse("2026-09-11T14:30:00Z"), ZoneId.of("America/Sao_Paulo"));
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 9, 11, 11, 30);
    private static final int RETENCAO = 24;

    private TalentBankService service;

    @BeforeEach
    void setUp() {
        service = new TalentBankService(candidatoRepository, candidaturaRepository, storageService,
                tokenService, emailService, RELOGIO, RETENCAO);
    }

    private static MultipartFile pdf() {
        return new MockMultipartFile("curriculo", "cv.pdf", null,
                "%PDF-1.7".getBytes(StandardCharsets.UTF_8));
    }

    private static CreateTalentBankEntryDTO inscricao() {
        return new CreateTalentBankEntryDTO("Maria Souza", "Maria@Email.com", "44999990000",
                "linkedin.com/in/maria", "Produção", true);
    }

    private static Candidato candidatoSalvo(String email) {
        Candidato c = new Candidato();
        c.setNome("Maria Souza");
        c.setEmail(new Email(email));
        c.setTelefone("44999990000");
        c.setCriadoEm(AGORA.minusMonths(6));
        darId(c);
        return c;
    }

    private static void darId(Candidato c) {
        try {
            Field id = Entity.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(c, UUID.randomUUID());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    // ─── Inscrição espontânea ────────────────────────────────────────────────

    @Test
    @DisplayName("E-mail novo cria candidato com consentimento, prazo e curriculo")
    void emailNovoCria() throws Exception {
        when(candidatoRepository.findByEmail_AddressIgnoreCase("maria@email.com"))
                .thenReturn(Optional.empty());
        when(candidatoRepository.save(any(Candidato.class))).thenAnswer(i -> {
            Candidato c = i.getArgument(0);
            if (c.getId() == null) darId(c);
            return c;
        });
        when(storageService.save(any(), anyString())).thenReturn("arquivo.pdf");
        when(tokenService.emitirPara(any())).thenReturn(Optional.of("tok"));

        service.inscrever(inscricao(), pdf());

        ArgumentCaptor<Candidato> captor = ArgumentCaptor.forClass(Candidato.class);
        verify(candidatoRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        Candidato salvo = captor.getValue();

        assertThat(salvo.getEmail().getAddress())
                .as("o indice unico e sobre lower(email): gravar com a caixa digitada criaria a segunda pessoa")
                .isEqualTo("maria@email.com");
        assertThat(salvo.getConsentimentoEm()).isEqualTo(AGORA);
        assertThat(salvo.getExpiraEm()).isEqualTo(AGORA.plusMonths(RETENCAO));
        assertThat(salvo.getPathCurriculo()).isEqualTo("arquivo.pdf");
        verify(emailService).enviarLinkDeAcesso(eqIgnorandoCaixa("maria@email.com"), anyString(), anyString());
    }

    private static String eqIgnorandoCaixa(String esperado) {
        return org.mockito.ArgumentMatchers.argThat(esperado::equalsIgnoreCase);
    }

    /**
     * <b>O teste que mais importa deste arquivo.</b>
     *
     * <p>E-mail que já existe não vira 409 e não sobrescreve: o upload é
     * descartado e a pessoa recebe o mesmo link de sempre. Um 409 seria o
     * oráculo; sobrescrever institucionalizaria o defeito que acabamos de
     * corrigir no caminho de candidatura, e apagaria o cadastro de quem errou o
     * endereço.
     */
    @Test
    @DisplayName("E-mail que ja existe nao altera nada e nao grava arquivo")
    void emailExistenteNaoAlteraNada() throws Exception {
        Candidato existente = candidatoSalvo("maria@email.com");
        existente.setNome("Maria Antiga");
        existente.setPathCurriculo("o-bom.pdf");

        when(candidatoRepository.findByEmail_AddressIgnoreCase("maria@email.com"))
                .thenReturn(Optional.of(existente));
        when(tokenService.emitirPara(existente)).thenReturn(Optional.of("tok"));

        service.inscrever(inscricao(), pdf());

        verify(storageService, never()).save(any(), anyString());
        assertThat(existente.getNome()).isEqualTo("Maria Antiga");
        assertThat(existente.getPathCurriculo()).isEqualTo("o-bom.pdf");
        verify(emailService).enviarLinkDeAcesso(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Inscricao espontanea sem curriculo e recusada")
    void semCurriculoERecusada() {
        assertThatThrownBy(() -> service.inscrever(inscricao(), null))
                .as("sem vaga e sem curriculo isto vira agenda de contatos")
                .isInstanceOf(CurriculoInvalidoException.class);
    }

    // ─── Pedir o link ────────────────────────────────────────────────────────

    /**
     * Nenhum token, nenhum e-mail, <b>nenhuma exceção</b>. Um
     * {@code orElseThrow} aqui transformaria a rota num oráculo por 404.
     */
    @Test
    @DisplayName("Pedir link para e-mail inexistente nao faz nada e nao estoura")
    void linkParaEmailInexistente() {
        when(candidatoRepository.findByEmail_AddressIgnoreCase("ninguem@x.com"))
                .thenReturn(Optional.empty());

        service.pedirLinkDeAcesso("Ninguem@X.com");

        verify(tokenService, never()).emitirPara(any());
        verify(emailService, never()).enviarLinkDeAcesso(anyString(), anyString(), anyString());
    }

    /**
     * Cooldown valendo: o token não é emitido, o e-mail não sai, e quem chamou
     * continua respondendo a mesma coisa. É o que fecha a amplificação de
     * e-mail sem biblioteca de rate limiting.
     */
    @Test
    @DisplayName("Cooldown valendo nao manda e-mail")
    void cooldownNaoMandaEmail() {
        Candidato existente = candidatoSalvo("maria@email.com");
        when(candidatoRepository.findByEmail_AddressIgnoreCase("maria@email.com"))
                .thenReturn(Optional.of(existente));
        when(tokenService.emitirPara(existente)).thenReturn(Optional.empty());

        service.pedirLinkDeAcesso("maria@email.com");

        verify(emailService, never()).enviarLinkDeAcesso(anyString(), anyString(), anyString());
    }

    // ─── O token ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Token desconhecido da 404")
    void tokenDesconhecido() {
        when(tokenService.resolver("nada")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verPorToken("nada"))
                .isInstanceOf(LinkDeAcessoInvalidoException.class);
    }

    @Test
    @DisplayName("Token expirado da 410 e nao devolve dado nenhum")
    void tokenExpirado() {
        TalentBankAccessToken acesso = new TalentBankAccessToken();
        acesso.setCandidato(candidatoSalvo("maria@email.com"));
        when(tokenService.resolver("velho")).thenReturn(Optional.of(acesso));
        when(tokenService.valido(acesso)).thenReturn(false);

        assertThatThrownBy(() -> service.verPorToken("velho"))
                .as("carregar primeiro e checar depois vaza por DTO parcial ou por log")
                .isInstanceOf(LinkDeAcessoExpiradoException.class);
    }

    @Test
    @DisplayName("Token de candidato ja anonimizado nao abre")
    void tokenDeAnonimizadoNaoAbre() {
        Candidato anonimo = candidatoSalvo("maria@email.com");
        anonimo.anonimizar(AGORA);

        TalentBankAccessToken acesso = new TalentBankAccessToken();
        acesso.setCandidato(anonimo);
        when(tokenService.resolver("tok")).thenReturn(Optional.of(acesso));
        when(tokenService.valido(acesso)).thenReturn(true);

        assertThatThrownBy(() -> service.verPorToken("tok"))
                .isInstanceOf(LinkDeAcessoExpiradoException.class);
    }

    // ─── Atualizar ───────────────────────────────────────────────────────────

    /**
     * Extensão diferente deixa o arquivo velho no disco para sempre — o nome é
     * {@code <id>.<ext>}, então {@code .pdf} e {@code .docx} são dois arquivos
     * — enquanto a pessoa acredita ter substituído.
     */
    @Test
    @DisplayName("Trocar o curriculo por outra extensao apaga o anterior")
    void trocaDeExtensaoApagaOAnterior() throws Exception {
        Candidato c = candidatoSalvo("maria@email.com");
        c.setPathCurriculo(c.getId() + ".docx");

        TalentBankAccessToken acesso = new TalentBankAccessToken();
        acesso.setCandidato(c);
        when(tokenService.resolver("tok")).thenReturn(Optional.of(acesso));
        when(tokenService.valido(acesso)).thenReturn(true);
        when(storageService.save(any(), anyString())).thenReturn(c.getId() + ".pdf");
        when(candidaturaRepository.findAllByCandidato(c)).thenReturn(List.of());

        service.atualizarPorToken("tok",
                new UpdateTalentBankEntryDTO("Maria", "44999990000", null, "Produção", false),
                pdf());

        verify(storageService).delete(c.getId() + ".docx");
        assertThat(c.getPathCurriculo()).isEqualTo(c.getId() + ".pdf");
    }

    @Test
    @DisplayName("Marcar consentimento no update renova o prazo")
    void updateRenovaOPrazo() throws Exception {
        Candidato c = candidatoSalvo("maria@email.com");
        c.registrarConsentimento(AGORA.minusMonths(20), RETENCAO);

        TalentBankAccessToken acesso = new TalentBankAccessToken();
        acesso.setCandidato(c);
        when(tokenService.resolver("tok")).thenReturn(Optional.of(acesso));
        when(tokenService.valido(acesso)).thenReturn(true);
        when(candidaturaRepository.findAllByCandidato(c)).thenReturn(List.of());

        service.atualizarPorToken("tok",
                new UpdateTalentBankEntryDTO("Maria", "44999990000", null, null, true), null);

        assertThat(c.getExpiraEm())
                .as("sem renovar, o registro expira logo depois de a pessoa confirmar que quer ficar")
                .isEqualTo(AGORA.plusMonths(RETENCAO));
    }

    // ─── Apagar ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Sem candidatura, apagar remove a linha e o arquivo")
    void semCandidaturaApaga() throws Exception {
        Candidato c = candidatoSalvo("maria@email.com");
        c.setPathCurriculo("abc.pdf");

        TalentBankAccessToken acesso = new TalentBankAccessToken();
        acesso.setCandidato(c);
        when(tokenService.resolver("tok")).thenReturn(Optional.of(acesso));
        when(tokenService.valido(acesso)).thenReturn(true);
        when(candidaturaRepository.existsByCandidato(c)).thenReturn(false);

        service.excluirPorToken("tok");

        verify(candidatoRepository).delete(c);
        verify(storageService).delete("abc.pdf");
        verify(tokenService).apagarTodosDe(c);
    }

    /**
     * Com candidatura, anonimiza. Um {@code DELETE} duro estouraria a FK — ou,
     * com cascade, levaria junto {@code candidaturas},
     * {@code historico_etapas} e {@code resposta_perguntas} de uma contratação
     * real.
     */
    @Test
    @DisplayName("Com candidatura, apagar anonimiza e preserva a linha")
    void comCandidaturaAnonimiza() throws Exception {
        Candidato c = candidatoSalvo("maria@email.com");
        c.setPathCurriculo("abc.pdf");

        TalentBankAccessToken acesso = new TalentBankAccessToken();
        acesso.setCandidato(c);
        when(tokenService.resolver("tok")).thenReturn(Optional.of(acesso));
        when(tokenService.valido(acesso)).thenReturn(true);
        when(candidaturaRepository.existsByCandidato(c)).thenReturn(true);

        service.excluirPorToken("tok");

        verify(candidatoRepository, never()).delete(any());
        assertThat(c.estaAnonimizado()).isTrue();
        assertThat(c.getEmail().getAddress()).endsWith("@removido.invalid");
        verify(storageService).delete("abc.pdf");
    }
}
