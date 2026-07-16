package com.proautokimium.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.user.AuthenticationDTO;
import com.proautokimium.api.Application.DTOs.user.RegisterDTO;
import com.proautokimium.api.Infrastructure.exceptions.auth.token.TokenInvalidException;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.PasswordResetTokenRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.authentication.AuthenticationService;
import com.proautokimium.api.Infrastructure.services.authentication.TokenAuthService;
import com.proautokimium.api.Infrastructure.services.email.EmailQueueService;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.enums.UserRole;
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

import com.proautokimium.api.Application.DTOs.authentication.ResetPasswordDTO;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;


@WebMvcTest(AuthenticationController.class)
@TestPropertySource(properties = {
        "server.port=0"
})
@Import(SecurityConfiguration.class)
class AuthenticationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private EmployeeRepository employeeRepository;

    @MockitoBean
    private TokenAuthService tokenAuthService;

    @MockitoBean
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private EmailQueueService emailQueueService;

    @MockitoBean
    private AuthenticationService authService;

    @Test
    @DisplayName("Deve fazer login com sucesso e retornar token")
    void shouldLoginSuccessfully() throws Exception {
        AuthenticationDTO dto = new AuthenticationDTO("admin", "123456");

        when(authService.login(any(AuthenticationDTO.class))).thenReturn("jwt-token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("jwt-token")));
    }

    @Test
    @DisplayName("Deve registrar usuário novo com senha criptografada")
    @WithMockUser(roles = "ADMIN")
    void sholdRegisterNewUser() throws Exception {
        RegisterDTO dto = new RegisterDTO("novo.usuario", "email@exemple.com", "123456", List.of(UserRole.ADMIN));

        when(userRepository.findByLogin(dto.login())).thenReturn(null);
        when(authService.signIn(any(RegisterDTO.class)))
                .thenReturn(new User("novo.usuario", "email@exemple.com", "hash", List.of(UserRole.ADMIN)));

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string("Usuário criado com sucesso!"));

        verify(authService).signIn(dto);
    }

    @Test
    @DisplayName("Não deve registrar usuário já existente")
    @WithMockUser(roles = "ADMIN")
    void shouldNotRegisterExistingUser() throws Exception {
        RegisterDTO dto = new RegisterDTO("admin", "email@email.com", "123456", List.of(UserRole.ADMIN));

        when(userRepository.findByLogin(dto.login()))
                .thenReturn(new User("admin", "admin", "hash", List.of(UserRole.ADMIN)));

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(content().string("O Usuário informado, já existe!"));

        verify(authService, never()).signIn(any());
    }

    @Test
    @DisplayName("Deve retornar ok no forgot-password mesmo quando usuário não existe")
    void shouldReturnOkWhenUserDoesNotExistOnForgotPassword() throws Exception {
        when(userRepository.findByLogin("inexistente")).thenReturn(null);

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login": "inexistente"
                                }
                                """))
                .andExpect(status().isOk());

        verify(tokenAuthService, never()).createToken(any());
        verify(emailQueueService, never()).sendNow(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve criar token e enviar email no forgot-password")
    void shouldCreateTokenAndSendEmailOnForgotPassword() throws Exception {
        User user = new User("admin", "admin@teste.com", "hash", List.of(UserRole.ADMIN));

        when(userRepository.findByLogin("admin")).thenReturn(user);
        when(tokenAuthService.createToken(user)).thenReturn("ABC123");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login": "admin"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string("Token de recuperação de senha enviado para o e-mail cadastrado."));

        verify(tokenAuthService).createToken(user);
        verify(emailQueueService).sendNow(eq("admin@teste.com"), any(), any(), any());
    }

    @Test
    @DisplayName("Deve retornar bad request quando token de reset é inválido")
    void shouldReturnBadRequestWhenResetTokenIsInvalid() throws Exception {
        when(authService.resetPassword(any(ResetPasswordDTO.class)))
                .thenThrow(new TokenInvalidException("Token inválido."));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "token": "TOKEN_INVALIDO",
                              "newPassword": "novaSenha123"
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Token inválido."));
    }

    @Test
    @DisplayName("Deve redefinir senha com sucesso")
    void shouldResetPasswordSuccessfully() throws Exception {
        when(authService.resetPassword(any(ResetPasswordDTO.class)))
                .thenReturn("Senha redefinida com sucesso.");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "ABC123",
                                  "newPassword": "novaSenha123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string("Senha redefinida com sucesso."));

        verify(authService).resetPassword(any(ResetPasswordDTO.class));
    }

    @Test
    @DisplayName("Deve atualizar roles do usuário")
    @WithMockUser(roles = "ADMIN")
    void shouldUpdateUserRoles() throws Exception {
        User user = new User("admin", "admin", "hash", List.of(UserRole.ADMIN));

        when(userRepository.findByLogin("admin")).thenReturn(user);

        mockMvc.perform(put("/api/auth/users/admin/roles")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roles": ["USER"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string("Roles Atualizadas com sucesso!"));

        verify(userRepository).save(user);
    }
}