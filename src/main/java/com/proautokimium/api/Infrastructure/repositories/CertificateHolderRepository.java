package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.CertificateHolder;
import com.proautokimium.api.domain.valueObjects.Email;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CertificateHolderRepository extends JpaRepository<CertificateHolder, UUID> {

    Optional<CertificateHolder> findByEmail(Email email);
    Optional<CertificateHolder> findByName(String name);
}
