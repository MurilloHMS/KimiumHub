package com.proautokimium.api.Infrastructure.services.processoSeletivo;

import com.proautokimium.api.Application.DTOs.processoSeletivo.vaga.CreateVagaDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.vaga.ResponseVagaDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.vaga.UpdateVagaDTO;
import com.proautokimium.api.Infrastructure.converters.processoSeletivo.VagaConverter;
import com.proautokimium.api.Infrastructure.exceptions.processoSeletivo.VagaNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.processoSeletivo.VagaRepository;
import com.proautokimium.api.domain.entities.processoSeletivo.Vaga;
import com.proautokimium.api.domain.enums.processoSeletivo.StatusVaga;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VagaServiceTest {

    @Mock
    private VagaRepository vagaRepository;

    @Mock
    private VagaConverter converter;

    @InjectMocks
    private VagaService vagaService;

    private UUID vagaId;
    private Vaga vaga;

    @BeforeEach
    void setUp() {
        vagaId = UUID.randomUUID();
        vaga = new Vaga();
        vaga.setTitulo("Desenvolvedor Java");
    }

    @Test
    @DisplayName("Deve criar vaga com sucesso")
    void deveCriarVagaComSucesso() {
        CreateVagaDTO dto = new CreateVagaDTO("Desenvolvedor Java", "Descrição", "Requisitos", "Beneficios", "area", LocalDateTime.now(), LocalDateTime.now().plusDays(2));
        when(converter.fromCreateDto(dto)).thenReturn(vaga);
        when(vagaRepository.save(vaga)).thenReturn(vaga);

        Vaga resultado = vagaService.create(dto);

        assertThat(resultado).isNotNull();
        verify(vagaRepository).save(vaga);
    }

    @Test
    @DisplayName("Deve atualizar vaga com sucesso")
    void deveAtualizarVagaComSucesso() {
        UpdateVagaDTO dto = new UpdateVagaDTO(vagaId, "Novo Titulo", "Nova Desc", "Novos Req", "Beneficios", "area", LocalDateTime.now(), LocalDateTime.now().plusDays(2));
        when(vagaRepository.findById(vagaId)).thenReturn(Optional.of(vaga));

        vagaService.update(dto);

        verify(converter).updateFromDto(dto, vaga);
        verify(vagaRepository).save(vaga);
    }

    @Test
    @DisplayName("Deve lançar VagaNotFoundException ao atualizar vaga inexistente")
    void deveLancarExcecaoAoAtualizarVagaInexistente() {
        UpdateVagaDTO dto = new UpdateVagaDTO(vagaId, "Titulo", "Desc", "Req", "Beneficios", "area", LocalDateTime.now(), LocalDateTime.now().plusDays(2));
        when(vagaRepository.findById(vagaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vagaService.update(dto))
                .isInstanceOf(VagaNotFoundException.class);

        verify(vagaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve publicar vaga com sucesso")
    void devePublicarVagaComSucesso() {
        vaga.setStatus(StatusVaga.RASCUNHO);
        when(vagaRepository.findById(vagaId)).thenReturn(Optional.of(vaga));

        vagaService.publicar(vagaId);

        verify(vagaRepository).save(vaga);
    }

    @Test
    @DisplayName("Deve arquivar vaga com sucesso")
    void deveArquivarVagaComSucesso() {
        vaga.setStatus(StatusVaga.ENCERRADA);
        when(vagaRepository.findById(vagaId)).thenReturn(Optional.of(vaga));

        vagaService.arquivar(vagaId);

        verify(vagaRepository).save(vaga);
    }

    @Test
    @DisplayName("Deve encerrar vaga com sucesso")
    void deveEncerrarVagaComSucesso() {
        vaga.setStatus(StatusVaga.PUBLICADA);
        when(vagaRepository.findById(vagaId)).thenReturn(Optional.of(vaga));

        vagaService.encerrar(vagaId);

        verify(vagaRepository).save(vaga);
    }

    @Test
    @DisplayName("Deve lançar VagaNotFoundException ao encerrar vaga inexistente")
    void deveLancarExcecaoAoEncerrarVagaInexistente() {
        when(vagaRepository.findById(vagaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vagaService.encerrar(vagaId))
                .isInstanceOf(VagaNotFoundException.class);
    }

    @Test
    @DisplayName("Deve listar vaga por ID com sucesso")
    void deveListarVagaPorId() {
        ResponseVagaDTO responseDto = new ResponseVagaDTO(vagaId, "Desenvolvedor Java", "Desc", "Req", "Beneficios", "area", LocalDateTime.now(), LocalDateTime.now().plusDays(2));
        when(vagaRepository.findById(vagaId)).thenReturn(Optional.of(vaga));
        when(converter.toDto(vaga)).thenReturn(responseDto);

        ResponseVagaDTO resultado = vagaService.listarVaga(vagaId);

        assertThat(resultado).isNotNull();
        assertThat(resultado.titulo()).isEqualTo("Desenvolvedor Java");
    }

    @Test
    @DisplayName("Deve listar vagas publicadas")
    void deveListarVagasPublicadas() {
        when(vagaRepository.findByStatus(StatusVaga.PUBLICADA)).thenReturn(List.of(vaga));
        when(converter.toDto(vaga)).thenReturn(mock(ResponseVagaDTO.class));

        List<ResponseVagaDTO> resultado = vagaService.listarVagasPublicadas();

        assertThat(resultado).hasSize(1);
    }

    @Test
    @DisplayName("Deve listar vagas arquivadas")
    void deveListarVagasArquivadas() {
        when(vagaRepository.findByStatus(StatusVaga.ARQUIVADA)).thenReturn(List.of(vaga));
        when(converter.toDto(vaga)).thenReturn(mock(ResponseVagaDTO.class));

        List<ResponseVagaDTO> resultado = vagaService.listarVagasArquivadas();

        assertThat(resultado).hasSize(1);
    }

    @Test
    @DisplayName("Deve listar vagas em rascunho")
    void deveListarVagasEmRascunho() {
        when(vagaRepository.findByStatus(StatusVaga.RASCUNHO)).thenReturn(List.of(vaga));
        when(converter.toDto(vaga)).thenReturn(mock(ResponseVagaDTO.class));

        List<ResponseVagaDTO> resultado = vagaService.listarVagasEmRascunho();

        assertThat(resultado).hasSize(1);
    }
}