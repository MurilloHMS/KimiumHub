package com.proautokimium.api.Infrastructure.services.permission;

import com.proautokimium.api.domain.enums.Permission;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O catálogo é lido dos `@PreAuthorize` de verdade, sem Spring e sem banco.
 *
 * Por isso o teste afirma sobre telas reais: se a varredura parar de enxergar
 * as anotações de método, a grade mostraria só "Ver" em toda tela, e ninguém
 * conseguiria liberar Incluir em lugar nenhum.
 */
class ScreenActionCatalogTest {

    private final ScreenActionCatalog catalog = new ScreenActionCatalog();

    @Test
    @DisplayName("lê as ações das anotações de método, inclusive as montadas com constante")
    void leAsAnotacoesDeMetodo() {
        assertThat(catalog.actionsOf("stock/products"))
                .contains(Permission.INCLUIR, Permission.ALTERAR, Permission.EXCLUIR);

        // PermissionAdminController monta as authorities com `ADMIN + ":..."`.
        assertThat(catalog.actionsOf("settings/admin")).containsExactly(
                Permission.ALTERAR, Permission.CONSULTAR, Permission.CONFIGURAR, Permission.INCLUIR);
    }

    @Test
    @DisplayName("tela que nenhuma anotação cita aparece só com Ver")
    void telaSemAnotacaoSoVer() {
        assertThat(catalog.actionsOf("tela/que-nao-existe")).containsExactly(Permission.CONSULTAR);
    }

    @Test
    @DisplayName("as telas antigas de permissão não são mais exigidas por nenhum endpoint")
    void telasAntigasSairam() {
        assertThat(catalog.all()).doesNotContainKeys(
                "settings/permissions/users", "settings/permissions/templates");
    }

    @Test
    @DisplayName("a ordem é a do enum, a mesma da grade")
    void ordemDoEnum() {
        List<Permission> acoes = catalog.actionsOf("stock/products");
        assertThat(acoes).isSortedAccordingTo(Enum::compareTo);
    }
}
