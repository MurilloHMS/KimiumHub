package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.NewsletterPreviaCliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NewsletterPreviaClienteRepository extends JpaRepository<NewsletterPreviaCliente, UUID> {

    List<NewsletterPreviaCliente> findByPreviaIdOrderByFaturamentoTotalDesc(UUID previaId);

    Optional<NewsletterPreviaCliente> findByPreviaIdAndCodigoCliente(UUID previaId, String codigoCliente);

    List<NewsletterPreviaCliente> findByPreviaIdAndCodigoClienteIn(UUID previaId, List<String> codigos);
}
