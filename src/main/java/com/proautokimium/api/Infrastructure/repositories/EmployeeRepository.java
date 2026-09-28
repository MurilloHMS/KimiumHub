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
}
