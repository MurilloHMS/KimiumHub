package com.proautokimium.api.Infrastructure.repositories.events;

import com.proautokimium.api.domain.entities.events.CompanyEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CompanyEventRepository extends JpaRepository<CompanyEvent, UUID> {

    /** Documentos: só publicados. O mais recente primeiro; a tela agrupa. */
    @Query("select e from CompanyEvent e where e.publishedAt is not null order by e.startDate desc")
    List<CompanyEvent> findPublished();

    /** Cadastro: todos, rascunho incluído. */
    @Query("select e from CompanyEvent e order by e.startDate desc")
    List<CompanyEvent> findAllForManagement();

    /**
     * Meus convites: os eventos publicados em que esta pessoa entra pelo público
     * — a mesma regra de {@code EmployeeRepository.findEventInvitees}, vista do
     * lado do funcionário. O mais recente primeiro.
     */
    @Query("""
            SELECT ev FROM CompanyEvent ev
            WHERE ev.publishedAt IS NOT NULL
              AND EXISTS (
                  SELECT e FROM Employee e LEFT JOIN e.team t
                  WHERE e.id = :employeeId
                    AND (ev.audienceAll = true
                         OR e.company MEMBER OF ev.audienceCompanies
                         OR t.department MEMBER OF ev.audienceDepartments
                         OR e MEMBER OF ev.audienceEmployees))
            ORDER BY ev.startDate DESC
            """)
    List<CompanyEvent> findInvitationsFor(@Param("employeeId") UUID employeeId);

    /**
     * Os candidatos ao lembrete de hoje: publicados, com o lembrete ligado, que
     * ainda não passaram do primeiro dia. A hora e o "antes de começar" exato
     * ({@code startsAt()}) o serviço confere — aqui é só o corte grosso.
     */
    @Query("""
            SELECT ev FROM CompanyEvent ev
            WHERE ev.publishedAt IS NOT NULL
              AND ev.reminderEnabled = true
              AND ev.startDate >= :today
            """)
    List<CompanyEvent> findReminderCandidates(@Param("today") LocalDate today);

    /**
     * Os candidatos ao "Começou agora" de hoje: online, publicados, com o aviso
     * ligado e ainda não enviado. O instante exato (entre o início e o fim) o
     * serviço confere.
     */
    @Query("""
            SELECT ev FROM CompanyEvent ev
            WHERE ev.publishedAt IS NOT NULL
              AND ev.locationType = com.proautokimium.api.domain.enums.events.EventLocationType.ONLINE
              AND ev.notifyLiveStart = true
              AND ev.liveStartNotifiedAt IS NULL
              AND ev.startDate <= :today
              AND ev.endDate >= :today
            """)
    List<CompanyEvent> findLiveStartCandidates(@Param("today") LocalDate today);

    /**
     * Marca o "Começou agora" como enviado, só se ainda não estava. Devolve 1
     * para quem ganhou a corrida e 0 para o resto: duas instâncias da API, ou
     * duas rodadas sobrepostas, não avisam duas vezes.
     */
    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE CompanyEvent ev SET ev.liveStartNotifiedAt = :now WHERE ev.id = :id AND ev.liveStartNotifiedAt IS NULL")
    int claimLiveStartNotice(@Param("id") UUID id, @Param("now") java.time.LocalDateTime now);
}
