package com.proautokimium.api.Infrastructure.repositories.events;

import com.proautokimium.api.domain.entities.events.CompanyEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CompanyEventRepository extends JpaRepository<CompanyEvent, UUID> {

    /** Documentos: só publicados. O mais recente primeiro; a tela agrupa. */
    @Query("select e from CompanyEvent e where e.publishedAt is not null order by e.startDate desc")
    List<CompanyEvent> findPublished();

    /** Cadastro: todos, rascunho incluído. */
    @Query("select e from CompanyEvent e order by e.startDate desc")
    List<CompanyEvent> findAllForManagement();
}
