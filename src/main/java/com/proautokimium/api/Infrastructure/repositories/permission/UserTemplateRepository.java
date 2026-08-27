package com.proautokimium.api.Infrastructure.repositories.permission;

import com.proautokimium.api.domain.entities.permission.UserTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface UserTemplateRepository extends JpaRepository<UserTemplate, UserTemplate.Key> {

    List<UserTemplate> findByUserId(String userId);

    /**
     * Apaga o histórico de carimbos de uma pessoa.
     *
     * Só o "copiar de outro usuário" usa: copiar a grade sem copiar por onde
     * ela passou deixaria a tela apontando divergência em toda célula, porque
     * o esperado seria calculado a partir dos carimbos errados.
     */
    void deleteByUserId(String userId);

    /**
     * Quantas pessoas cada modelo já carimbou.
     *
     * Uma consulta agrupada em vez de uma por modelo: a tela de modelos mostra
     * os onze de uma vez, e onze consultas para desenhar uma lista é o começo
     * do N+1 que ninguém percebe enquanto são onze.
     */
    @Query("""
        SELECT ut.templateId AS templateId, COUNT(ut) AS total
          FROM UserTemplate ut
         GROUP BY ut.templateId
    """)
    List<Count> countByTemplate();

    /** O par que a consulta agrupada devolve. */
    interface Count {
        UUID getTemplateId();
        long getTotal();
    }
}
