package com.proautokimium.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.email.SmtpEmailRequestDTO;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.email.EmailService;
import com.proautokimium.api.domain.entities.EmailEntity;
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

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmailController.class)
@TestPropertySource(properties = {"server.port=0"})
@Import(SecurityConfiguration.class)
class EmailControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean EmailService emailService;
    @MockitoBean TokenService tokenService;
    @MockitoBean AuthenticationManager authenticationManager;
    @MockitoBean UserRepository userRepository;

    @Test
    @DisplayName("POST /api/email - deve criar email e retornar 200")
    @WithMockUser
    void deveCriarEmailComSucesso() throws Exception {
        SmtpEmailRequestDTO dto = new SmtpEmailRequestDTO("remetente");
        doNothing().when(emailService).saveEmail(any(SmtpEmailRequestDTO.class));

        mockMvc.perform(post("/api/email")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/email - deve retornar 403 sem autenticação")
    void deveRetornar403SemAutenticacao() throws Exception {
        SmtpEmailRequestDTO dto = new SmtpEmailRequestDTO("remetente");

        mockMvc.perform(post("/api/email")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/email - deve retornar todos os emails quando autenticado")
    @WithMockUser
    void deveRetornarTodosOsEmailsAutenticado() throws Exception {
        EmailEntity entity = mock(EmailEntity.class);
        when(emailService.getAll()).thenReturn(Set.of(entity));

        mockMvc.perform(get("/api/email"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/email - deve retornar 403 sem autenticação")
    void deveRetornar403AoListarSemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/email"))
                .andExpect(status().isForbidden());
    }
}
