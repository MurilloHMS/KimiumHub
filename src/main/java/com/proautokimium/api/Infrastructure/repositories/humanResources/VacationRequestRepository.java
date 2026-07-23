package com.proautokimium.api.Infrastructure.repositories.humanResources;

import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.Team;
import com.proautokimium.api.domain.entities.humanResources.VacationRequest;
import com.proautokimium.api.domain.enums.humanResources.VacationRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface VacationRequestRepository extends JpaRepository<VacationRequest, UUID> {

    List<VacationRequest> findByEmployeeOrderByRequestedAtDesc(Employee employee);
    List<VacationRequest> findByStatus(VacationRequestStatus status);

    /** Solicitações do status dado cujo período se sobrepõe ao intervalo — usado pelo calendário. */
    List<VacationRequest> findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            VacationRequestStatus status, LocalDate rangeEnd, LocalDate rangeStart);

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
