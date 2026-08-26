package com.proautokimium.api.Infrastructure.services.permission;

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
}
