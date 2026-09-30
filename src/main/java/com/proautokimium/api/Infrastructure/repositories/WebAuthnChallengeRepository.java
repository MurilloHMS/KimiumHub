package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.auth.WebAuthnChallenge;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface WebAuthnChallengeRepository extends JpaRepository<WebAuthnChallenge, UUID> {

    /**
     * Lê travando a linha até o fim da transação. Duas chamadas simultâneas com
     * o mesmo desafio leriam as duas "não usado" e passariam as duas; com a
     * trava, a segunda espera a primeira gravar o uso, e aí vê "usado".
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM WebAuthnChallenge c WHERE c.id = :id")
    Optional<WebAuthnChallenge> findForUpdate(@Param("id") UUID id);

    /**
     * Desafio vencido não serve para nada. O de login é criado por um endpoint
     * público, então a tabela cresce a cada toque em "Entrar com a digital" —
     * inclusive de quem só quer enchê-la.
     */
    @Modifying
    @Query("DELETE FROM WebAuthnChallenge c WHERE c.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}
