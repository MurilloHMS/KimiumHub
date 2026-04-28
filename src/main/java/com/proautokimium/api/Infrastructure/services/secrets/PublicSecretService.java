package com.proautokimium.api.Infrastructure.services.secrets;

import com.proautokimium.api.Infrastructure.exceptions.secrets.SecretExpiredException;
import com.proautokimium.api.Infrastructure.exceptions.secrets.SecretNotFoundException;
import com.proautokimium.api.Infrastructure.interfaces.secrets.PublicSecretProjection;
import com.proautokimium.api.Infrastructure.repositories.PublicSecretRepository;
import com.proautokimium.api.domain.entities.PublicSecret;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PublicSecretService {
    private final PublicSecretRepository repository;
    private final CryptoService cryptoService;
    private final CryptoTokenService tokenService;

    @Value("${public-secret.expiration-hours:24}")
    private int expirationHours;

    public PublicSecretService(PublicSecretRepository repository, CryptoService cryptoService, CryptoTokenService tokenService) {
        this.repository = repository;
        this.cryptoService = cryptoService;
        this.tokenService = tokenService;
    }

    @Transactional
    public String create(String content) throws Exception{
        String token = tokenService.generateToken();
        String tokenHash = tokenService.hashToken(token);
        EncryptedData enc = cryptoService.encrypt(content);

        PublicSecret entity = new PublicSecret();
        entity.setTokenHash(tokenHash);
        entity.setEncryptedContent(enc.getCipherText());
        entity.setIv(enc.getIv());
        entity.setAuthTag(enc.getAuthTag());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setExpiresAt(LocalDateTime.now().plusHours(expirationHours));
        repository.save(entity);
        return token;
    }

    @Transactional
    public String consume(String token) throws Exception{
        String hash = tokenService.hashToken(token);
        PublicSecretProjection proj = repository.deleteAndReturn(hash)
                .orElseThrow(SecretNotFoundException::new);

        if(proj.getExpiresAt().isBefore(LocalDateTime.now()))
            throw new SecretExpiredException();

        return cryptoService.decrypt(proj.getEncryptedContent(), proj.getIv(), proj.getAuthTag());
    }
}
