package com.proautokimium.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.secrets.CreateSecretRequestDTO;
import com.proautokimium.api.Infrastructure.exceptions.secrets.SecretExpiredException;
import com.proautokimium.api.Infrastructure.exceptions.secrets.SecretNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.secrets.PublicSecretService;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicSecretController.class)
@TestPropertySource(properties = {"server.port=0", "app.base-url=http://localhost"})
@Import(SecurityConfiguration.class)
class PublicSecretControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean PublicSecretService publicSecretService;
    @MockitoBean TokenService tokenService;
    @MockitoBean AuthenticationManager authenticationManager;
    @MockitoBean UserRepository userRepository;

    @Test
    @DisplayName("POST /api/public-secrets - deve criar segredo e retornar URL")
    @WithMockUser
    void deveCriarSegredoComSucesso() throws Exception {
        CreateSecretRequestDTO dto = new CreateSecretRequestDTO("conteudo secreto");
        when(publicSecretService.create("conteudo secreto")).thenReturn("token-gerado");

        mockMvc.perform(post("/api/public-secrets")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("http://localhost/s/token-gerado"));
    }

    @Test
    @DisplayName("POST /api/public-secrets - deve retornar 403 sem autenticação")
    void deveRetornar403SemAutenticacao() throws Exception {
        CreateSecretRequestDTO dto = new CreateSecretRequestDTO("conteudo");

        mockMvc.perform(post("/api/public-secrets")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/public-secrets/{token} - deve consumir segredo sem autenticação")
    void deveConsumirSegredoSemAutenticacao() throws Exception {
        when(publicSecretService.consume("token-valido")).thenReturn("conteudo decriptado");

        mockMvc.perform(get("/api/public-secrets/{token}", "token-valido"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("conteudo decriptado"));
    }

    @Test
    @DisplayName("GET /api/public-secrets/{token} - deve retornar 404 quando segredo não existe")
    void deveRetornar404QuandoSegredoNaoExiste() throws Exception {
        when(publicSecretService.consume("token-invalido")).thenThrow(new SecretNotFoundException());

        mockMvc.perform(get("/api/public-secrets/{token}", "token-invalido"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/public-secrets/{token} - deve retornar 410 quando segredo expirado")
    void deveRetornar410QuandoSegredoExpirado() throws Exception {
        when(publicSecretService.consume("token-expirado")).thenThrow(new SecretExpiredException());

        mockMvc.perform(get("/api/public-secrets/{token}", "token-expirado"))
                .andExpect(status().is(410));
    }
}
