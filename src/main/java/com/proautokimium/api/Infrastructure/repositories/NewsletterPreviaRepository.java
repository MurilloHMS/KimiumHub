package com.proautokimium.api.Infrastructure.repositories;

import com.proautokimium.api.domain.entities.NewsletterPrevia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NewsletterPreviaRepository extends JpaRepository<NewsletterPrevia, UUID> {

    /**
     * O rascunho do mês, confirmado ou não.
     *
     * Um por período — a chave única na tabela garante isso, e é o que faz
     * "pedir a prévia do mesmo mês duas vezes" devolver o mesmo rascunho em vez
     * de criar outro por baixo.
     */
    Optional<NewsletterPrevia> findByMesAndAno(int mes, int ano);
}
