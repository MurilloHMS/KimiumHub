package com.proautokimium.api.Infrastructure.repositories.permission;

import com.proautokimium.api.domain.entities.permission.PermissionTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PermissionTemplateRepository extends JpaRepository<PermissionTemplate, UUID> {

    Optional<PermissionTemplate> findByName(String name);

    List<PermissionTemplate> findByActiveTrueOrderByNameAsc();
}