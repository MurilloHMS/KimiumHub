package com.proautokimium.api.Infrastructure.repositories.humanResources;

import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.Team;
import com.proautokimium.api.domain.entities.humanResources.VacationRequest;
import com.proautokimium.api.domain.enums.humanResources.VacationRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VacationRequestRepository extends JpaRepository<VacationRequest, UUID> {

    List<VacationRequest> findByEmployeeOrderByRequestedAtDesc(Employee employee);

    /**
     * Lê o pedido travando a linha até o fim da transação (SELECT ... FOR UPDATE).
     *
     * Aprovar e reprovar ao mesmo tempo liam os dois PENDING: o último a
     * gravar vencia, e o pedido podia terminar REPROVADO com o saldo já
     * descontado. Travado, o segundo espera e encontra o status já mudado.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT vr FROM VacationRequest vr WHERE vr.id = :id")
    Optional<VacationRequest> findByIdForUpdate(@Param("id") UUID id);
    List<VacationRequest> findByStatus(VacationRequestStatus status);
    List<VacationRequest> findAllByOrderByRequestedAtDesc();
    List<VacationRequest> findByStatusOrderByRequestedAtDesc(VacationRequestStatus status);

    /** Solicitações do status dado cujo período se sobrepõe ao intervalo — usado pelo calendário. */
    List<VacationRequest> findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            VacationRequestStatus status, LocalDate rangeEnd, LocalDate rangeStart);

    List<VacationRequest> findByEmployeeAndStatus(Employee employee, VacationRequestStatus status);

    /**
     * O próprio funcionário já tem férias, nos status dados, que cruzam o período?
     *
     * A consulta do setor abaixo exclui o próprio funcionário de propósito, e
     * só roda para quem tem time — então o mesmo pedido podia ser feito duas
     * vezes, e aprovar os dois descontava o saldo duas vezes.
     *
     * Os status vêm de quem chama: criar confere PENDING e APPROVED; aprovar
     * confere só APPROVED, e assim o pedido que está sendo aprovado (ainda
     * PENDING) não conflita consigo mesmo.
     */
    @Query("""
            SELECT COUNT(vr) > 0 FROM VacationRequest vr
            WHERE vr.employee = :employee
            AND vr.status IN :statuses
            AND vr.startDate <= :endDate
            AND vr.endDate >= :startDate
            """)
    boolean existsOverlapForEmployee(
            @Param("employee") Employee employee,
            @Param("statuses") List<VacationRequestStatus> statuses,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Solicitações PENDING/APPROVED de outros funcionários do mesmo Setor cujo período
     * se sobrepõe ao informado — usado pro bloqueio rígido de férias simultâneas.
     */
    @Query("""
            SELECT vr FROM VacationRequest vr
            WHERE vr.employee.team = :team
            AND vr.employee <> :employee
            AND vr.status IN (com.proautokimium.api.domain.enums.humanResources.VacationRequestStatus.PENDING,
                               com.proautokimium.api.domain.enums.humanResources.VacationRequestStatus.APPROVED)
            AND vr.startDate <= :endDate
            AND vr.endDate >= :startDate
            """)
    List<VacationRequest> findOverlappingInTeam(
            @Param("team") Team team,
            @Param("employee") Employee employee,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
