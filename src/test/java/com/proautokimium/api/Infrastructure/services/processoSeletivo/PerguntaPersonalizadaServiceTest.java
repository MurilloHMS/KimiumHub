package com.proautokimium.api.Infrastructure.services.processoSeletivo;

import com.proautokimium.api.Application.DTOs.processoSeletivo.perguntas.CreatePerguntaDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.perguntas.ResponsePerguntaPersonalizadaDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.perguntas.UpdatePerguntaDTO;
import com.proautokimium.api.Infrastructure.converters.processoSeletivo.PerguntaPersonalizadaConverter;
import com.proautokimium.api.Infrastructure.exceptions.processoSeletivo.PerguntaNotFoundExeption;
import com.proautokimium.api.Infrastructure.exceptions.processoSeletivo.VagaNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.processoSeletivo.PerguntaPersonalizadaRepository;
import com.proautokimium.api.Infrastructure.repositories.processoSeletivo.VagaRepository;
import com.proautokimium.api.domain.entities.processoSeletivo.PerguntaPersonalizada;
import com.proautokimium.api.domain.entities.processoSeletivo.Vaga;
import com.proautokimium.api.domain.enums.processoSeletivo.TipoPergunta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PerguntaPersonalizadaServiceTest {

    @Mock private PerguntaPersonalizadaRepository perguntaPersonalizadaRepository;
    @Mock private VagaRepository vagaRepository;
    @Mock private PerguntaPersonalizadaConverter converter;

    @InjectMocks
    private PerguntaPersonalizadaService perguntaService;

    private UUID vagaId;
    private UUID perguntaId;
    private Vaga vaga;
    private PerguntaPersonalizada pergunta;

    @BeforeEach
    void setUp() {
        vagaId = UUID.randomUUID();
        perguntaId = UUID.randomUUID();

        vaga = new Vaga();
        vaga.setTitulo("Desenvolvedor Java");

        pergunta = new PerguntaPersonalizada();
        pergunta.setEnunciado("Qual sua experiência com Spring Boot?");
        pergunta.setVaga(vaga);
    }

    // ─── create ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Deve criar pergunta personalizada com sucesso")
    void deveCriarPerguntaComSucesso() {
        CreatePerguntaDTO dto = new CreatePerguntaDTO("Qual sua experiência com Spring Boot?", TipoPergunta.TEXTO_LIVRE, false, (short) 1);
        when(vagaRepository.findById(vagaId)).thenReturn(Optional.of(vaga));
        when(converter.fromCreateDto(dto)).thenReturn(pergunta);

        perguntaService.create(dto, vagaId);

        verify(perguntaPersonalizadaRepository).save(pergunta);
        assertThat(pergunta.getVaga()).isEqualTo(vaga);
    }

    @Test
    @DisplayName("Deve lançar VagaNotFoundException ao criar pergunta para vaga inexistente")
    void deveLancarExcecaoAoCriarPerguntaParaVagaInexistente() {
        CreatePerguntaDTO dto = new CreatePerguntaDTO("Pergunta qualquer", TipoPergunta.TEXTO_LIVRE, false, (short) 1);
        when(vagaRepository.findById(vagaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> perguntaService.create(dto, vagaId))
                .isInstanceOf(VagaNotFoundException.class);

        verify(perguntaPersonalizadaRepository, never()).save(any());
    }

    // ─── update ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Deve atualizar pergunta com sucesso")
    void deveAtualizarPerguntaComSucesso() {
        UpdatePerguntaDTO dto = new UpdatePerguntaDTO(perguntaId, "Pergunta atualizada", TipoPergunta.TEXTO_LIVRE, false, (short) 1);
        when(perguntaPersonalizadaRepository.findById(perguntaId)).thenReturn(Optional.of(pergunta));

        perguntaService.update(dto);

        verify(converter).updateFromDto(dto, pergunta);
        verify(perguntaPersonalizadaRepository).save(pergunta);
    }

    @Test
    @DisplayName("Deve lançar PerguntaNotFoundExeption ao atualizar pergunta inexistente")
    void deveLancarExcecaoAoAtualizarPerguntaInexistente() {
        UpdatePerguntaDTO dto = new UpdatePerguntaDTO(perguntaId, "Pergunta atualizada", TipoPergunta.TEXTO_LIVRE, false, (short) 1);
        when(perguntaPersonalizadaRepository.findById(perguntaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> perguntaService.update(dto))
                .isInstanceOf(PerguntaNotFoundExeption.class);

        verify(perguntaPersonalizadaRepository, never()).save(any());
    }

    // ─── delete ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Deve deletar pergunta com sucesso")
    void deveDeletarPerguntaComSucesso() {
        perguntaService.delete(perguntaId);

        verify(perguntaPersonalizadaRepository).deleteById(perguntaId);
    }

    // ─── listarPerguntasPorVaga ──────────────────────────────────────────────

    @Test
    @DisplayName("Deve listar perguntas por vaga com sucesso")
    void deveListarPerguntasPorVaga() {
        ResponsePerguntaPersonalizadaDTO responseDto = mock(ResponsePerguntaPersonalizadaDTO.class);
        when(vagaRepository.findById(vagaId)).thenReturn(Optional.of(vaga));
        when(perguntaPersonalizadaRepository.findAllByVaga(vaga)).thenReturn(List.of(pergunta));
        when(converter.toDto(pergunta)).thenReturn(responseDto);

        List<ResponsePerguntaPersonalizadaDTO> resultado = perguntaService.listarPerguntasPorVaga(vagaId);

        assertThat(resultado).hasSize(1);
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando vaga não tem perguntas")
    void deveRetornarListaVaziaQuandoSemPerguntas() {
        when(vagaRepository.findById(vagaId)).thenReturn(Optional.of(vaga));
        when(perguntaPersonalizadaRepository.findAllByVaga(vaga)).thenReturn(List.of());

        List<ResponsePerguntaPersonalizadaDTO> resultado = perguntaService.listarPerguntasPorVaga(vagaId);

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Deve lançar VagaNotFoundException ao listar perguntas de vaga inexistente")
    void deveLancarExcecaoAoListarPerguntasDeVagaInexistente() {
        when(vagaRepository.findById(vagaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> perguntaService.listarPerguntasPorVaga(vagaId))
                .isInstanceOf(VagaNotFoundException.class);
    }
}