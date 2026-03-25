package com.proautokimium.api.Infrastructure.repositories.processoSeletivo;

import com.proautokimium.api.domain.entities.processoSeletivo.Vaga;
import com.proautokimium.api.domain.enums.processoSeletivo.StatusVaga;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VagaRepository extends JpaRepository<Vaga, UUID> {
    List<Vaga> findByStatus(StatusVaga status);
}
