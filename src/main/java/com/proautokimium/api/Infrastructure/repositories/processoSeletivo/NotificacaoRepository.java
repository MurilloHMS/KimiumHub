package com.proautokimium.api.Infrastructure.repositories.processoSeletivo;

import com.proautokimium.api.domain.entities.processoSeletivo.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificacaoRepository extends JpaRepository<Notificacao, UUID> {
}
