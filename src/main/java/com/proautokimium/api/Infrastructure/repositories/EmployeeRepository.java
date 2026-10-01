package com.proautokimium.api.Infrastructure.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.enums.humanResources.TransportType;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    /**
     * Lê o funcionário travando a linha até o fim da transação (SELECT ... FOR UPDATE).
     *
     * Para quem vai GRAVAR o saldo de férias: duas aprovações simultâneas liam
     * o mesmo saldo e cada uma gravava o seu desconto — um se perdia. Travado,
     * a segunda espera e lê o saldo já descontado.
     *
     * Trava em vez de @Version: o saldo mora em `parceiros` (SINGLE_TABLE), e o
     * Hibernate só aceita @Version na raiz, Partner — o que poria trava otimista
     * em todo cliente e vendedor, inclusive na conciliação com o Sankhya.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Employee e WHERE e.id = :id")
    Optional<Employee> findByIdForUpdate(@Param("id") UUID id);
	Employee findByCodParceiro(String codParceiro);

    /** Funcionarios num setor — usado antes de excluir o setor. */
    long countByTeamId(UUID teamId);

    /** Funcionarios numa hierarquia — usado antes de excluir a hierarquia. */
    long countByHierarquiaId(UUID hierarchyId);

    Optional<Employee> findByEmail_Address(String emailAdress);
    Optional<Employee> findByUsername(String username);
    List<Employee> findByAtivoTrue();

    /** Casa pelo CPF ignorando formatação (compara apenas os dígitos). */
    @Query("SELECT e FROM Employee e WHERE function('regexp_replace', e.documento, '[^0-9]', '', 'g') = :cpf")
    Optional<Employee> findByCpfDigits(@Param("cpf") String cpfDigits);

    @Query("SELECT e FROM Employee e WHERE function('regexp_replace', e.documento, '[^0-9]', '', 'g') = :cpf")
    List<Employee> findAllByCpfDigits(@Param("cpf") String cpfDigits);

    List<Employee> findByTransportTypeAndAtivoTrue(TransportType transportType);

    /**
     * Os convidados de um evento, calculados na hora: funcionários ativos, com
     * login ativo, que entram pelo público do evento — todos, ou a soma das
     * empresas, dos setores (pela equipe) e das pessoas escolhidas.
     *
     * <p>LEFT JOIN explícito na equipe: quem não tem equipe precisa continuar na
     * lista (convidado pela empresa ou pelo nome). Pela especificação do JPA, o
     * caminho com ponto ({@code e.team.department}) é INNER JOIN e tiraria essa
     * pessoa; o Hibernate 6.6 gera LEFT JOIN aqui (medido em 2026-10-01), mas é
     * escolha dele e já mudou entre versões. Escrito, não depende dela.
     *
     * <p>O {@code EXISTS} + {@code OR} pergunta "existe ao menos uma porta de
     * entrada?", e por isso ninguém sai repetido.
     */
    @Query("""
            SELECT e FROM Employee e
            LEFT JOIN e.team t
            WHERE e.ativo = true
              AND EXISTS (SELECT u FROM users u WHERE u.employee = e AND u.active = true)
              AND EXISTS (
                  SELECT ev FROM CompanyEvent ev
                  WHERE ev.id = :eventId
                    AND (ev.audienceAll = true
                         OR e.company MEMBER OF ev.audienceCompanies
                         OR t.department MEMBER OF ev.audienceDepartments
                         OR e MEMBER OF ev.audienceEmployees))
            ORDER BY e.name
            """)
    List<Employee> findEventInvitees(@Param("eventId") UUID eventId);

    /**
     * Quem pode ser escolhido a dedo como convidado: ativo e com login — a mesma
     * porta de entrada de {@link #findEventInvitees}. Escolher quem não entra
     * por ela seria uma escolha que não convida ninguém.
     */
    @Query("""
            SELECT e FROM Employee e
            WHERE e.ativo = true
              AND EXISTS (SELECT u FROM users u WHERE u.employee = e AND u.active = true)
            ORDER BY e.name
            """)
    List<Employee> findInvitable();

    /** A mesma regra para uma pessoa só: abrir o evento, responder, contar a visualização. */
    @Query("""
            SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END
            FROM Employee e
            LEFT JOIN e.team t
            WHERE e.id = :employeeId
              AND e.ativo = true
              AND EXISTS (SELECT u FROM users u WHERE u.employee = e AND u.active = true)
              AND EXISTS (
                  SELECT ev FROM CompanyEvent ev
                  WHERE ev.id = :eventId
                    AND (ev.audienceAll = true
                         OR e.company MEMBER OF ev.audienceCompanies
                         OR t.department MEMBER OF ev.audienceDepartments
                         OR e MEMBER OF ev.audienceEmployees))
            """)
    boolean isInvitedToEvent(@Param("eventId") UUID eventId, @Param("employeeId") UUID employeeId);
}
