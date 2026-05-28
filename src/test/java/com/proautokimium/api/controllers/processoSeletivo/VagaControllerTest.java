package com.proautokimium.api.controllers.processoSeletivo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.processoSeletivo.vaga.CreateVagaDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.vaga.ResponseVagaDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.vaga.UpdateVagaDTO;
import com.proautokimium.api.Infrastructure.exceptions.processoSeletivo.VagaNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.processoSeletivo.VagaService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VagaController.class)
@TestPropertySource(properties = {
        "server.port=0"
})
@Import(SecurityConfiguration.class)
class VagaControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean VagaService vagaService;
    @MockitoBean private TokenService tokenService;
    @MockitoBean private AuthenticationManager authenticationManager;
    @MockitoBean private UserRepository userRepository;

    @Test
    @DisplayName("GET /api/vaga/publicadas - deve retornar lista de vagas publicadas")
    void deveRetornarVagasPublicadas() throws Exception {
        UUID id = UUID.randomUUID();
        ResponseVagaDTO dto = new ResponseVagaDTO(id, "Dev Java", "Desc", "Req", "Beneficios", "area", LocalDateTime.now(), LocalDateTime.now().plusDays(2));
        when(vagaService.listarVagasPublicadas()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/vaga/publicadas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titulo").value("Dev Java"));
    }

    @Test
    @DisplayName("GET /api/vaga/arquivadas - deve retornar lista de vagas arquivadas")
    @WithMockUser(roles = "ADMIN")
    void deveRetornarVagasArquivadas() throws Exception {
        when(vagaService.listarVagasArquivadas()).thenReturn(List.of());

        mockMvc.perform(get("/api/vaga/arquivadas")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/vaga/rascunhos - deve retornar vagas em rascunho")
    @WithMockUser(roles = "ADMIN")
    void deveRetornarVagasRascunho() throws Exception {
        when(vagaService.listarVagasEmRascunho()).thenReturn(List.of());

        mockMvc.perform(get("/api/vaga/rascunhos")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/vaga/encerradas - deve retornar vagas encerradas")
    @WithMockUser(roles = "ADMIN")
    void deveRetornarVagasEncerradas() throws Exception {
        when(vagaService.listarVagasEncerrados()).thenReturn(List.of());

        mockMvc.perform(get("/api/vaga/encerradas")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/vaga - deve cadastrar vaga e retornar o ID")
    @WithMockUser(roles = "ADMIN")
    void deveCadastrarVaga() throws Exception {
        UUID id = UUID.randomUUID();
        CreateVagaDTO dto = new CreateVagaDTO("Dev Java", "Descrição", "Requisitos", "Beneficios", "area", LocalDateTime.now(), LocalDateTime.now().plusDays(2));

        com.proautokimium.api.domain.entities.processoSeletivo.Vaga vaga =
                mock(com.proautokimium.api.domain.entities.processoSeletivo.Vaga.class);
        when(vaga.getId()).thenReturn(id);
        when(vagaService.create(any(CreateVagaDTO.class))).thenReturn(vaga);

        mockMvc.perform(post("/api/vaga")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string("\"" + id + "\""));
    }

    @Test
    @DisplayName("PUT /api/vaga - deve atualizar vaga com sucesso")
    @WithMockUser(roles = "ADMIN")
    void deveAtualizarVaga() throws Exception {
        UpdateVagaDTO dto = new UpdateVagaDTO(UUID.randomUUID(), "Novo Titulo", "Nova Desc", "Novos Req", "Beneficios", "area", LocalDateTime.now(), LocalDateTime.now().plusDays(2));
        doNothing().when(vagaService).update(any(UpdateVagaDTO.class));

        mockMvc.perform(put("/api/vaga")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string("Vaga Atualizada com sucesso!"));
    }

    @Test
    @DisplayName("PUT /api/vaga/{id}/publicar - deve publicar vaga com sucesso")
    @WithMockUser(roles = "ADMIN")
    void devePublicarVaga() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(vagaService).publicar(id);

        mockMvc.perform(put("/api/vaga/{id}/publicar", id)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Vaga Publicada com sucesso!"));
    }

    @Test
    @DisplayName("PUT /api/vaga/{id}/arquivar - deve arquivar vaga com sucesso")
    @WithMockUser(roles = "ADMIN")
    void deveArquivarVaga() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(vagaService).arquivar(id);

        mockMvc.perform(put("/api/vaga/{id}/arquivar", id)
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/vaga/{id}/encerrar - deve encerrar vaga com sucesso")
    @WithMockUser(roles = "ADMIN")
    void deveEncerrarVaga() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(vagaService).encerrar(id);

        mockMvc.perform(put("/api/vaga/{id}/encerrar", id)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Vaga Encerrada com sucesso!"));
    }

    @Test
    @DisplayName("PUT /api/vaga/{id}/publicar - deve retornar 4xx quando vaga não existe")
    void deveRetornarErroAoPublicarVagaInexistente() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(VagaNotFoundException.class).when(vagaService).publicar(id);

        mockMvc.perform(put("/api/vaga/{id}/publicar", id))
                .andExpect(status().is4xxClientError());
    }
}