package com.proautokimium.api.Infrastructure.repositories.processoSeletivo;

import com.proautokimium.api.domain.entities.processoSeletivo.Candidato;
import com.proautokimium.api.domain.entities.processoSeletivo.Candidatura;
import com.proautokimium.api.domain.entities.processoSeletivo.Vaga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CandidaturaRepository extends JpaRepository<Candidatura, UUID> {
    boolean existsByCandidatoAndVaga(Candidato candidato, Vaga vaga);
    @Query("""
    select c
    from Candidatura c
    join fetch c.candidato
    join fetch c.vaga
    where c.vaga.id = :vagaId
""")
    List<Candidatura> findCandidaturasByVagaId(UUID vagaId);
}
