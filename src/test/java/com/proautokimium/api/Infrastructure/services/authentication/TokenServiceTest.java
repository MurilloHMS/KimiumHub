package com.proautokimium.api.Infrastructure.services.authentication;

import com.proautokimium.api.Infrastructure.repositories.FirstAccessTokenRepository;
import com.proautokimium.api.Infrastructure.repositories.PasswordResetTokenRepository;
import com.proautokimium.api.domain.entities.auth.PasswordResetToken;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TokenServiceTest {

    private final PasswordResetTokenRepository repository = mock(PasswordResetTokenRepository.class);
    private final FirstAccessTokenRepository firstAccessTokenRepository = mock(FirstAccessTokenRepository.class);
    private final TokenAuthService service = new TokenAuthService(repository, firstAccessTokenRepository, Clock.systemDefaultZone());

    @Test
    @DisplayName("Deve gerar token com 6 caracteres e salvar no repositório")
    void shouldGenerateAndPersistResetToken() {
        User user = new User("admin", "admin@teste.com", "hash", List.of(UserRole.ADMIN));

        String token = service.createToken(user);

        assertThat(token).hasSize(6);
        assertThat(token).matches("^[A-Za-z0-9]{6}$");

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(repository).save(captor.capture());

        PasswordResetToken savedToken = captor.getValue();
        assertThat(savedToken.getToken()).isEqualTo(token);
        assertThat(savedToken.getUser()).isEqualTo(user);
        assertThat(savedToken.getExpiration()).isAfter(LocalDateTime.now().plusMinutes(29));
    }
}