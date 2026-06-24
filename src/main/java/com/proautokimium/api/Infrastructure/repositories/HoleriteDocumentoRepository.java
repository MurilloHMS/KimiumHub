package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.HoleriteDocumento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HoleriteDocumentoRepository extends JpaRepository<HoleriteDocumento, UUID> {

    List<HoleriteDocumento> findByEmployeeOrderByCompetenciaDesc(Employee employee);
}
