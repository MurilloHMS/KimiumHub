package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.auth.WebAuthnCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebAuthnCredentialRepository extends JpaRepository<WebAuthnCredential, UUID> {

    /** No login, a única pista de quem é: o id que o aparelho deu à credencial. */
    Optional<WebAuthnCredential> findByCredentialId(String credentialId);

    boolean existsByCredentialId(String credentialId);

    /** As digitais de uma pessoa, a mais nova primeiro (Perfil e cadastro do funcionário). */
    List<WebAuthnCredential> findByUserIdOrderByCreatedAtDesc(String userId);
}
