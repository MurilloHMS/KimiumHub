package com.proautokimium.api.Infrastructure.services.processoSeletivo;

import com.proautokimium.api.Application.DTOs.processoSeletivo.candidato.CreateCandidatoDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.candidato.ResponseCandidatoDTO;
import com.proautokimium.api.Infrastructure.converters.processoSeletivo.CandidatoConverter;
import com.proautokimium.api.Infrastructure.exceptions.processoSeletivo.CandidatoAlreadyExistsException;
import com.proautokimium.api.Infrastructure.repositories.processoSeletivo.CandidatoRepository;
import com.proautokimium.api.domain.abstractions.Entity;
import com.proautokimium.api.domain.entities.processoSeletivo.Candidato;
import com.proautokimium.api.domain.valueObjects.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidatoServiceTest {

    @Mock
    private CandidatoRepository candidatoRepository;

    @Mock
    private CandidatoConverter converter;

    @InjectMocks
    private CandidatoService candidatoService;

    private CreateCandidatoDTO createDto;
    private Candidato candidato;

    @BeforeEach
    void setUp() {
        createDto = new CreateCandidatoDTO("João Silva", "joao@email.com", "11999999999", "linkedin.com/in/joao", "");

        candidato = new Candidato();
        candidato.setNome("João Silva");
        candidato.setEmail(new Email("joao@email.com"));
    }

    @Test
    @DisplayName("Deve criar candidato com sucesso quando o email não existe")
    void deveCriarCandidatoComSucesso(){
        when(candidatoRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());
        when(converter.fromCreateDto(createDto)).thenReturn(candidato);
        when(candidatoRepository.save(candidato)).thenReturn(candidato);

        Candidato result = candidatoService.create(createDto);

        assertThat(result).isNotNull();
        assertThat(result.getNome()).isEqualTo("João Silva");
        verify(candidatoRepository).save(candidato);
    }

    @Test
    @DisplayName("Deve lançar CandidatoAlreadyExistsException quando email já cadastrado")
    void deveLancarExcecaoQuandoEmailExiste(){
        when(candidatoRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(candidato));

        assertThatThrownBy(() ->
                candidatoService.create(createDto)).isInstanceOf(CandidatoAlreadyExistsException.class);

        verify(candidatoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve listar todos os candidatos")
    void deveListarCandidatos() {
        Candidato candidato2 = new Candidato();
        candidato2.setNome("Maria Souza");

        ResponseCandidatoDTO dto1 = new ResponseCandidatoDTO(
                "João Silva",
                "joao@email.com",
                "11999999999",
                "linkedin.com/in/joao",
                null,
                null
        );

        ResponseCandidatoDTO dto2 = new ResponseCandidatoDTO(
                "Maria Souza",
                "maria@email.com",
                "11888888888",
                null,
                null,
                null
        );

        when(candidatoRepository.findAll())
                .thenReturn(List.of(candidato, candidato2));

        when(converter.toDto(any(Candidato.class)))
                .thenAnswer(invocation -> {
                    Candidato c = invocation.getArgument(0);

                    if ("João Silva".equals(c.getNome())) {
                        return dto1;
                    }

                    return dto2;
                });

        List<ResponseCandidatoDTO> resultado = candidatoService.listarCandidatos();

        assertThat(resultado).hasSize(2);

        assertThat(resultado)
                .extracting(ResponseCandidatoDTO::nome)
                .containsExactlyInAnyOrder(
                        "João Silva",
                        "Maria Souza"
                );
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não há candidatos")
    void deveRetornarListaVaziaQuandoNaoHaCandidatos(){
        when(candidatoRepository.findAll()).thenReturn(List.of());

        List<ResponseCandidatoDTO> result = candidatoService.listarCandidatos();
        assertThat(result).isEmpty();
    }
}