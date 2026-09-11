package com.proautokimium.api.Infrastructure.repositories.processoSeletivo;

import com.proautokimium.api.domain.entities.processoSeletivo.Candidato;
import com.proautokimium.api.domain.entities.processoSeletivo.Vaga;
import com.proautokimium.api.domain.valueObjects.Email;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CandidatoRepository extends JpaRepository<Candidato, UUID> {
    Optional<Candidato> findByEmail(Email email);

    /**
     * Busca insensivel a caixa, e ela e obrigatoria desde a V103.
     *
     * <p>O indice unico e sobre {@code lower(email)}. Com a busca sensivel a
     * caixa, {@code Joao@x.com} nao encontraria o {@code joao@x.com} que ja
     * existe, tentaria inserir, e bateria no indice — <b>500 num endpoint
     * publico</b>. Alem disso, a pessoa que voltasse digitando com outra caixa
     * nunca alcancaria o proprio cadastro.
     *
     * <p>O {@code _Address} navega para dentro do {@code @Embedded Email}.
     */
    Optional<Candidato> findByEmail_AddressIgnoreCase(String address);
}
