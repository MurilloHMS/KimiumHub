package com.proautokimium.api.Infrastructure.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.proautokimium.api.domain.entities.Newsletter;

public interface NewsletterRepository extends JpaRepository<Newsletter, UUID>{

}
