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

    /**
     * As candidaturas de uma pessoa.
     *
     * <p>Decide o caminho do "apagar meus dados": sem nenhuma, a linha sai
     * inteira; com alguma, ela e anonimizada -- um DELETE duro estouraria a FK,
     * ou levaria junto historico e respostas de uma contratacao real.
     */
    @Query("""
    select c
    from Candidatura c
    join fetch c.vaga
    where c.candidato = :candidato
""")
    List<Candidatura> findAllByCandidato(Candidato candidato);

    boolean existsByCandidato(Candidato candidato);

    /** Quantas cada candidato tem, para a aba interna nao fazer N+1. */
    @Query("select c.candidato.id, count(c) from Candidatura c group by c.candidato.id")
    List<Object[]> contarPorCandidato();
}
