package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {
    Optional<Profile> findBySlug(String slug);
    Optional<Profile> findBySlugAndAtivoTrue(String slug);
    boolean existsBySlug(String slug);
}