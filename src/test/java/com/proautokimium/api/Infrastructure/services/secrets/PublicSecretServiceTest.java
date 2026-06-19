package com.proautokimium.api.Infrastructure.services.secrets;

import com.proautokimium.api.Infrastructure.exceptions.secrets.SecretExpiredException;
import com.proautokimium.api.Infrastructure.exceptions.secrets.SecretNotFoundException;
import com.proautokimium.api.Infrastructure.interfaces.secrets.PublicSecretProjection;
import com.proautokimium.api.Infrastructure.repositories.PublicSecretRepository;
import com.proautokimium.api.domain.entities.PublicSecret;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicSecretServiceTest {

    @Mock
    private PublicSecretRepository repository;

    @Mock
    private CryptoService cryptoService;

    @Mock
    private CryptoTokenService tokenService;

    @InjectMocks
    private PublicSecretService publicSecretService;

    @Test
    @DisplayName("Deve criar segredo e retornar token")
    void deveCriarSegredoComSucesso() throws Exception {
        EncryptedData encryptedData = new EncryptedData(new byte[]{1, 2}, new byte[]{3, 4}, new byte[]{5, 6});
        when(tokenService.generateToken()).thenReturn("token123");
        when(tokenService.hashToken("token123")).thenReturn("hash123");
        when(cryptoService.encrypt("conteudo secreto")).thenReturn(encryptedData);
        when(repository.save(any(PublicSecret.class))).thenReturn(mock(PublicSecret.class));

        String result = publicSecretService.create("conteudo secreto");

        assertThat(result).isEqualTo("token123");
        verify(repository).save(any(PublicSecret.class));
    }

    @Test
    @DisplayName("Deve consumir segredo válido e retornar conteúdo")
    void deveConsumirSegredoComSucesso() throws Exception {
        PublicSecretProjection proj = mock(PublicSecretProjection.class);
        when(proj.getExpiresAt()).thenReturn(LocalDateTime.now().plusHours(1));
        when(proj.getEncryptedContent()).thenReturn(new byte[]{1});
        when(proj.getIv()).thenReturn(new byte[]{2});
        when(proj.getAuthTag()).thenReturn(new byte[]{3});
        when(tokenService.hashToken("token123")).thenReturn("hash123");
        when(repository.deleteAndReturn("hash123")).thenReturn(Optional.of(proj));
        when(cryptoService.decrypt(any(), any(), any())).thenReturn("conteudo decriptado");

        String result = publicSecretService.consume("token123");

        assertThat(result).isEqualTo("conteudo decriptado");
    }

    @Test
    @DisplayName("Deve lançar SecretNotFoundException ao consumir token inexistente")
    void deveLancarExcecaoAoConsumirTokenInexistente() throws Exception {
        when(tokenService.hashToken("token-invalido")).thenReturn("hash-invalido");
        when(repository.deleteAndReturn("hash-invalido")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> publicSecretService.consume("token-invalido"))
                .isInstanceOf(SecretNotFoundException.class);
    }

    @Test
    @DisplayName("Deve lançar SecretExpiredException ao consumir segredo expirado")
    void deveLancarExcecaoAoConsumirSegredoExpirado() throws Exception {
        PublicSecretProjection proj = mock(PublicSecretProjection.class);
        when(proj.getExpiresAt()).thenReturn(LocalDateTime.now().minusHours(1));
        when(tokenService.hashToken("token-expirado")).thenReturn("hash-expirado");
        when(repository.deleteAndReturn("hash-expirado")).thenReturn(Optional.of(proj));

        assertThatThrownBy(() -> publicSecretService.consume("token-expirado"))
                .isInstanceOf(SecretExpiredException.class);
    }
}
