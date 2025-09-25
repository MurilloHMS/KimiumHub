package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.Contact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ContactRepository extends JpaRepository<Contact, UUID> {
}
