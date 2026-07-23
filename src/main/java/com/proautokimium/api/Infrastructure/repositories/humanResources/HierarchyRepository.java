package com.proautokimium.api.Infrastructure.repositories.humanResources;

import com.proautokimium.api.domain.entities.humanResources.Hierarchy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HierarchyRepository extends JpaRepository<Hierarchy, UUID> {
}
