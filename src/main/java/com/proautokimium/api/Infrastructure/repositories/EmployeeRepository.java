package com.proautokimium.api.Infrastructure.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.proautokimium.api.domain.entities.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
	Employee findByCodParceiro(String codParceiro);

    Optional<Employee> findByEmail_Address(String emailAdress);
    Optional<Employee> findByUsername(String username);

    /** Casa pelo CPF ignorando formatação (compara apenas os dígitos). */
    @Query("SELECT e FROM Employee e WHERE function('regexp_replace', e.documento, '[^0-9]', '', 'g') = :cpf")
    Optional<Employee> findByCpfDigits(@Param("cpf") String cpfDigits);
}
