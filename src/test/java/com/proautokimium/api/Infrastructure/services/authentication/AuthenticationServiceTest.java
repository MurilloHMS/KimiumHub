package com.proautokimium.api.Infrastructure.services.authentication;

import com.proautokimium.api.Application.DTOs.authentication.NewAccessPasswordDTO;
import com.proautokimium.api.Application.DTOs.user.RegisterDTO;
import com.proautokimium.api.Infrastructure.exceptions.auth.UserAlreadyExistsException;
import com.proautokimium.api.Infrastructure.exceptions.auth.UserBlockedException;
import com.proautokimium.api.Infrastructure.repositories.CustomerRepository;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.PasswordResetTokenRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.email.AuthEmailService;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.auth.FirstAccessToken;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuthenticationServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final Instant NOON = Instant.parse("2026-07-16T15:00:00Z"); // 12:00 em São Paulo
    private static final LocalDateTime NOON_LOCAL = LocalDateTime.ofInstant(NOON, ZONE);

    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
    private final TokenAuthService tokenAuthService = mock(TokenAuthService.class);
    private final PasswordResetTokenRepository passwordResetTokenRepository = mock(PasswordResetTokenRepository.class);
    private final TokenService tokenService = mock(TokenService.class);
    private final AuthEmailService authEmailService = mock(AuthEmailService.class);
    private final CustomerRepository customerRepository = mock(CustomerRepository.class);

    private final AuthenticationService service = new AuthenticationService(
            authenticationManager,
            userRepository,
            employeeRepository,
            tokenAuthService,
            passwordResetTokenRepository,
            tokenService,
            Clock.fixed(NOON, ZONE),
            authEmailService,
            customerRepository
    );

    @Test
    @DisplayName("Não deve criar usuário no primeiro acesso quando o funcionário já possui usuário vinculado")
    void shouldRejectFirstAccessSignInWhenEmployeeAlreadyHasUser() {
        FirstAccessToken token = tokenForEmployee("João Silva");
        User existing = new User("joao.silva", "joao@teste.com", "hash", java.util.List.of(UserRole.USER));

        when(tokenAuthService.getToken("ABC123")).thenReturn(Optional.of(token));
        when(userRepository.findByEmployee_Id(token.getPartner().getId())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.signInFirstAccess("ABC123", new NewAccessPasswordDTO("Senha@123", "novo@teste.com")))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("joao.silva");

        verify(userRepository, never()).save(any());
        verify(tokenAuthService, never()).markTokenUsed(any());
    }

    @Test
    @DisplayName("Deve criar usuário e marcar o token como usado no primeiro acesso")
    void shouldCreateUserAndMarkTokenUsedOnFirstAccessSignIn() {
        FirstAccessToken token = tokenForEmployee("João Silva");

        when(tokenAuthService.getToken("ABC123")).thenReturn(Optional.of(token));
        when(userRepository.findByEmployee_Id(token.getPartner().getId())).thenReturn(Optional.empty());
        when(userRepository.existsByLogin(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User created = service.signInFirstAccess("ABC123", new NewAccessPasswordDTO("Senha@123", "novo@teste.com"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getRoles()).containsExactly(UserRole.USER);
        assertThat(saved.getEmployee()).isEqualTo(token.getPartner());
        assertThat(saved.getLogin()).isNotBlank();
        assertThat(saved.getPassword()).isNotEqualTo("Senha@123");
        assertThat(created).isEqualTo(saved);

        verify(tokenAuthService).markTokenUsed(token);
    }

    @Test
    @DisplayName("Token dentro do prazo e não usado deve ser considerado válido")
    void shouldConsiderTokenValidWhenNotExpiredAndNotUsed() {
        FirstAccessToken token = tokenForEmployee("João Silva");
        token.setExpiration(NOON_LOCAL.plusMinutes(1));

        when(tokenAuthService.isValid("ABC123")).thenReturn(Optional.of(token));

        assertThat(service.firstAccessTokenIsValid("ABC123")).isTrue();
    }

    @Test
    @DisplayName("Token expirado deve ser considerado inválido")
    void shouldConsiderTokenInvalidWhenExpired() {
        FirstAccessToken token = tokenForEmployee("João Silva");
        token.setExpiration(NOON_LOCAL.minusMinutes(1));

        when(tokenAuthService.isValid("ABC123")).thenReturn(Optional.of(token));

        assertThat(service.firstAccessTokenIsValid("ABC123")).isFalse();
    }

    @Test
    @DisplayName("Token já usado deve ser considerado inválido mesmo dentro do prazo")
    void shouldConsiderTokenInvalidWhenAlreadyUsed() {
        FirstAccessToken token = tokenForEmployee("João Silva");
        token.setExpiration(NOON_LOCAL.plusMinutes(30));
        token.markUsed();

        when(tokenAuthService.isValid("ABC123")).thenReturn(Optional.of(token));

        assertThat(service.firstAccessTokenIsValid("ABC123")).isFalse();
    }

    @Test
    @DisplayName("Não deve permitir bloquear um usuário com role DEVELOPER")
    void shouldBlockUserWhenNotActive() {
        User user = mock(User.class);
        when(user.getLogin()).thenReturn("murillo.henrique");
        when(user.getRoles()).thenReturn(List.of(UserRole.DEVELOPER));
        when(userRepository.findByLogin("murillo.henrique")).thenReturn(user);

        assertThrows(
                UserBlockedException.class,
                () -> service.blockUser("murillo.henrique")
        );

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Não deve criar usuário quando login já existe")
    void shouldRejectCreateUserIfAlreadyExists(){
        RegisterDTO register = new RegisterDTO(
                "usuario.existe", "email@mail.com", "Password@123", List.of(UserRole.USER));

        when(userRepository.findByLogin("usuario.existe"))
                .thenReturn(new User("usuario.existe", "outro@email.com", "hash", List.of(UserRole.USER)));

        assertThrows(
                UserAlreadyExistsException.class,
                () -> service.signIn(register)
        );

        verify(userRepository, never()).save(any());
    }

    private FirstAccessToken tokenForEmployee(String employeeName) {
        Employee employee = new Employee();
        employee.id = UUID.randomUUID();
        employee.setName(employeeName);

        FirstAccessToken token = new FirstAccessToken();
        token.setToken("ABC123");
        token.setPartner(employee);
        token.setExpiration(NOON_LOCAL.plusMinutes(30));
        return token;
    }
}
