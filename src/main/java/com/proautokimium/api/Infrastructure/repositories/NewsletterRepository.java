package com.proautokimium.api.Infrastructure.repositories;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.proautokimium.api.domain.entities.Newsletter;
import com.proautokimium.api.domain.enums.EmailStatus;

public interface NewsletterRepository extends JpaRepository<Newsletter, UUID>{
	List<Newsletter> findAllByStatus(EmailStatus status);

	List<Newsletter> findAllByStatusIn(Collection<EmailStatus> status);

	List<Newsletter> findTop15ByStatusIn(Collection<EmailStatus> status);

	List<Newsletter> findByCodigoClienteInAndStatusAndDataBetweenOrderByDataAsc(
            Collection<String> codigos, EmailStatus status, LocalDate from, LocalDate to);
}
