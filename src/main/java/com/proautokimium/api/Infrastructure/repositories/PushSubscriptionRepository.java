package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.PushSubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscriptionEntity, UUID> {

    List<PushSubscriptionEntity> findByRecipientLogin(String recipientLogin);

    Optional<PushSubscriptionEntity> findByEndpoint(String endpoint);

    void deleteByEndpoint(String endpoint);
}
