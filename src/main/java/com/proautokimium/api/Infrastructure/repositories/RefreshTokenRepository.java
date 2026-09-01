package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.auth.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * O token que chegou na renovação, procurado pelo HASH.
     *
     * O valor cru nunca sai do navegador de quem tem a sessão: o que a tabela
     * guarda é o SHA-256 dele, e é por ele que se procura.
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Os tokens ainda vivos de uma pessoa.
     *
     * Usado no logout e na detecção de reuso — nos dois casos a ação é a mesma:
     * revogar tudo o que ainda vale. `IsNull` nas duas datas é o equivalente do
     * `UsedFalse` do modelo antigo, agora que a ausência da data é o que
     * significa "não aconteceu".
     */
    List<RefreshToken> findByUserIdAndUsedAtIsNullAndRevokedAtIsNull(String userId);
}
