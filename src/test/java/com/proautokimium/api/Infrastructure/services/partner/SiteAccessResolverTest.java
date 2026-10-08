package com.proautokimium.api.Infrastructure.services.partner;

import com.proautokimium.api.Application.DTOs.partners.EmployeeSiteAccess;
import com.proautokimium.api.Infrastructure.repositories.FirstAccessTokenRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.auth.FirstAccessToken;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.enums.SiteAccess;
import com.proautokimium.api.domain.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A situação de cada funcionário no site: com acesso, bloqueado ou pendente.
 *
 * Quem decide é a tabela de contas (`users.employee_id`) e a dos códigos de
 * primeiro acesso não usados. O teste passa listas prontas para os dois
 * repositórios e confere o que sai para cada funcionário.
 */
class SiteAccessResolverTest {

    private final UserRepository users = mock(UserRepository.class);
    private final FirstAccessTokenRepository tokens = mock(FirstAccessTokenRepository.class);
    private final SiteAccessResolver resolver = new SiteAccessResolver(users, tokens);

    // ─── Fixtures ────────────────────────────────────────────────────────────

    private static Employee funcionario(String nome) {
        Employee employee = new Employee();
        employee.id = UUID.randomUUID();
        employee.setName(nome);
        employee.setAtivo(true);
        return employee;
    }

    private static User conta(String login, Employee employee, boolean ativa) {
        User user = new User(login, login + "@kimium.com", "hash", List.of(UserRole.USER));
        user.setEmployee(employee);
        user.setActive(ativa);
        return user;
    }

    /** Um código pedido e não usado. A validade é 30 minutos depois do pedido. */
    private static FirstAccessToken codigo(Employee employee, LocalDateTime pedidoEm) {
        FirstAccessToken token = new FirstAccessToken();
        token.setPartner(employee);
        token.setExpiration(pedidoEm.plusMinutes(30));
        return token;
    }

    private Map<UUID, EmployeeSiteAccess> resolver(List<Employee> employees, List<User> contas, List<FirstAccessToken> codigos) {
        when(users.findAllWithEmployee()).thenReturn(contas);
        when(tokens.findByUsedFalse()).thenReturn(codigos);
        return resolver.resolve(employees);
    }

    // ─── As situações ────────────────────────────────────────────────────────

    @Test
    @DisplayName("quem tem conta ativa está com acesso, e o login vem junto")
    void contaAtivaEstaComAcesso() {
        Employee ricardo = funcionario("Ricardo Lima");

        EmployeeSiteAccess situacao = resolver(List.of(ricardo), List.of(conta("ricardo", ricardo, true)), List.of())
                .get(ricardo.getId());

        assertThat(situacao.status()).isEqualTo(SiteAccess.ACTIVE);
        assertThat(situacao.login()).isEqualTo("ricardo");
        assertThat(situacao.firstAccessRequestedAt()).isNull();
    }

    @Test
    @DisplayName("quem tem conta bloqueada está bloqueado, com o login")
    void contaBloqueada() {
        Employee carlos = funcionario("Carlos Dias");

        EmployeeSiteAccess situacao = resolver(List.of(carlos), List.of(conta("carlos", carlos, false)), List.of())
                .get(carlos.getId());

        assertThat(situacao.status()).isEqualTo(SiteAccess.BLOCKED);
        assertThat(situacao.login()).isEqualTo("carlos");
    }

    @Test
    @DisplayName("sem conta e sem código pedido: pendente, sem data")
    void nuncaEntrou() {
        Employee diego = funcionario("Diego Martins");

        EmployeeSiteAccess situacao = resolver(List.of(diego), List.of(), List.of()).get(diego.getId());

        assertThat(situacao.status()).isEqualTo(SiteAccess.PENDING);
        assertThat(situacao.login()).isNull();
        assertThat(situacao.firstAccessRequestedAt()).isNull();
    }

    /**
     * **O detalhe que o RH usa para cobrar.** O código não guarda quando foi
     * pedido, só quando vence; o pedido é a validade menos os 30 minutos.
     */
    @Test
    @DisplayName("sem conta, com código pedido e não usado: pendente, com a hora do pedido")
    void pediuONaoConcluiu() {
        Employee bruna = funcionario("Bruna Teixeira");
        LocalDateTime pedido = LocalDateTime.of(2026, 10, 3, 9, 12);

        EmployeeSiteAccess situacao = resolver(List.of(bruna), List.of(), List.of(codigo(bruna, pedido)))
                .get(bruna.getId());

        assertThat(situacao.status()).isEqualTo(SiteAccess.PENDING);
        assertThat(situacao.firstAccessRequestedAt()).isEqualTo(pedido);
    }

    @Test
    @DisplayName("pediu o código mais de uma vez: vale o pedido mais recente")
    void valeOPedidoMaisRecente() {
        Employee bruna = funcionario("Bruna Teixeira");
        LocalDateTime primeiro = LocalDateTime.of(2026, 10, 1, 8, 0);
        LocalDateTime ultimo = LocalDateTime.of(2026, 10, 3, 9, 12);

        EmployeeSiteAccess situacao = resolver(List.of(bruna), List.of(),
                List.of(codigo(bruna, ultimo), codigo(bruna, primeiro))).get(bruna.getId());

        assertThat(situacao.firstAccessRequestedAt()).isEqualTo(ultimo);
    }

    /** Conta é conta: um código velho não pendura "pediu o código" em quem já entrou. */
    @Test
    @DisplayName("quem já tem conta não mostra o código antigo, mesmo que ele não tenha sido usado")
    void contaVenceOCodigo() {
        Employee ana = funcionario("Ana Souza");

        EmployeeSiteAccess situacao = resolver(List.of(ana), List.of(conta("ana", ana, true)),
                List.of(codigo(ana, LocalDateTime.of(2026, 10, 1, 8, 0)))).get(ana.getId());

        assertThat(situacao.status()).isEqualTo(SiteAccess.ACTIVE);
        assertThat(situacao.firstAccessRequestedAt()).isNull();
    }

    @Test
    @DisplayName("todo funcionário pedido tem resposta, e conta de quem não foi pedido não atrapalha")
    void todoFuncionarioTemResposta() {
        Employee ricardo = funcionario("Ricardo Lima");
        Employee diego = funcionario("Diego Martins");
        Employee deFora = funcionario("Outra Pessoa");

        Map<UUID, EmployeeSiteAccess> mapa = resolver(List.of(ricardo, diego),
                List.of(conta("ricardo", ricardo, true), conta("outra", deFora, true), conta("admin", null, true)),
                List.of());

        assertThat(mapa).containsOnlyKeys(ricardo.getId(), diego.getId());
    }
}
