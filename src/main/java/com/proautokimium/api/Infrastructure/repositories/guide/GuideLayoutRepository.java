package com.proautokimium.api.Infrastructure.repositories.guide;

import com.proautokimium.api.domain.entities.guide.GuideLayout;
import com.proautokimium.api.domain.enums.guide.GuideLayoutStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GuideLayoutRepository extends JpaRepository<GuideLayout, UUID> {

    Optional<GuideLayout> findFirstByStatus(GuideLayoutStatus status);

    List<GuideLayout> findByStatusInOrderByVersionDesc(Collection<GuideLayoutStatus> statuses);

    @Query("select coalesce(max(g.version), 0) from GuideLayout g")
    int findMaxVersion();
}
