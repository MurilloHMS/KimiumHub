package com.proautokimium.api.Infrastructure.repositories.humanResources;

import com.proautokimium.api.domain.entities.humanResources.HrReportRecipient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HrReportRecipientRepository extends JpaRepository<HrReportRecipient, UUID> {
    List<HrReportRecipient> findAllByOrderByEmailAsc();
    boolean existsByEmail(String email);
}
