package com.proautokimium.api.Infrastructure.repositories.humanResources;

import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.EmployeeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EmployeeDocumentRepository extends JpaRepository<EmployeeDocument, UUID> {
    List<EmployeeDocument> findByEmployeeOrderByUploadedAtDesc(Employee employee);

    /**
     * A lista do RH. Filtros opcionais: nulo = todos. `join fetch` traz o
     * funcionário e o tipo na MESMA consulta — sem ele, cada linha da tela
     * dispararia mais dois SELECTs (o problema N+1).
     *
     * O status não filtra aqui: ele depende de "hoje" e do tipo, e é calculado
     * em Java pelo `statusOn`.
     */
    @Query("""
            select d from EmployeeDocument d
              join fetch d.employee
              left join fetch d.type
             where (:employeeId is null or d.employee.id = :employeeId)
               and (:typeId is null or d.type.id = :typeId)
             order by d.uploadedAt desc
            """)
    List<EmployeeDocument> search(@Param("employeeId") UUID employeeId, @Param("typeId") UUID typeId);

    /** Os do próprio funcionário, com o tipo junto (a tela "Meus documentos" mostra o nome dele). */
    @Query("""
            select d from EmployeeDocument d
              left join fetch d.type
             where d.employee.id = :employeeId
             order by d.uploadedAt desc
            """)
    List<EmployeeDocument> findMine(@Param("employeeId") UUID employeeId);

    /**
     * O documento ATIVO do mesmo tipo — o candidato a ser substituído quando o
     * RH vincula um ASO novo. "Ativo" = ninguém tomou o lugar dele ainda.
     */
    List<EmployeeDocument> findByEmployee_IdAndType_IdAndReplacedByIsNull(UUID employeeId, UUID typeId);

    /**
     * Quem pode gerar aviso: tem vencimento, não foi substituído, e o tipo está
     * ativo. Documento sem tipo (os da V59) não entra — não há de quem ler os
     * dias nem quem avisar.
     *
     * `join fetch` no funcionário e no tipo: o aviso usa os dois em cada
     * documento, e sem isso seriam dois SELECTs por linha.
     */
    @Query("""
            select d from EmployeeDocument d
              join fetch d.employee
              join fetch d.type t
             where d.dueDate is not null
               and d.replacedBy is null
               and t.active = true
            """)
    List<EmployeeDocument> findAlertCandidates();
}
