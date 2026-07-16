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
import com.proautokimium.api.Infrastructure.services.email.AuthEmailService;
import com.proautokimium.api.Infrastructure.services.email.EmailQueueService;
import com.proautokimium.api.Infrastructure.services.notification.NotificationService;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.enums.NotificationType;
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
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private AuthEmailService authEmailService;

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
    @DisplayName("Deve bloquear o primeiro acesso quando o funcionário já possui usuário vinculado")
    void shouldBlockFirstAccessWhenEmployeeAlreadyHasUser() throws Exception {
        Employee employee = new Employee();
        employee.id = UUID.randomUUID();

        when(employeeRepository.findByCpfDigits("12345678900")).thenReturn(Optional.of(employee));
        when(userRepository.findByEmployee_Id(employee.getId()))
                .thenReturn(Optional.of(new User("joao.silva", "joao@teste.com", "hash", List.of(UserRole.USER))));

        mockMvc.perform(post("/api/auth/first-access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cpf": "12345678900",
                                  "email": "novo@teste.com"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "Já existe um usuário cadastrado para o CPF informado. Utilize a recuperação de senha ou contate o RH."));

        verify(tokenAuthService, never()).createTokenByEmployee(any());
        verify(authEmailService, never()).sendFirstAccessToken(any(), any());
    }

    @Test
    @DisplayName("Deve enviar o token de primeiro acesso quando o funcionário ainda não tem usuário")
    void shouldSendFirstAccessTokenWhenEmployeeHasNoUser() throws Exception {
        Employee employee = new Employee();
        employee.id = UUID.randomUUID();

        when(employeeRepository.findByCpfDigits("12345678900")).thenReturn(Optional.of(employee));
        when(userRepository.findByEmployee_Id(employee.getId())).thenReturn(Optional.empty());
        when(tokenAuthService.createTokenByEmployee(employee)).thenReturn("ABC123");

        mockMvc.perform(post("/api/auth/first-access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cpf": "12345678900",
                                  "email": "novo@teste.com"
                                }
                                """))
                .andExpect(status().isOk());

        verify(authEmailService).sendFirstAccessToken("novo@teste.com", "ABC123");
    }

    @Test
    @DisplayName("Deve criar usuário no sign-in do primeiro acesso e notificar RH e Desenvolvedores")
    void shouldCreateUserAndNotifyRhAndDevelopersOnFirstAccessSignIn() throws Exception {
        User created = userCreatedViaFirstAccess();

        when(authService.firstAccessTokenIsValid("ABC123")).thenReturn(true);
        when(authService.signInFirstAccess(eq("ABC123"), any())).thenReturn(created);
        when(userRepository.findByRolesIn(List.of(UserRole.RH, UserRole.DEVELOPER))).thenReturn(List.of(
                new User("ana.rh", "ana@teste.com", "hash", List.of(UserRole.RH)),
                new User("beto.dev", "beto@teste.com", "hash", List.of(UserRole.DEVELOPER))));

        mockMvc.perform(post("/api/auth/first-access/ABC123/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "Senha@123",
                                  "email": "novo@teste.com"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("joao.silva")));

        verify(notificationService).notify(eq("ana.rh"), eq(NotificationType.GERAL), any(), contains("joao.silva"), isNull());
        verify(notificationService).notify(eq("beto.dev"), eq(NotificationType.GERAL), any(), contains("joao.silva"), isNull());
    }

    @Test
    @DisplayName("Deve retornar bad request no sign-in quando o token é inválido")
    void shouldReturnBadRequestOnFirstAccessSignInWhenTokenIsInvalid() throws Exception {
        when(authService.firstAccessTokenIsValid("INVALIDO")).thenReturn(false);

        mockMvc.perform(post("/api/auth/first-access/INVALIDO/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "Senha@123",
                                  "email": "novo@teste.com"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(authService, never()).signInFirstAccess(any(), any());
        verify(notificationService, never()).notify(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve criar o usuário mesmo quando a notificação de RH/Desenvolvedores falha")
    void shouldStillCreateUserWhenNotificationFails() throws Exception {
        User created = userCreatedViaFirstAccess();

        when(authService.firstAccessTokenIsValid("ABC123")).thenReturn(true);
        when(authService.signInFirstAccess(eq("ABC123"), any())).thenReturn(created);
        when(userRepository.findByRolesIn(any())).thenThrow(new RuntimeException("banco fora do ar"));

        mockMvc.perform(post("/api/auth/first-access/ABC123/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "Senha@123",
                                  "email": "novo@teste.com"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    private User userCreatedViaFirstAccess() {
        Employee employee = new Employee();
        employee.setName("João Silva");

        User created = new User("joao.silva", "novo@teste.com", "hash", List.of(UserRole.USER));
        created.setEmployee(employee);
        return created;
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