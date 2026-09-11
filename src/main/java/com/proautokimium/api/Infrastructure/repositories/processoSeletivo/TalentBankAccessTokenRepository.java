package com.proautokimium.api.Infrastructure.repositories.processoSeletivo;

import com.proautokimium.api.domain.entities.processoSeletivo.Candidato;
import com.proautokimium.api.domain.entities.processoSeletivo.TalentBankAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TalentBankAccessTokenRepository extends JpaRepository<TalentBankAccessToken, UUID> {

    Optional<TalentBankAccessToken> findByTokenHash(String tokenHash);

    /**
     * Revoga todos os links vivos de um candidato.
     *
     * <p>Chamado antes de emitir um novo. Sem isso, cada pedido deixa mais uma
     * chave de 24h viva numa caixa de e-mail — e o link mais novo deixa de ser
     * o único que funciona, que é o que as pessoas esperam quando pedem de novo
     * porque o primeiro não chegou.
     */
    @Modifying
    @Query("update TalentBankAccessToken t set t.revokedAt = :agora " +
           "where t.candidato = :candidato and t.revokedAt is null")
    int revogarVivosDo(@Param("candidato") Candidato candidato, @Param("agora") LocalDateTime agora);

    /**
     * O cooldown, feito com a tabela que já existe em vez de biblioteca nova.
     *
     * <p>Fecha a amplificação de e-mail: sem ele, uma rota pública sem rate
     * limiting dispara quantas mensagens alguém quiser para um endereço que
     * esteja na base.
     */
    boolean existsByCandidatoAndRevokedAtIsNullAndCreatedAtAfter(Candidato candidato, LocalDateTime desde);

    /** Limpeza: expirado não abre nada, e revogado velho já cumpriu o papel. */
    @Query("select t from TalentBankAccessToken t " +
           "where t.expiresAt < :agora or (t.revokedAt is not null and t.revokedAt < :limiteRevogados)")
    List<TalentBankAccessToken> paraLimpar(@Param("agora") LocalDateTime agora,
                                           @Param("limiteRevogados") LocalDateTime limiteRevogados);

    List<TalentBankAccessToken> findAllByCandidato(Candidato candidato);
}
