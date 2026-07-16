package com.proautokimium.api.Infrastructure.services.authentication;

import com.proautokimium.api.Infrastructure.repositories.FirstAccessTokenRepository;
import com.proautokimium.api.Infrastructure.repositories.PasswordResetTokenRepository;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.auth.FirstAcessToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TokenAuthServiceTest {

    private final PasswordResetTokenRepository passwordResetTokenRepository = mock(PasswordResetTokenRepository.class);
    private final FirstAccessTokenRepository firstAccessTokenRepository = mock(FirstAccessTokenRepository.class);

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final Instant NOON = Instant.parse("2026-07-16T15:00:00Z"); // 12:00 em São Paulo

    @Test
    @DisplayName("Token deve expirar 30 minutos após a criação")
    void shouldExpireTokenThirtyMinutesAfterCreation() {
        Clock fixedClock = Clock.fixed(NOON, ZONE);
        TokenAuthService service = new TokenAuthService(passwordResetTokenRepository, firstAccessTokenRepository, fixedClock);

        service.createTokenByEmployee(new Employee());

        ArgumentCaptor<FirstAcessToken> captor = ArgumentCaptor.forClass(FirstAcessToken.class);
        verify(firstAccessTokenRepository).save(captor.capture());

        assertThat(captor.getValue().getExpiration())
                .isEqualTo(LocalDateTime.of(2026, 7, 16, 12, 30));
    }

    @Test
    @DisplayName("Expiração deve acompanhar a hora da chamada, não a da construção do serviço")
    void shouldComputeExpirationAtCallTimeNotAtServiceConstruction() {
        Clock movingClock = mock(Clock.class);
        when(movingClock.getZone()).thenReturn(ZONE);
        when(movingClock.instant()).thenReturn(NOON, NOON.plusSeconds(2 * 60 * 60));
        TokenAuthService service = new TokenAuthService(passwordResetTokenRepository, firstAccessTokenRepository, movingClock);

        service.createTokenByEmployee(new Employee());
        service.createTokenByEmployee(new Employee());

        ArgumentCaptor<FirstAcessToken> captor = ArgumentCaptor.forClass(FirstAcessToken.class);
        verify(firstAccessTokenRepository, times(2)).save(captor.capture());

        assertThat(captor.getAllValues().get(0).getExpiration())
                .isEqualTo(LocalDateTime.of(2026, 7, 16, 12, 30));
        assertThat(captor.getAllValues().get(1).getExpiration())
                .isEqualTo(LocalDateTime.of(2026, 7, 16, 14, 30));
    }
}
