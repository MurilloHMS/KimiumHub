package com.proautokimium.api.Infrastructure.services.permission;

import com.proautokimium.api.Infrastructure.repositories.permission.TemplatePermissionRepository;
import com.proautokimium.api.Infrastructure.repositories.permission.UserPermissionRepository;
import com.proautokimium.api.domain.enums.Permission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mantém "todas as combinações existem" verdadeiro sozinho.
 *
 * Sem isto, uma tela nova no catálogo não tem linha em `template_permissions`
 * nem em `user_permissions` — e some para todo mundo, inclusive para quem a
 * escreveu. O sintoma é "sumiu do menu", que ninguém associa a permissão.
 *
 * A alternativa seria criar as linhas junto com a tela, na própria migration.
 * Só que isso cobre quem lembra de escrever, e o esquecimento é justamente o
 * problema. Aqui o remendo acontece mesmo quando ninguém lembrou.
 *
 * **Nasce tudo negado**, que é o comportamento certo para o padrão "negar": a
 * tela aparece no grid de configuração fechada, esperando alguém liberar.
 */
@Service
public class PermissionSyncService {

    private static final Logger log = LoggerFactory.getLogger(PermissionSyncService.class);

    private final TemplatePermissionRepository templatePermissions;
    private final UserPermissionRepository userPermissions;

    public PermissionSyncService(TemplatePermissionRepository templatePermissions,
                                 UserPermissionRepository userPermissions) {
        this.templatePermissions = templatePermissions;
        this.userPermissions = userPermissions;
    }

    /**
     * Roda no boot, depois de a aplicação estar de pé.
     *
     * `ApplicationReadyEvent` e não `@PostConstruct`: o segundo roda antes de o
     * contexto terminar, e uma falha ali derruba a subida por causa de um
     * remendo — que é o contrário do que este serviço existe para fazer.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        try {
            Result resultado = sync();
            if (resultado.total() > 0) {
                log.info("Permissões sincronizadas: {} em modelos, {} em usuários.",
                        resultado.templateRows(), resultado.userRows());
            }
        } catch (Exception e) {
            // Não derruba a aplicação: sem a sincronização o sistema funciona,
            // só não enxerga tela nova. Derrubar por causa disso trocaria um
            // problema pequeno e visível por um grande.
            log.error("Falha ao sincronizar permissões. Tela nova pode estar invisível.", e);
        }
    }

    /**
     * Cria o que falta, e devolve quanto criou.
     *
     * Idempotente por construção — o `WHERE NOT EXISTS` de cada consulta faz a
     * segunda execução não criar nada. Rodar em toda instância que sobe é
     * seguro.
     */
    @Transactional
    public Result sync() {
        int emModelos = 0;
        int emUsuarios = 0;

        // Uma permissão por vez: passar as sete juntas exigiria `UNNEST` com
        // array, que é do Postgres, e os testes rodam em H2.
        for (Permission permission : Permission.values()) {
            emModelos  += templatePermissions.createMissing(permission.name());
            emUsuarios += userPermissions.createMissing(permission.name());
        }

        return new Result(emModelos, emUsuarios);
    }

    /** Quanto a sincronização criou, para o log e para o teste afirmarem. */
    public record Result(int templateRows, int userRows) {
        public int total() {
            return templateRows + userRows;
        }
    }
}
