package com.proautokimium.api.Infrastructure.repositories.permission;

import com.proautokimium.api.domain.entities.permission.TemplatePermission;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TemplatePermissionRepository
        extends JpaRepository<TemplatePermission, TemplatePermission.Key> {

    List<TemplatePermission> findByTemplateId(UUID templateId);

    /**
     * Cria as combinações que faltam para uma permissão, fechadas.
     *
     * Roda no boot, uma vez por valor do enum. O `WHERE NOT EXISTS` faz a
     * segunda execução não criar nada, então rodar sempre é seguro — e é o que
     * garante que tela nova não fique invisível quando alguém esquecer de
     * configurá-la.
     *
     * Uma permissão por chamada, e não as sete de uma vez: passar a lista
     * exigiria `UNNEST` com array, que é do Postgres, e os testes rodam em H2.
     * Sete comandos idempotentes no boot custam menos que manter duas variantes
     * de SQL que só uma delas é exercitada.
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO template_permissions (template_id, screen_code, permission, allowed)
        SELECT t.id, s.code, :permission, FALSE
          FROM permission_templates t
         CROSS JOIN screens s
         WHERE NOT EXISTS (
               SELECT 1 FROM template_permissions x
                WHERE x.template_id = t.id
                  AND x.screen_code = s.code
                  AND x.permission  = :permission
         )
        """, nativeQuery = true)
    int createMissing(@Param("permission") String permission);

    List<TemplatePermission> findByTemplateIdAndAllowedTrue(UUID templateId);

    /**
     * Quantas células cada modelo tem ligadas.
     *
     * É o "9 telas" da lista lateral. Agrupada pelo mesmo motivo da contagem de
     * carimbos: desenhar onze linhas não pode custar onze consultas.
     */
    @Query("""
        SELECT tp.templateId AS templateId, COUNT(tp) AS total
          FROM TemplatePermission tp
         WHERE tp.allowed = true
         GROUP BY tp.templateId
    """)
    List<Count> countAllowedByTemplate();

    /** O par que a consulta agrupada devolve. */
    interface Count {
        UUID getTemplateId();
        long getTotal();
    }
}
