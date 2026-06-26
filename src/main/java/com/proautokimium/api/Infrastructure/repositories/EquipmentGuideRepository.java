package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.EquipmentGuide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repositório JPA para {@link EquipmentGuide}.
 */
@Repository
public interface EquipmentGuideRepository extends JpaRepository<EquipmentGuide, UUID> {

    /** Remove os vínculos do equipamento com produtos (tabela de junção) antes de excluí-lo. */
    @Modifying
    @Query(value = "DELETE FROM product_equipment WHERE equipment_id = :id", nativeQuery = true)
    void deleteProductLinks(@Param("id") UUID id);
}
