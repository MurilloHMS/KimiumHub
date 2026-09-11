package com.proautokimium.api.Infrastructure.repositories.processoSeletivo;

import com.proautokimium.api.domain.entities.processoSeletivo.Vaga;
import com.proautokimium.api.domain.enums.processoSeletivo.StatusVaga;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface VagaRepository extends JpaRepository<Vaga, UUID> {
    List<Vaga> findByStatus(StatusVaga status);
    List<Vaga> findAllByDataEncerramentoBeforeAndStatus(LocalDateTime data, StatusVaga status);

    /**
     * As areas distintas, de TODAS as vagas -- nao so das publicadas.
     *
     * <p>O site montava esta lista no navegador, a partir das vagas
     * publicadas. Resultado: ela vinha <b>vazia</b> quando nao havia vaga
     * aberta, que e exatamente quando o cadastro espontaneo importa.
     */
    @org.springframework.data.jpa.repository.Query(
        "select distinct v.area from Vaga v where v.area is not null and v.area <> '' order by v.area")
    List<String> listarAreas();
}
