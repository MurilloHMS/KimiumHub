package com.proautokimium.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Infrastructure.services.permission.PermissionService;
import com.proautokimium.api.Application.DTOs.user.AuthenticationDTO;
import com.proautokimium.api.Application.DTOs.user.RegisterDTO;
import com.proautokimium.api.Infrastructure.exceptions.auth.token.TokenInvalidException;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.PasswordResetTokenRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.authentication.AuthenticationService;
import com.proautokimium.api.Infrastructure.services.authentication.AuthorizationService;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
import com.proautokimium.api.Application.DTOs.user.LoginResponseDTO;


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

    // O SecurityFilter passa a somar as permissões de tela às roles.
    @MockitoBean
    private PermissionService permissionService;

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

    @MockitoBean
    private AuthorizationService authorizationService;

    @Test
    @DisplayName("Deve fazer login com sucesso e retornar token")
    void shouldLoginSuccessfully() throws Exception {
        AuthenticationDTO dto = new AuthenticationDTO("admin", "123456");

        when(authService.login(any(AuthenticationDTO.class)))
                .thenReturn(new LoginResponseDTO("jwt-token", "refresh-token"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("jwt-token")));
    }

    @Test
    @DisplayName("Deve registrar usuário novo com senha criptografada")
    @WithMockUser(authorities = {"settings/admin:INCLUIR"})
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
    @WithMockUser(authorities = {"settings/admin:ENVIAR"})
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
    @WithMockUser(authorities = {"settings/admin:ENVIAR"})
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

    /**
     * A permissão de configuração do Admin, e não mais a role.
     *
     * Este teste passava **sem authority nenhuma** até 2026-08-27, porque o
     * endpoint não tinha `@PreAuthorize` — ele afirmava o buraco em vez de
     * proteger contra ele. O par abaixo é o que faltava.
     */
    @Test
    @DisplayName("Deve atualizar roles do usuário")
    @WithMockUser(username = "admin", authorities = {"settings/admin:CONFIGURAR"})
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

    /**
     * **O buraco que estava aberto.**
     *
     * Sem `settings/admin:CONFIGURAR`, qualquer funcionário logado mudava a
     * role de qualquer pessoa — inclusive a própria, para ADMIN. Não era
     * decisão: ninguém tinha reparado que a anotação faltava.
     */
    @Test
    @DisplayName("funcionário comum não muda a role de ninguém")
    @WithMockUser(username = "ricardo", authorities = {"ROLE_USER", "rh/hub:CONSULTAR"})
    void funcionarioComumNaoMudaRoles() throws Exception {
        mockMvc.perform(put("/api/auth/users/admin/roles")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roles": ["ADMIN"]
                                }
                                """))
                .andExpect(status().isForbidden());

        verify(userRepository, never()).save(any(User.class));
    }

    /** Vincular funcionário a uma conta estava igualmente aberto. */
    @Test
    @DisplayName("funcionário comum não vincula funcionário a conta nenhuma")
    @WithMockUser(username = "ricardo", authorities = {"ROLE_USER"})
    void funcionarioComumNaoVincula() throws Exception {
        mockMvc.perform(put("/api/auth/users/admin/employee")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        // Corpo VALIDO de proposito: a validacao roda antes do
                        // @PreAuthorize, entao um corpo torto devolve 400 e o
                        // teste mediria a validacao em vez da porta.
                        .content("{\"codParceiro\":\"000123\"}"))
                .andExpect(status().isForbidden());
    }

    /**
     * Trocar a própria senha deixou de exigir ADMIN — e passou a ser a PRÓPRIA.
     *
     * O endpoint tirava o login do corpo e ignorava a senha atual. Só abrir o
     * `@PreAuthorize` teria dado a qualquer funcionário logado o poder de
     * trocar a senha de qualquer pessoa. Estes dois testes são o par: a porta
     * abriu, e o alvo passou a ser quem está autenticado.
     */
    @Test
    @DisplayName("qualquer pessoa logada troca a própria senha")
    @WithMockUser(username = "ricardo", authorities = {"ROLE_USER"})
    void qualquerUmTrocaAPropriaSenha() throws Exception {
        User ricardo = new User("ricardo", "ricardo@teste.com",
                new BCryptPasswordEncoder().encode("Antiga123@"), List.of(UserRole.USER));
        when(userRepository.findByLogin("ricardo")).thenReturn(ricardo);

        mockMvc.perform(post("/api/auth/change-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login": "ricardo",
                                  "currentPassword": "Antiga123@",
                                  "newPassword": "Nova12345@"
                                }
                                """))
                .andExpect(status().isOk());

        verify(userRepository).save(ricardo);
    }

    /**
     * **O teste que impede a escalada.**
     *
     * O corpo manda `login: "admin"`, e quem está autenticado é o Ricardo. A
     * senha trocada tem que ser a dele — e como a senha atual dele não confere
     * com a mandada, nada é trocado.
     */
    @Test
    @DisplayName("o login do corpo não escolhe a vítima")
    @WithMockUser(username = "ricardo", authorities = {"ROLE_USER"})
    void corpoNaoEscolheAVitima() throws Exception {
        User ricardo = new User("ricardo", "ricardo@teste.com",
                new BCryptPasswordEncoder().encode("Antiga123@"), List.of(UserRole.USER));
        when(userRepository.findByLogin("ricardo")).thenReturn(ricardo);

        mockMvc.perform(post("/api/auth/change-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login": "admin",
                                  "currentPassword": "SenhaDoAdmin1@",
                                  "newPassword": "Invadida123@"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        verify(userRepository, never()).save(any(User.class));
        verify(userRepository, never()).findByLogin("admin");
    }

    @Test
    @DisplayName("Deve delegar a recuperação de senha e responder sempre igual")
    void shouldDelegateForgotPassword() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "login": "12.345.678/0001-90"
                                }
                                """))
                .andExpect(status().isOk());

        verify(authorizationService).forgotPassword("12.345.678/0001-90");
    }

    // ─── O fluxo de primeiro acesso ──────────────────────────────────────────

    /**
     * **Estas duas rotas não podem exigir login, nunca.**
     *
     * Quem valida o token e quem escolhe a senha ainda não tem conta — é o que
     * o primeiro acesso é. Se elas ficarem atrás de permissão, funcionário novo
     * nenhum entra no sistema, e o sintoma é um link de e-mail que abre numa
     * tela de erro.
     *
     * O `SecurityPaths` liberava `/api/auth/first-access/**`, e esse padrão
     * casa também o caminho base — deixando aberto o endpoint que DISPARA o
     * convite. Trocado por dois padrões de um segmento; estes testes existem
     * para provar que a troca não fechou o que precisava ficar aberto.
     */
    @Test
    @DisplayName("validar o token do primeiro acesso não exige login")
    void validarTokenNaoExigeLogin() throws Exception {
        mockMvc.perform(post("/api/auth/first-access/um-token-qualquer/is-valid")
                        .with(csrf()))
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertNotEquals(
                        403, result.getResponse().getStatus(),
                        "quem chega pelo link do e-mail ainda nao tem conta"));
    }

    @Test
    @DisplayName("escolher a senha no primeiro acesso não exige login")
    void escolherSenhaNaoExigeLogin() throws Exception {
        mockMvc.perform(post("/api/auth/first-access/um-token-qualquer/sign-in")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "novato",
                                  "password": "Primeira123@"
                                }
                                """))
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertNotEquals(
                        403, result.getResponse().getStatus(),
                        "quem escolhe a senha ainda nao tem conta"));
    }

    /**
     * **O primeiro acesso tem que passar sem login.**
     *
     * Quem chama aqui ainda não tem usuário: é o funcionário pedindo o dele.
     * Uma vez este endpoint foi fechado por engano, lido como convite do RH, e
     * o fluxo inteiro ficou inalcançável — as duas etapas seguintes já eram
     * públicas e ninguém conseguia chegar nelas.
     *
     * Fechar de novo é fácil, de dois jeitos independentes: uma anotação no
     * método, ou tirar o caminho de `SecurityPaths.PUBLIC_POST`. Este teste
     * pega os dois, porque afirma a resposta e não o caminho.
     *
     * A resposta esperada é `409` de propósito. Sem autenticação, o pedido tem
     * que chegar ao corpo do método — e o método, com um CPF que não existe,
     * responde conflito. Um `403` aqui significa que ele nem entrou.
     */
    @Test
    @DisplayName("pedir o primeiro acesso não exige login")
    void primeiroAcessoEhPublico() throws Exception {
        when(employeeRepository.findByCpfDigits("00000000000")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/first-access")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpf\":\"00000000000\",\"email\":\"alguem@teste.com\"}"))
                .andExpect(status().isConflict());
    }

    /**
     * **A saída de emergência não se fecha por requisição.**
     *
     * A conta de desenvolvedor é o que garante que sempre exista alguém capaz
     * de reabrir o controle de acesso. Se a role caísse por um PUT, o sistema
     * poderia ficar sem ninguém que consiga configurá-lo — e a volta seria
     * `INSERT` no banco.
     */
    @Test
    @DisplayName("não dá para tirar a role de desenvolvedor")
    @WithMockUser(username = "murillo", authorities = {"settings/admin:CONFIGURAR"})
    void naoTiraODesenvolvedor() throws Exception {
        User dev = new User("murillo", "murillo@teste.com", "hash",
                List.of(UserRole.DEVELOPER, UserRole.ADMIN));
        when(userRepository.findByLogin("murillo")).thenReturn(dev);

        mockMvc.perform(put("/api/auth/users/murillo/roles")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roles": ["USER"]
                                }
                                """))
                .andExpect(status().isConflict());

        verify(userRepository, never()).save(any(User.class));
    }

    /** E mexer nas outras roles dele continua valendo, desde que ela fique. */
    @Test
    @DisplayName("as outras roles do desenvolvedor podem mudar")
    @WithMockUser(username = "murillo", authorities = {"settings/admin:CONFIGURAR"})
    void asOutrasRolesPodemMudar() throws Exception {
        User dev = new User("murillo", "murillo@teste.com", "hash",
                List.of(UserRole.DEVELOPER, UserRole.ADMIN));
        when(userRepository.findByLogin("murillo")).thenReturn(dev);

        mockMvc.perform(put("/api/auth/users/murillo/roles")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roles": ["DEVELOPER", "RH"]
                                }
                                """))
                .andExpect(status().isOk());

        verify(userRepository).save(dev);
    }
}
