package com.proautokimium.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.faq.FaqCreateDTO;
import com.proautokimium.api.Application.DTOs.faq.FaqPublicResponseDTO;
import com.proautokimium.api.Application.DTOs.faq.FaqResponseDTO;
import com.proautokimium.api.Application.DTOs.faq.FaqUpdateDTO;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.faq.FaqService;
import com.proautokimium.api.domain.enums.StatusPostagem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FaqController.class)
@TestPropertySource(properties = {"server.port=0"})
@Import(SecurityConfiguration.class)
class FaqControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean FaqService faqService;
    @MockitoBean TokenService tokenService;
    @MockitoBean AuthenticationManager authenticationManager;
    @MockitoBean UserRepository userRepository;

    @Test
    @DisplayName("GET /api/faq/public - deve retornar FAQs publicados sem autenticação")
    void deveRetornarFaqsPublicadosSemAutenticacao() throws Exception {
        FaqPublicResponseDTO dto = new FaqPublicResponseDTO("Pergunta", "Resposta", StatusPostagem.PUBLICADO);
        when(faqService.getAllPublic()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/faq/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Pergunta"))
                .andExpect(jsonPath("$[0].status").value("PUBLICADO"));
    }

    @Test
    @DisplayName("GET /api/faq - deve retornar todos os FAQs quando autenticado")
    @WithMockUser
    void deveRetornarTodosFaqsAutenticado() throws Exception {
        FaqResponseDTO dto = new FaqResponseDTO(UUID.randomUUID(), "Pergunta", "Resposta", StatusPostagem.RASCUNHO);
        when(faqService.getAll()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/faq"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Pergunta"))
                .andExpect(jsonPath("$[0].status").value("RASCUNHO"));
    }

    @Test
    @DisplayName("GET /api/faq - deve retornar 403 sem autenticação")
    void deveRetornar403SemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/faq"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/faq - deve criar FAQ e retornar 201")
    @WithMockUser
    void deveCriarFaqComSucesso() throws Exception {
        FaqCreateDTO dto = new FaqCreateDTO("Pergunta", "Resposta");
        doNothing().when(faqService).create(any(FaqCreateDTO.class));

        mockMvc.perform(post("/api/faq")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(content().string("Faq criado com sucesso!"));
    }

    @Test
    @DisplayName("PUT /api/faq/{id} - deve atualizar FAQ e retornar 200")
    @WithMockUser
    void deveAtualizarFaqComSucesso() throws Exception {
        UUID id = UUID.randomUUID();
        FaqUpdateDTO dto = new FaqUpdateDTO("Novo Titulo", "Novo Body");
        doNothing().when(faqService).update(any(UUID.class), any(FaqUpdateDTO.class));

        mockMvc.perform(put("/api/faq/{id}", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string("Faq atualizado com sucesso!"));
    }

    @Test
    @DisplayName("PUT /api/faq/{id}/arquivar - deve arquivar FAQ e retornar 200")
    @WithMockUser
    void deveArquivarFaqComSucesso() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(faqService).setArchived(id);

        mockMvc.perform(put("/api/faq/{id}/arquivar", id)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Faq arquivado com sucesso!"));
    }

    @Test
    @DisplayName("PUT /api/faq/{id}/rascunho - deve marcar FAQ como rascunho e retornar 200")
    @WithMockUser
    void deveMarcarFaqComoRascunhoComSucesso() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(faqService).setDraft(id);

        mockMvc.perform(put("/api/faq/{id}/rascunho", id)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Faq marcado como rascunho com sucesso!"));
    }

    @Test
    @DisplayName("PUT /api/faq/{id}/publicar - deve publicar FAQ e retornar 200")
    @WithMockUser
    void devePublicarFaqComSucesso() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(faqService).setPublished(id);

        mockMvc.perform(put("/api/faq/{id}/publicar", id)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Faq publicado com sucesso!"));
    }

    @Test
    @DisplayName("PUT /api/faq/{id}/publicar - deve retornar 403 sem autenticação")
    void deveRetornar403AoPublicarSemAutenticacao() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(put("/api/faq/{id}/publicar", id))
                .andExpect(status().isForbidden());
    }
}
