package com.proautokimium.api.Infrastructure.services.permission;

import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.repositories.permission.ScreenRepository;
import com.proautokimium.api.Infrastructure.repositories.permission.UserPermissionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * A tradução de linha de banco para authority do Spring.
 *
 * O formato importa mais do que parece: `@PreAuthorize("hasAuthority('...')")`
 * compara **string com string**. Uma diferença de dois-pontos, de caixa ou de
 * espaço não dá erro em lugar nenhum — só nega o acesso, e o sintoma é um 403
 * que ninguém sabe explicar.
 */
@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock private UserPermissionRepository repository;
    // Dependências novas da proteção do desenvolvedor. Sem elas o `@InjectMocks`
    // passa `null` e todo teste da classe morre em NPE — não só o do atalho.
    @Mock private UserRepository users;
    @Mock private ScreenRepository screens;
    @InjectMocks private PermissionService service;

    @Test
    @DisplayName("A authority sai exatamente no formato que o @PreAuthorize compara")
    void authoritySaiNoFormatoCerto() {
        when(repository.findAuthorities("u1")).thenReturn(List.of(
                "stock/movements:EXCLUIR",
                "stock/movements:CONSULTAR"));

        var authorities = service.authoritiesOf("u1");

        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .containsExactly("stock/movements:EXCLUIR", "stock/movements:CONSULTAR");
    }

    /**
     * Quem não tem nada não tem `null` — tem lista vazia.
     *
     * O `SecurityFilter` soma esta lista às roles. Um `null` aqui viraria
     * `NullPointerException` no meio do filtro, e o sintoma seria 500 em toda
     * requisição de quem ainda não foi configurado.
     */
    @Test
    @DisplayName("Sem permissão nenhuma, devolve lista vazia e não nulo")
    void semPermissaoDevolveVazio() {
        when(repository.findAuthorities("u1")).thenReturn(List.of());

        assertThat(service.authoritiesOf("u1")).isEmpty();
    }

    // ─── A conta que não se tranca ───────────────────────────────────────────

    /**
     * **O desenvolvedor tem tudo, e não consulta `user_permissions`.**
     *
     * Sem esta saída, a tela de permissões consegue trancar o próprio dono: um
     * "bloquear tudo" na pessoa errada e ninguém mais abre a configuração — o
     * mesmo impasse que a V87 resolveu no banco, agora alcançável por dois
     * cliques.
     */
    @Test
    @DisplayName("desenvolvedor recebe todas as telas vezes todas as permissões")
    void desenvolvedorTemTudo() {
        when(users.isDeveloper("u-dev")).thenReturn(true);
        when(screens.findByActiveTrueOrderByModuleAscSortOrderAsc())
                .thenReturn(List.of(tela("stock/movements"), tela("rh/hub")));

        var authorities = service.authoritiesOf("u-dev").stream()
                .map(GrantedAuthority::getAuthority).toList();

        assertThat(authorities)
                .as("duas telas vezes as sete permissões")
                .hasSize(14)
                .contains("stock/movements:EXCLUIR", "rh/hub:CONFIGURAR");
    }

    /**
     * E a tabela nem é lida.
     *
     * Se fosse, um desenvolvedor com a grade zerada perderia acesso — que é
     * exatamente o que esta proteção existe para impedir.
     */
    @Test
    @DisplayName("a grade do desenvolvedor não é consultada")
    void desenvolvedorNaoDependeDaTabela() {
        when(users.isDeveloper("u-dev")).thenReturn(true);
        when(screens.findByActiveTrueOrderByModuleAscSortOrderAsc()).thenReturn(List.of());

        service.authoritiesOf("u-dev");

        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never())
                .findAuthorities("u-dev");
    }

    private static com.proautokimium.api.domain.entities.permission.Screen tela(String code) {
        var screen = new com.proautokimium.api.domain.entities.permission.Screen();
        screen.setCode(code);
        screen.setLabel(code);
        screen.setModule("Teste");
        return screen;
    }
}
