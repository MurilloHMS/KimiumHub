package com.proautokimium.api.Infrastructure.repositories.processoSeletivo;

import com.proautokimium.api.domain.entities.processoSeletivo.PerguntaPersonalizada;
import com.proautokimium.api.domain.entities.processoSeletivo.Vaga;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PerguntaPersonalizadaRepository extends JpaRepository<PerguntaPersonalizada, UUID> {
    List<PerguntaPersonalizada> findAllByVaga(Vaga vaga);
}
