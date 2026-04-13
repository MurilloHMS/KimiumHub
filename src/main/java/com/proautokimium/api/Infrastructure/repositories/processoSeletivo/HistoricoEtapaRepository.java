package com.proautokimium.api.Infrastructure.repositories.processoSeletivo;

import com.proautokimium.api.domain.entities.processoSeletivo.HistoricoEtapa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HistoricoEtapaRepository extends JpaRepository<HistoricoEtapa, UUID> {
}
