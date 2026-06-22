package com.proautokimium.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.smtp.SmtpMail;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.email.smtp.SmtpService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SmtpController.class)
@TestPropertySource(properties = {"server.port=0"})
@Import(SecurityConfiguration.class)
class SmtpControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean SmtpService smtpService;
    @MockitoBean TokenService tokenService;
    @MockitoBean AuthenticationManager authenticationManager;
    @MockitoBean UserRepository userRepository;

    @Test
    @DisplayName("POST /api/smtp/send - deve enviar e-mail e retornar 200")
    @WithMockUser
    void deveEnviarEmailComSucesso() throws Exception {
        SmtpMail mail = new SmtpMail(
                List.of("destinatario@teste.com"),
                "remetente@teste.com",
                null,
                "Assunto de teste",
                "Corpo do email",
                null, null, null
        );
        doNothing().when(smtpService).sendEmail(any(), any());

        MockMultipartFile data = new MockMultipartFile(
                "data", "", "application/json",
                objectMapper.writeValueAsBytes(mail)
        );

        mockMvc.perform(multipart("/api/smtp/send")
                        .file(data)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("E-mail enviado com sucesso!"));
    }

    @Test
    @DisplayName("POST /api/smtp/send - deve retornar 403 sem autenticação")
    void deveRetornar403SemAutenticacao() throws Exception {
        SmtpMail mail = new SmtpMail(List.of("to@test.com"), "from@test.com", null, "Subj", "Body", null, null, null);

        MockMultipartFile data = new MockMultipartFile(
                "data", "", "application/json",
                objectMapper.writeValueAsBytes(mail)
        );

        mockMvc.perform(multipart("/api/smtp/send")
                        .file(data)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
