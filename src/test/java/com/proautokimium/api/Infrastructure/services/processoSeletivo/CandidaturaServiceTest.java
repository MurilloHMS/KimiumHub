package com.proautokimium.api.Infrastructure.services.processoSeletivo;

import com.proautokimium.api.Application.DTOs.processoSeletivo.candidaturas.CreateCandidaturaDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.candidaturas.ResponseCandidaturaDTO;
import com.proautokimium.api.Infrastructure.converters.processoSeletivo.CandidaturaConverter;
import com.proautokimium.api.Infrastructure.exceptions.processoSeletivo.CandidaturaAlreadyExistsException;
import com.proautokimium.api.Infrastructure.exceptions.processoSeletivo.VagaNotFoundException;
import com.proautokimium.api.Infrastructure.factories.EmailFactory;
import com.proautokimium.api.Infrastructure.repositories.processoSeletivo.CandidatoRepository;
import com.proautokimium.api.Infrastructure.repositories.processoSeletivo.CandidaturaRepository;
import com.proautokimium.api.Infrastructure.repositories.processoSeletivo.VagaRepository;
import com.proautokimium.api.Infrastructure.services.email.EmailQueueService;
import com.proautokimium.api.Infrastructure.services.storage.StorageService;
import com.proautokimium.api.domain.abstractions.Entity;
import com.proautokimium.api.domain.entities.email.EmailQueue;
import com.proautokimium.api.domain.entities.processoSeletivo.Candidato;
import com.proautokimium.api.domain.entities.processoSeletivo.Candidatura;
import com.proautokimium.api.domain.entities.processoSeletivo.Vaga;
import com.proautokimium.api.domain.enums.processoSeletivo.Etapa;
import com.proautokimium.api.domain.enums.processoSeletivo.StatusCandidatura;
import com.proautokimium.api.domain.valueObjects.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidaturaServiceTest {

    @Mock private CandidatoRepository candidatoRepository;
    @Mock private CandidaturaRepository candidaturaRepository;
    @Mock private VagaRepository vagaRepository;
    @Mock private StorageService storageService;
    @Mock private EmailQueueService emailService;
    @Mock private EmailFactory emailFactory;
    @Mock private CandidaturaConverter converter;

    /**
     * Construído à mão, e não por {@code @InjectMocks}, porque o serviço
     * depende de um {@code Clock} — e relógio de verdade não é dublê. Com
     * {@code Clock.fixed} a data virou asserção exata em vez de "não é nulo".
     */
    private CandidaturaService candidaturaService;

    /** 11/09/2026 11:30 em São Paulo. O fuso está no relógio, não na conta. */
    private static final Clock RELOGIO =
            Clock.fixed(Instant.parse("2026-09-11T14:30:00Z"), ZoneId.of("America/Sao_Paulo"));

    private UUID vagaId;
    private UUID candidaturaId;
    private Candidato candidato;
    private Vaga vaga;
    private Candidatura candidatura;
    private CreateCandidaturaDTO createDto;

    @BeforeEach
    void setUp() {
        candidaturaService = new CandidaturaService(candidatoRepository, candidaturaRepository,
                vagaRepository, storageService, emailService, emailFactory, converter, RELOGIO,
                "https://proautokimium.com.br", 24);

        vagaId = UUID.randomUUID();
        candidaturaId = UUID.randomUUID();

        candidato = new Candidato();
        candidato.setNome("João Silva");
        candidato.setEmail(new Email("joao@email.com"));

        vaga = new Vaga();
        vaga.setTitulo("Desenvolvedor Java");

        candidatura = new Candidatura();
        candidatura.setCandidato(candidato);
        candidatura.setVaga(vaga);
        candidatura.setEtapaAtual(Etapa.PROPOSTA);

        createDto = new CreateCandidaturaDTO(vagaId,
                "João Silva", "joao@email.com", "11999999999",
                "linkedin.com/in/joao"
        , false);
    }

    // ─── getCandidaturaByVagaId ───────────────────────────────────────────────

    @Test
    @DisplayName("Deve retornar candidaturas por vaga com sucesso")
    void deveRetornarCandidaturasPorVaga() {
        ResponseCandidaturaDTO responseDto = mock(ResponseCandidaturaDTO.class);
        when(vagaRepository.findById(vagaId)).thenReturn(Optional.of(vaga));
        when(candidaturaRepository.findCandidaturasByVagaId(vagaId)).thenReturn(List.of(candidatura));
        when(converter.toDto(candidatura)).thenReturn(responseDto);

        List<ResponseCandidaturaDTO> resultado = candidaturaService.getCandidaturaByVagaId(vagaId);

        assertThat(resultado).hasSize(1);
    }

    @Test
    @DisplayName("Deve lançar VagaNotFoundException ao buscar candidaturas de vaga inexistente")
    void deveLancarExcecaoAoBuscarCandidaturaDeVagaInexistente() {
        when(vagaRepository.findById(vagaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> candidaturaService.getCandidaturaByVagaId(vagaId))
                .isInstanceOf(VagaNotFoundException.class);
    }

    // ─── create ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Deve criar candidatura para candidato existente sem currículo")
    void deveCriarCandidaturaParaCandidatoExistente() throws IOException {
        when(candidatoRepository.findByEmail_AddressIgnoreCase(anyString())).thenReturn(Optional.of(candidato));
        when(vagaRepository.findById(vagaId)).thenReturn(Optional.of(vaga));
        when(candidaturaRepository.existsByCandidatoAndVaga(candidato, vaga)).thenReturn(false);
        when(candidaturaRepository.save(any(Candidatura.class))).thenReturn(candidatura);
        when(emailFactory.candidaturaConfirmada(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mock(EmailQueue.class));

        candidaturaService.create(createDto, null);

        ArgumentCaptor<Candidatura> captor = ArgumentCaptor.forClass(Candidatura.class);
        verify(candidaturaRepository).save(captor.capture());
        Candidatura saved = captor.getValue();

        assertThat(saved.getCandidato().getEmail()).isEqualTo(new Email(createDto.email()));
        assertThat(saved.getVaga()).isEqualTo(vaga);
        assertThat(saved.getEtapaAtual()).isEqualTo(Etapa.TRIAGEM);
        assertThat(saved.getStatus()).isEqualTo(StatusCandidatura.EM_ANDAMENTO);

        verify(emailService).create(any(EmailQueue.class));
        verify(emailFactory).candidaturaConfirmada(eq("joao@email.com"), eq("João Silva"), eq("Desenvolvedor Java"), anyString());
        verify(storageService, never()).save(any(), any());
    }

    @Test
    @DisplayName("Deve criar candidatura para novo candidato e salvar currículo")
    void deveCriarCandidaturaParaNovoCandidatoComCurriculo() throws IOException {
        MultipartFile curriculo = mock(MultipartFile.class);
        when(curriculo.isEmpty()).thenReturn(false);
        when(storageService.save(any(), any())).thenReturn("curriculo.pdf");

        when(candidatoRepository.findByEmail_AddressIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(candidatoRepository.save(any(Candidato.class)))
                .thenAnswer(invocation -> {
                    Candidato c = invocation.getArgument(0);

                    if (c.getId() == null) {
                        Field field = Entity.class.getDeclaredField("id");
                        field.setAccessible(true);
                        field.set(c, UUID.randomUUID());
                    }

                    return c;
                });
        when(vagaRepository.findById(vagaId)).thenReturn(Optional.of(vaga));
        when(candidaturaRepository.existsByCandidatoAndVaga(any(), any())).thenReturn(false);
        when(candidaturaRepository.save(any(Candidatura.class))).thenReturn(candidatura);
        when(emailFactory.candidaturaConfirmada(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mock(EmailQueue.class));

        candidaturaService.create(createDto, curriculo);

        ArgumentCaptor<Candidato> candidatoCaptor = ArgumentCaptor.forClass(Candidato.class);
        verify(candidatoRepository, times(2)).save(candidatoCaptor.capture());
        Candidato candidatoSalvo = candidatoCaptor.getValue();
        assertThat(candidatoSalvo.getPathCurriculo()).isEqualTo("curriculo.pdf");

        verify(storageService).save(eq(curriculo), eq(candidatoSalvo.getId().toString()));

        verify(candidaturaRepository).save(any(Candidatura.class));

        verify(emailFactory).candidaturaConfirmada(eq("joao@email.com"), eq("João Silva"), eq("Desenvolvedor Java"), anyString());
        verify(emailService).create(any(EmailQueue.class));
    }

    @Test
    @DisplayName("Deve lançar CandidaturaAlreadyExistsException se candidato já se candidatou")
    void deveLancarExcecaoSeCandidatoJaSeCandidatou() {
        when(candidatoRepository.findByEmail_AddressIgnoreCase(anyString())).thenReturn(Optional.of(candidato));
        when(vagaRepository.findById(vagaId)).thenReturn(Optional.of(vaga));
        when(candidaturaRepository.existsByCandidatoAndVaga(candidato, vaga)).thenReturn(true);

        assertThatThrownBy(() -> candidaturaService.create(createDto, null))
                .isInstanceOf(CandidaturaAlreadyExistsException.class);

        verify(candidaturaRepository, never()).save(any(Candidatura.class));
    }

    @Test
    @DisplayName("Deve lançar VagaNotFoundException ao criar candidatura para vaga inexistente")
    void deveLancarExcecaoAoCriarCandidaturaParaVagaInexistente() {
        // Sem stub do candidato de proposito: desde 2026-09-11 a vaga e resolvida
        // PRIMEIRO, entao a busca do candidato nem acontece neste caminho.
        when(vagaRepository.findById(vagaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> candidaturaService.create(createDto, null))
                .isInstanceOf(VagaNotFoundException.class);
    }

    // ─── avancarEtapa ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Deve avançar etapa para CONTRATADO e enviar e-mail de aprovação")
    void deveAvancarEtapaParaContratado() {
        when(candidaturaRepository.findById(candidaturaId))
                .thenReturn(Optional.of(candidatura));

        when(candidaturaRepository.save(any(Candidatura.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(emailFactory.candidaturaAprovada(
                anyString(),
                anyString(),
                anyString()))
                .thenReturn(mock(EmailQueue.class));

        candidaturaService.avancarEtapa(candidaturaId);

        verify(emailFactory).candidaturaAprovada(
                anyString(),
                anyString(),
                anyString());

        verify(emailService).create(any(EmailQueue.class));
    }

    @Test
    @DisplayName("Deve lançar RuntimeException ao avançar etapa de candidatura inexistente")
    void deveLancarExcecaoAoAvancarEtapaInexistente() {
        when(candidaturaRepository.findById(candidaturaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> candidaturaService.avancarEtapa(candidaturaId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Candidatura não encontrada");
    }

    // ─── aprovarCandidatura ──────────────────────────────────────────────────

    @Test
    @DisplayName("Deve aprovar candidatura e enviar e-mail de aprovação")
    void deveAprovarCandidatura() {
        when(candidaturaRepository.findById(candidaturaId)).thenReturn(Optional.of(candidatura));
        when(candidaturaRepository.save(candidatura)).thenReturn(candidatura);
        when(emailFactory.candidaturaAprovada(anyString(), anyString(), anyString()))
                .thenReturn(mock(EmailQueue.class));

        candidaturaService.aprovarCandidatura(candidaturaId);

        verify(emailService).create(any(EmailQueue.class));
    }

    @Test
    @DisplayName("Deve lançar RuntimeException ao aprovar candidatura inexistente")
    void deveLancarExcecaoAoAprovarCandidaturaInexistente() {
        when(candidaturaRepository.findById(candidaturaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> candidaturaService.aprovarCandidatura(candidaturaId))
                .isInstanceOf(RuntimeException.class);
    }

    // ─── reprovarCandidatura ─────────────────────────────────────────────────

    @Test
    @DisplayName("Deve reprovar candidatura e enviar e-mail de reprovação")
    void deveReprovarCandidatura() {
        when(candidaturaRepository.findById(candidaturaId)).thenReturn(Optional.of(candidatura));
        when(candidaturaRepository.save(candidatura)).thenReturn(candidatura);
        when(emailFactory.candidaturaReprovada(anyString(), anyString(), anyString()))
                .thenReturn(mock(EmailQueue.class));

        candidaturaService.reprovarCandidatura(candidaturaId);

        verify(emailService).create(any(EmailQueue.class));
    }

    // ─── encerrarCandidatura ─────────────────────────────────────────────────

    @Test
    @DisplayName("Deve encerrar candidatura e enviar e-mail")
    void deveEncerrarCandidatura() {
        when(candidaturaRepository.findById(candidaturaId)).thenReturn(Optional.of(candidatura));
        when(candidaturaRepository.save(candidatura)).thenReturn(candidatura);
        when(emailFactory.candidaturaReprovada(anyString(), anyString(), anyString()))
                .thenReturn(mock(EmailQueue.class));

        candidaturaService.encerrarCandidatura(candidaturaId);

        verify(emailService).create(any(EmailQueue.class));
    }

    // ─── enviarEmailEtapa (comportamento indireto via avancarEtapa) ──────────

    @Test
    @DisplayName("Deve enviar e-mail de aprovação quando etapa avança para CONTRATADO")
    void deveEnviarEmailAprovacaoQuandoEtapaContratado() {
        when(candidaturaRepository.findById(candidaturaId)).thenReturn(Optional.of(candidatura));
        when(candidaturaRepository.save(candidatura)).thenReturn(candidatura);
        when(emailFactory.candidaturaAprovada(anyString(), anyString(), anyString()))
                .thenReturn(mock(EmailQueue.class));

        candidaturaService.avancarEtapa(candidaturaId);

        verify(emailFactory).candidaturaAprovada(anyString(), anyString(), anyString());
        verify(emailFactory, never()).avancoEtapa(anyString(), anyString(), anyString());
    }

    // ─── Os tres defeitos do caminho publico ────────────────────────────────
    //
    // Escritos em 2026-09-11, ANTES da correcao, e por isso nascem vermelhos.
    // Cada um trava um defeito que o banco de talentos vai depender de nao ter.

    /**
     * <b>Reenviar tem que atualizar os dados.</b>
     *
     * <p>Hoje o ramo do candidato existente descarta {@code nome},
     * {@code telefone} e {@code urlLinkedin} do formulario: o cadastro de dois
     * anos atras vence o que a pessoa acabou de digitar. A aba do banco de
     * talentos mostraria o telefone velho como se fosse o atual.
     *
     * <p>A asserção é sobre a <b>entidade</b>, e não sobre uma chamada a
     * {@code save}: sob {@code @Transactional} mutar a instância gerenciada
     * basta, e o teste não deve ditar qual das duas formas o serviço usa.
     */
    @Test
    @DisplayName("Candidato que ja existe tem nome, telefone e LinkedIn atualizados pelo reenvio")
    void reenvioAtualizaOsDadosDoCandidato() throws IOException {
        candidato.setNome("Joao Silva");
        candidato.setTelefone("11888888888");
        candidato.setUrlLinkedin("linkedin.com/in/joao-antigo");

        CreateCandidaturaDTO comDadosNovos = new CreateCandidaturaDTO(vagaId,
                "Joao Pedro da Silva", "joao@email.com", "11999997777",
                "linkedin.com/in/joao-pedro", false);

        when(candidatoRepository.findByEmail_AddressIgnoreCase(anyString())).thenReturn(Optional.of(candidato));
        when(vagaRepository.findById(vagaId)).thenReturn(Optional.of(vaga));
        when(candidaturaRepository.existsByCandidatoAndVaga(any(), any())).thenReturn(false);
        when(candidaturaRepository.save(any(Candidatura.class))).thenReturn(candidatura);
        when(emailFactory.candidaturaConfirmada(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mock(EmailQueue.class));

        candidaturaService.create(comDadosNovos, null);

        assertThat(candidato.getNome()).isEqualTo("Joao Pedro da Silva");
        assertThat(candidato.getTelefone())
                .as("telefone velho na ficha e o que faz o RH ligar para o numero errado")
                .isEqualTo("11999997777");
        assertThat(candidato.getUrlLinkedin()).isEqualTo("linkedin.com/in/joao-pedro");
    }

    /**
     * <b>Candidato criado pelo site nasce sem data.</b>
     *
     * <p>A coluna {@code criado_em} tem {@code DEFAULT CURRENT_TIMESTAMP} na
     * V35, e o default <b>nunca dispara</b>: o Hibernate emite a coluna no
     * INSERT com {@code null}. Nenhum {@code ALTER ... SET DEFAULT} conserta
     * isso — o conserto é em Java.
     *
     * <p>A asserção é só "não é nulo" de propósito. Comparar com
     * {@code LocalDateTime.now()} é flaky, e a asserção exata entra quando o
     * {@code Clock} injetado chegar a este serviço.
     */
    @Test
    @DisplayName("Candidato novo nasce com criadoEm preenchido")
    void candidatoNovoNasceComCriadoEm() throws IOException {
        when(candidatoRepository.findByEmail_AddressIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(candidatoRepository.save(any(Candidato.class))).thenAnswer(invocation -> {
            Candidato c = invocation.getArgument(0);
            if (c.getId() == null) {
                Field field = Entity.class.getDeclaredField("id");
                field.setAccessible(true);
                field.set(c, UUID.randomUUID());
            }
            return c;
        });
        when(vagaRepository.findById(vagaId)).thenReturn(Optional.of(vaga));
        when(candidaturaRepository.existsByCandidatoAndVaga(any(), any())).thenReturn(false);
        when(candidaturaRepository.save(any(Candidatura.class))).thenReturn(candidatura);
        when(emailFactory.candidaturaConfirmada(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mock(EmailQueue.class));

        candidaturaService.create(createDto, null);

        ArgumentCaptor<Candidato> captor = ArgumentCaptor.forClass(Candidato.class);
        verify(candidatoRepository, atLeastOnce()).save(captor.capture());

        assertThat(captor.getValue().getCriadoEm())
                .as("a data sai do Clock injetado: LocalDateTime.now() cru daria tres horas de diferenca")
                .isEqualTo(LocalDateTime.of(2026, 9, 11, 11, 30));
    }

    /**
     * <b>Candidatura duplicada nao pode tocar no disco.</b>
     *
     * <p>Hoje o curriculo e gravado <i>antes</i> da checagem de duplicata. O
     * nome do arquivo e fixo ({@code <candidatoId>.<ext>}) e a gravacao usa
     * {@code REPLACE_EXISTING}, entao um duplo clique <b>sobrescreve o
     * curriculo bom</b> e so depois lanca 409.
     *
     * <p>E {@code @Transactional} nao salva disso: escrita em filesystem nao
     * faz rollback. Quem corrige e a ordem — resolver vaga, checar duplicata,
     * validar, e gravar por ultimo.
     *
     * <p>Os dois stubs sao {@code lenient} porque o caminho correto lanca antes
     * de chegar neles; sem isso o teste passaria a quebrar por
     * {@code UnnecessaryStubbingException} exatamente quando o defeito fosse
     * corrigido.
     */
    @Test
    @DisplayName("Candidatura duplicada nao grava o curriculo em disco")
    void duplicataNaoGravaOCurriculo() throws Exception {
        MultipartFile curriculo = mock(MultipartFile.class);
        lenient().when(curriculo.isEmpty()).thenReturn(false);
        lenient().when(storageService.save(any(), any())).thenReturn("novo.pdf");

        candidato.setPathCurriculo("o-bom.pdf");
        // O id precisa existir: sem ele o caminho atual estoura em NullPointer
        // ao montar o nome do arquivo, e o vermelho seria da falta do id — nao
        // da ordem entre gravar e checar duplicata, que e o que este teste trava.
        Field id = Entity.class.getDeclaredField("id");
        id.setAccessible(true);
        id.set(candidato, UUID.randomUUID());

        when(candidatoRepository.findByEmail_AddressIgnoreCase(anyString())).thenReturn(Optional.of(candidato));
        when(vagaRepository.findById(vagaId)).thenReturn(Optional.of(vaga));
        when(candidaturaRepository.existsByCandidatoAndVaga(candidato, vaga)).thenReturn(true);

        assertThatThrownBy(() -> candidaturaService.create(createDto, curriculo))
                .isInstanceOf(CandidaturaAlreadyExistsException.class);

        verify(storageService, never()).save(any(), any());
        assertThat(candidato.getPathCurriculo())
                .as("o curriculo que ja estava la nao pode ser trocado por um envio recusado")
                .isEqualTo("o-bom.pdf");
    }
}
