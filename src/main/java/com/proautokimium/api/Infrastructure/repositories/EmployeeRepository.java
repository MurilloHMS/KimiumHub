package com.proautokimium.api.Infrastructure.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.proautokimium.api.domain.entities.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
	Employee findByCodParceiro(String codParceiro);
}
