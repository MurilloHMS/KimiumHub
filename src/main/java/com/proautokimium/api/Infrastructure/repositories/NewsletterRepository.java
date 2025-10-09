package com.proautokimium.api.Infrastructure.repositories;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.proautokimium.api.domain.entities.Newsletter;
import com.proautokimium.api.domain.enums.EmailStatus;

public interface NewsletterRepository extends JpaRepository<Newsletter, UUID>{
	List<Newsletter> findAllByStatus(EmailStatus status);

	List<Newsletter> findAllByStatusIn(Collection<EmailStatus> status);
}
