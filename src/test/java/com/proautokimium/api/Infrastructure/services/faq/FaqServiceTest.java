package com.proautokimium.api.Infrastructure.services.faq;

import com.proautokimium.api.Application.DTOs.faq.FaqCreateDTO;
import com.proautokimium.api.Application.DTOs.faq.FaqPublicResponseDTO;
import com.proautokimium.api.Application.DTOs.faq.FaqResponseDTO;
import com.proautokimium.api.Application.DTOs.faq.FaqUpdateDTO;
import com.proautokimium.api.Infrastructure.converters.FaqConverter;
import com.proautokimium.api.Infrastructure.repositories.FaqRepository;
import com.proautokimium.api.domain.entities.Faq;
import com.proautokimium.api.domain.enums.StatusPostagem;
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
class FaqServiceTest {

    @Mock
    private FaqRepository repository;

    @Mock
    private FaqConverter converter;

    @InjectMocks
    private FaqService faqService;

    private UUID faqId;
    private Faq faq;

    @BeforeEach
    void setUp() {
        faqId = UUID.randomUUID();
        faq = new Faq();
        faq.setTitle("Pergunta de teste");
        faq.setBody("Resposta de teste");
    }

    @Test
    @DisplayName("Deve retornar lista de todos os FAQs")
    void deveRetornarTodosFaqs() {
        FaqResponseDTO responseDTO = new FaqResponseDTO(faqId, "Pergunta", "Resposta", StatusPostagem.RASCUNHO);
        when(repository.findAll()).thenReturn(List.of(faq));
        when(converter.toDto(faq)).thenReturn(responseDTO);

        List<FaqResponseDTO> result = faqService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Pergunta");
        verify(repository).findAll();
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não há FAQs")
    void deveRetornarListaVaziaQuandoNaoHaFaqs() {
        when(repository.findAll()).thenReturn(List.of());

        List<FaqResponseDTO> result = faqService.getAll();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar apenas FAQs com status PUBLICADO")
    void deveRetornarFaqsPublicados() {
        FaqPublicResponseDTO publicDTO = new FaqPublicResponseDTO("Pergunta", "Resposta", StatusPostagem.PUBLICADO);
        when(repository.findAllByStatus(StatusPostagem.PUBLICADO)).thenReturn(List.of(faq));
        when(converter.toPublicDto(faq)).thenReturn(publicDTO);

        List<FaqPublicResponseDTO> result = faqService.getAllPublic();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(StatusPostagem.PUBLICADO);
        verify(repository).findAllByStatus(StatusPostagem.PUBLICADO);
    }

    @Test
    @DisplayName("Deve criar FAQ com sucesso")
    void deveCriarFaqComSucesso() {
        FaqCreateDTO createDTO = new FaqCreateDTO("Pergunta", "Resposta");
        when(converter.fromCreateDto(createDTO)).thenReturn(faq);

        faqService.create(createDTO);

        verify(converter).fromCreateDto(createDTO);
        verify(repository).save(faq);
    }

    @Test
    @DisplayName("Deve atualizar FAQ quando encontrado")
    void deveAtualizarFaqQuandoEncontrado() {
        FaqUpdateDTO updateDTO = new FaqUpdateDTO("Novo Titulo", "Novo Body");
        when(repository.findById(faqId)).thenReturn(Optional.of(faq));

        faqService.update(faqId, updateDTO);

        verify(converter).updateFromDto(updateDTO, faq);
    }

    @Test
    @DisplayName("Não deve chamar converter ao atualizar FAQ inexistente")
    void naoDeveAtualizarFaqQuandoNaoEncontrado() {
        FaqUpdateDTO updateDTO = new FaqUpdateDTO("Titulo", "Body");
        when(repository.findById(faqId)).thenReturn(Optional.empty());

        faqService.update(faqId, updateDTO);

        verify(converter, never()).updateFromDto(any(), any());
    }

    @Test
    @DisplayName("Deve publicar FAQ quando encontrado")
    void devePublicarFaqQuandoEncontrado() {
        when(repository.findById(faqId)).thenReturn(Optional.of(faq));

        faqService.setPublished(faqId);

        assertThat(faq.getStatus()).isEqualTo(StatusPostagem.PUBLICADO);
    }

    @Test
    @DisplayName("Deve arquivar FAQ quando encontrado")
    void deveArquivarFaqQuandoEncontrado() {
        when(repository.findById(faqId)).thenReturn(Optional.of(faq));

        faqService.setArchived(faqId);

        assertThat(faq.getStatus()).isEqualTo(StatusPostagem.ARQUIVADO);
    }

    @Test
    @DisplayName("Deve marcar FAQ como rascunho quando encontrado")
    void deveMarcarFaqComoRascunhoQuandoEncontrado() {
        faq.publicar();
        when(repository.findById(faqId)).thenReturn(Optional.of(faq));

        faqService.setDraft(faqId);

        assertThat(faq.getStatus()).isEqualTo(StatusPostagem.RASCUNHO);
    }

    @Test
    @DisplayName("Não deve alterar status quando FAQ não encontrado")
    void naoDeveAlterarStatusQuandoFaqNaoEncontrado() {
        when(repository.findById(faqId)).thenReturn(Optional.empty());

        faqService.setPublished(faqId);

        assertThat(faq.getStatus()).isEqualTo(StatusPostagem.RASCUNHO);
    }
}
