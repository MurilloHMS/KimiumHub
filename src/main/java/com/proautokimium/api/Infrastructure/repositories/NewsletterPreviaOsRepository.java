package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.NewsletterPreviaOs;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NewsletterPreviaOsRepository extends JpaRepository<NewsletterPreviaOs, UUID> {

    List<NewsletterPreviaOs> findByPreviaId(UUID previaId);

    List<NewsletterPreviaOs> findByPreviaIdAndCodigoCliente(UUID previaId, String codigoCliente);

    Optional<NewsletterPreviaOs> findByPreviaIdAndNumeroOs(UUID previaId, int numeroOs);
}
