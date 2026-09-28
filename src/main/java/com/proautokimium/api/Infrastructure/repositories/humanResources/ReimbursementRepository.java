package com.proautokimium.api.Infrastructure.repositories.humanResources;

import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.Reimbursement;
import com.proautokimium.api.domain.enums.humanResources.ReimbursementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

import java.util.List;
import java.util.UUID;

public interface ReimbursementRepository extends JpaRepository<Reimbursement, UUID> {
    List<Reimbursement> findByEmployeeOrderByRequestedAtDesc(Employee employee);
    List<Reimbursement> findByStatusOrderByRequestedAtDesc(ReimbursementStatus status);
    List<Reimbursement> findAllByOrderByRequestedAtDesc();

    /** Totais do mês e grade filtrada por mês: pela data da despesa, como o comprovante. */
    List<Reimbursement> findByExpenseDateBetween(LocalDate from, LocalDate to);
    List<Reimbursement> findByEmployeeAndExpenseDateBetween(Employee employee, LocalDate from, LocalDate to);
    List<Reimbursement> findByExpenseDateBetweenOrderByRequestedAtDesc(LocalDate from, LocalDate to);
    List<Reimbursement> findByStatusAndExpenseDateBetweenOrderByRequestedAtDesc(
            ReimbursementStatus status, LocalDate from, LocalDate to);

    /**
     * Linhas do comprovante para a diretoria: período pela DATA DA DESPESA,
     * status escolhidos na tela. Traz funcionário e revisor no mesmo SELECT —
     * sem o JOIN FETCH, cada linha do PDF custaria duas consultas a mais.
     *
     * Duas consultas (todos / um funcionário) em vez de `:employee IS NULL`:
     * o Postgres não sabe o tipo de um parâmetro nulo e recusa a consulta.
     */
    @Query("""
            SELECT r FROM Reimbursement r
            JOIN FETCH r.employee e
            LEFT JOIN FETCH r.reviewedBy
            WHERE r.expenseDate BETWEEN :from AND :to
            AND r.status IN :statuses
            ORDER BY e.name, r.expenseDate, r.requestedAt
            """)
    List<Reimbursement> findForReport(@Param("from") LocalDate from,
                                      @Param("to") LocalDate to,
                                      @Param("statuses") List<ReimbursementStatus> statuses);

    @Query("""
            SELECT r FROM Reimbursement r
            JOIN FETCH r.employee e
            LEFT JOIN FETCH r.reviewedBy
            WHERE r.employee = :employee
            AND r.expenseDate BETWEEN :from AND :to
            AND r.status IN :statuses
            ORDER BY r.expenseDate, r.requestedAt
            """)
    List<Reimbursement> findForReportByEmployee(@Param("employee") Employee employee,
                                                @Param("from") LocalDate from,
                                                @Param("to") LocalDate to,
                                                @Param("statuses") List<ReimbursementStatus> statuses);
}
