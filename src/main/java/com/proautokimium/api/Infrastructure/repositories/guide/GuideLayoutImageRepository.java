package com.proautokimium.api.Infrastructure.repositories.guide;

import com.proautokimium.api.domain.entities.guide.GuideLayoutImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GuideLayoutImageRepository extends JpaRepository<GuideLayoutImage, UUID> {
}
