package com.proautokimium.api.Infrastructure.services.permission;

import com.proautokimium.api.Infrastructure.repositories.permission.TemplatePermissionRepository;
import com.proautokimium.api.Infrastructure.repositories.permission.UserPermissionRepository;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.entities.permission.PermissionTemplate;
import com.proautokimium.api.domain.entities.permission.Screen;
import com.proautokimium.api.domain.enums.Permission;
import com.proautokimium.api.domain.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A rede que impede a tela invisível.
 *
 * Uma tela nova sem linha em `template_permissions` e `user_permissions` some
 * para todo mundo — inclusive para quem a escreveu — e o sintoma é "sumiu do
 * menu", que ninguém associa a permissão.
 *
 * O que estes testes protegem não é o `INSERT`: é ele continuar **idempotente**
 * e continuar **deixando o cliente de fora**. As duas coisas quebram em
 * silêncio, e uma delas dá acesso indevido.
 */
@DataJpaTest
@ActiveProfiles("test")
class PermissionSyncServiceTest {

    @Autowired private TestEntityManager entityManager;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TemplatePermissionRepository templatePermissions;
    @Autowired private UserPermissionRepository userPermissions;

    private PermissionSyncService service;

    private static final int PERMISSOES = Permission.values().length;

    @BeforeEach
    void setUp() {
        // Construído na mão: o `@DataJpaTest` só monta a camada de dados, e o
        // serviço não tem dependência além dos dois repositórios.
        service = new PermissionSyncService(templatePermissions, userPermissions);
    }

    private Screen tela(String code) {
        Screen screen = new Screen();
        screen.setCode(code);
        screen.setLabel(code);
        screen.setModule("Teste");
        screen.setSortOrder(10);
        return entityManager.persist(screen);
    }

    private PermissionTemplate modelo(String name) {
        PermissionTemplate template = new PermissionTemplate();
        template.setName(name);
        return entityManager.persist(template);
    }

    private User usuario(String login, UserRole role) {
        User user = new User(login, login + "@teste.com", "Senha123@", List.of(role));
        return entityManager.persist(user);
    }

    private int linhasDoUsuario(String login) {
        return jdbc.queryForObject("""
            SELECT count(*) FROM user_permissions up
             JOIN users u ON u.id = up.user_id
             WHERE u.login = ?
            """, Integer.class, login);
    }

    // ─── O que ela cria ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Cria as sete permissões de cada tela, para modelo e usuário")
    void criaAsSetePorTela() {
        tela("stock/devolucoes");
        modelo("Almoxarifado");
        usuario("funcionario", UserRole.USER);
        entityManager.flush();

        var resultado = service.sync();

        assertThat(resultado.templateRows()).isEqualTo(PERMISSOES);
        assertThat(resultado.userRows()).isEqualTo(PERMISSOES);
    }

    /**
     * **Nasce tudo negado.**
     *
     * É o comportamento certo para o padrão "negar": a tela aparece no grid de
     * configuração fechada, esperando alguém liberar. Se nascesse permitida, uma
     * tela nova ficaria aberta para a empresa inteira até alguém reparar.
     */
    @Test
    @DisplayName("As linhas nascem negadas")
    void nasceTudoNegado() {
        tela("rh/holerit");
        modelo("RH");
        entityManager.flush();

        service.sync();

        Integer permitidas = jdbc.queryForObject(
            "SELECT count(*) FROM template_permissions WHERE allowed", Integer.class);
        assertThat(permitidas).isZero();
    }

    /**
     * **O teste que permite rodar isto em todo boot.**
     *
     * Sem a idempotência, cada instância que sobe duplicaria as linhas — e a
     * chave composta transformaria isso num erro no boot, não num aviso.
     */
    @Test
    @DisplayName("Rodar de novo não cria nada")
    void rodarDeNovoNaoCriaNada() {
        tela("stock/hub");
        modelo("Base");
        usuario("funcionario", UserRole.USER);
        entityManager.flush();

        service.sync();
        var segunda = service.sync();

        assertThat(segunda.total()).isZero();
    }

    /**
     * **Cliente fica de fora, e isto é sobre acesso indevido.**
     *
     * O portal do cliente tem sessão e escopo próprios, decididos pela API. Dar
     * linhas de tela de ERP a um cliente seria dizer que ele participa deste
     * sistema — e o dia em que alguém liberasse uma tela "para todos", ele
     * entraria junto.
     */
    @Test
    @DisplayName("Cliente não recebe linha nenhuma")
    void clienteNaoRecebeLinha() {
        tela("rh/employees");
        modelo("Base");
        usuario("funcionario", UserRole.USER);
        usuario("cliente", UserRole.CLIENTE);
        entityManager.flush();

        service.sync();

        assertThat(linhasDoUsuario("funcionario")).isEqualTo(PERMISSOES);
        assertThat(linhasDoUsuario("cliente")).isZero();
    }

    /**
     * O caso real: o sistema está rodando, alguém cadastra uma tela, e a
     * sincronização do próximo boot precisa alcançar **só ela**.
     */
    @Test
    @DisplayName("Tela cadastrada depois recebe as linhas na próxima sincronização")
    void telaNovaEhAlcancada() {
        tela("stock/hub");
        modelo("Base");
        entityManager.flush();
        service.sync();

        tela("stock/devolucoes");
        entityManager.flush();

        var segunda = service.sync();

        // Só as sete da tela nova, não as quatorze das duas.
        assertThat(segunda.templateRows()).isEqualTo(PERMISSOES);
    }
}
