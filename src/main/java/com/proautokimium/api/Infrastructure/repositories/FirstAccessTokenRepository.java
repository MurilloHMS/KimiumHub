package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.auth.FirstAcessToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FirstAccessTokenRepository extends JpaRepository<FirstAcessToken, UUID> {
    Optional<FirstAcessToken> findByToken(String token);
}
