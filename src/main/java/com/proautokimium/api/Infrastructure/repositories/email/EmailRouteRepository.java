package com.proautokimium.api.Infrastructure.repositories.email;

import com.proautokimium.api.domain.entities.email.EmailRoute;
import com.proautokimium.api.domain.enums.email.EmailOrigin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EmailRouteRepository extends JpaRepository<EmailRoute, EmailOrigin> {

    /** Usado pela regra "remetente em uso não se desativa". */
    boolean existsBySender_IdOrReplyTo_Id(UUID senderId, UUID replyToId);
}
