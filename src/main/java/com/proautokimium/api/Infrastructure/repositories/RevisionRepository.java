package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.Revision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RevisionRepository extends JpaRepository<Revision, UUID> {
    @Query(value = "SELECT * FROM revision WHERE vehicle_id = :id", nativeQuery = true)
    List<Revision> findRevisionByVehicleId(@Param("id") UUID id);
}
