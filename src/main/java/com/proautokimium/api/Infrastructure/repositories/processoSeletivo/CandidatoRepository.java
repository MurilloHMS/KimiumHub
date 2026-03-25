package com.proautokimium.api.Infrastructure.repositories.processoSeletivo;

import com.proautokimium.api.domain.entities.processoSeletivo.Vaga;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CandidatoRepository extends JpaRepository<Vaga, UUID> {
}
