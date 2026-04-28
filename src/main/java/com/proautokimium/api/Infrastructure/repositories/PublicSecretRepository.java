package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.Infrastructure.interfaces.secrets.PublicSecretProjection;
import com.proautokimium.api.domain.entities.PublicSecret;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PublicSecretRepository extends JpaRepository<PublicSecret, UUID > {

    @Transactional
    @Modifying
    @Query("DELETE FROM PublicSecret p WHERE p.expiresAt < :now")
    int deleteExpired(@Param("now")LocalDateTime now);

    @Query(value = """
        DELETE FROM public_secrets
            WHERE token_hash = :tokenHash
                RETURNING id, encrypted_content, iv, auth_tag, expires_at
    """, nativeQuery = true)
    Optional<PublicSecretProjection> deleteAndReturn(@Param("tokenHash") String tokenHash);
}
