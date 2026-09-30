package com.proautokimium.api.Infrastructure.repositories.sales;

import com.proautokimium.api.domain.entities.sales.Checklist;
import com.proautokimium.api.domain.enums.sales.ChecklistStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ChecklistRepository extends JpaRepository<Checklist, UUID> {

    List<Checklist> findBySellerLoginOrderByLastSubmittedAtDesc(String sellerLogin);

    List<Checklist> findAllByOrderByLastSubmittedAtDesc();

    List<Checklist> findByStatusInOrderByLastSubmittedAtDesc(Collection<ChecklistStatus> statuses);
}
