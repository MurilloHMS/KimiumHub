package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.auth.FirstAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FirstAccessTokenRepository extends JpaRepository<FirstAccessToken, UUID> {
    Optional<FirstAccessToken> findByToken(String token);
}
