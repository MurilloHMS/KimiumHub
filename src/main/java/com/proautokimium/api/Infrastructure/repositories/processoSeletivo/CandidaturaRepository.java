package com.proautokimium.api.Infrastructure.repositories.processoSeletivo;

import com.proautokimium.api.domain.entities.processoSeletivo.Candidatura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CandidaturaRepository extends JpaRepository<Candidatura, UUID> {
}
