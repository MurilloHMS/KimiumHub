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
import com.proautokimium.api.domain.valueObjects.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.reflect.Field;
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

    @InjectMocks
    private CandidaturaService candidaturaService;

    private UUID vagaId;
    private UUID candidaturaId;
    private Candidato candidato;
    private Vaga vaga;
    private Candidatura candidatura;
    private CreateCandidaturaDTO createDto;

    @BeforeEach
    void setUp() {
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
        );
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
        when(candidatoRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(candidato));
        when(vagaRepository.findById(vagaId)).thenReturn(Optional.of(vaga));
        when(candidaturaRepository.existsByCandidatoAndVaga(candidato, vaga)).thenReturn(false);
        when(candidaturaRepository.save(any(Candidatura.class))).thenReturn(candidatura);
        when(emailFactory.candidaturaConfirmada(anyString(), anyString(), anyString()))
                .thenReturn(mock(EmailQueue.class));

        candidaturaService.create(createDto, null);

        verify(candidaturaRepository).save(any(Candidatura.class));
        verify(emailService).create(any(EmailQueue.class));
    }

    @Test
    @DisplayName("Deve criar candidatura para novo candidato e salvar currículo")
    void deveCriarCandidaturaParaNovoCandidatoComCurriculo() throws IOException {
        MultipartFile curriculo = mock(MultipartFile.class);
        when(curriculo.isEmpty()).thenReturn(false);
        when(storageService.save(any(), any())).thenReturn("curriculo.pdf");

        when(candidatoRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());
        when(candidatoRepository.save(any(Candidato.class)))
                .thenAnswer(invocation -> {
                    Candidato c = invocation.getArgument(0);

                    Field field = Entity.class.getDeclaredField("id");
                    field.setAccessible(true);
                    field.set(c, UUID.randomUUID());

                    return c;
                });
        when(vagaRepository.findById(vagaId)).thenReturn(Optional.of(vaga));
        when(candidaturaRepository.existsByCandidatoAndVaga(any(), any())).thenReturn(false);
        when(candidaturaRepository.save(any(Candidatura.class))).thenReturn(candidatura);
        when(emailFactory.candidaturaConfirmada(anyString(), anyString(), anyString()))
                .thenReturn(mock(EmailQueue.class));

        candidaturaService.create(createDto, curriculo);

        verify(storageService).save(any(), any());
        verify(candidaturaRepository).save(any(Candidatura.class));
    }

    @Test
    @DisplayName("Deve lançar CandidaturaAlreadyExistsException se candidato já se candidatou")
    void deveLancarExcecaoSeCandidatoJaSeCandidatou() {
        when(candidatoRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(candidato));
        when(vagaRepository.findById(vagaId)).thenReturn(Optional.of(vaga));
        when(candidaturaRepository.existsByCandidatoAndVaga(candidato, vaga)).thenReturn(true);

        assertThatThrownBy(() -> candidaturaService.create(createDto, null))
                .isInstanceOf(CandidaturaAlreadyExistsException.class);

        verify(candidaturaRepository, never()).save(any(Candidatura.class));
    }

    @Test
    @DisplayName("Deve lançar VagaNotFoundException ao criar candidatura para vaga inexistente")
    void deveLancarExcecaoAoCriarCandidaturaParaVagaInexistente() {
        when(candidatoRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(candidato));
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
}