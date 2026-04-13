package com.proautokimium.api.Infrastructure.repositories.processoSeletivo;

import com.proautokimium.api.domain.entities.processoSeletivo.RespostaPergunta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RespostaPerguntaRepository extends JpaRepository<RespostaPergunta, UUID> {
}
