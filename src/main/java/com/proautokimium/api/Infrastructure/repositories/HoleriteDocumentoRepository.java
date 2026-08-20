package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.HoleriteDocumento;
import com.proautokimium.api.domain.enums.HoleriteTipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface HoleriteDocumentoRepository extends JpaRepository<HoleriteDocumento, UUID> {

    List<HoleriteDocumento> findByEmployeeOrderByCompetenciaDesc(Employee employee);

    /**
     * Quem já tem holerite desta competência e tipo.
     *
     * Uma consulta para o lote inteiro, e não um `exists` por página: a busca
     * por CPF já faz `regexp_replace` sem índice, e somar N desses num PDF de
     * 200 páginas deixa o envio lento a ponto de ninguém usar.
     */
    @Query("SELECT h.employee.id FROM HoleriteDocumento h WHERE h.competencia = :competencia AND h.tipo = :tipo")
    Set<UUID> findEmployeeIdsByCompetenciaAndTipo(@Param("competencia") LocalDate competencia,
                                                  @Param("tipo") HoleriteTipo tipo);

    /** A tela do funcionário não mostra cancelado. Ele continua na auditoria. */
    List<HoleriteDocumento> findByEmployeeAndCanceledAtIsNullOrderByCompetenciaDesc(Employee employee);

    /**
     * A grade da auditoria. O join fetch evita N+1: sem ele, cada linha faria
     * uma consulta para ler o nome do funcionário.
     */
    @Query("""
        SELECT h FROM HoleriteDocumento h
        JOIN FETCH h.employee e
        WHERE h.competencia = :competencia AND h.tipo = :tipo
        ORDER BY e.name
    """)
    List<HoleriteDocumento> findParaAuditoria(@Param("competencia") LocalDate competencia,
                                              @Param("tipo") HoleriteTipo tipo);
}
