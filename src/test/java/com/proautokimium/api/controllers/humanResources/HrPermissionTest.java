package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.humanResources.CompanyService;
import com.proautokimium.api.Infrastructure.services.humanResources.ReimbursementService;
import com.proautokimium.api.Infrastructure.services.permission.PermissionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * As duas regras do lote de RH — e **os primeiros testes de controller que este
 * módulo tem**.
 *
 * Os 19 controllers de RH não tinham nenhum `@WebMvcTest`. A suíte inteira
 * passava verde depois de eu anotar 55 endpoints, e não porque estava certo:
 * porque não havia nada olhando. Isto aqui é o mínimo que cobre as duas
 * decisões que podem quebrar tela sem dar erro.
 */
@WebMvcTest({CompanyController.class, ReimbursementController.class})
@TestPropertySource(properties = {"server.port=0"})
@Import(SecurityConfiguration.class)
class HrPermissionTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean CompanyService companyService;
    @MockitoBean ReimbursementService reimbursementService;
    @MockitoBean PermissionService permissionService;
    @MockitoBean UserRepository userRepository;
    @MockitoBean TokenService tokenService;
    @MockitoBean AuthenticationManager authenticationManager;

    // ─── Regra 1: leitura compartilhada ───────────────────────────────────────

    /**
     * **A lista de empresas alimenta três telas.**
     *
     * A Estrutura, os Cargos & Níveis e o cadastro de Funcionários leem os
     * mesmos combos. Exigir `rh/organizational-structure` aqui deixaria o
     * formulário de funcionário com a empresa vazia — e um combo vazio não
     * parece falta de permissão, parece cadastro faltando.
     */
    @Test
    @DisplayName("quem cadastra funcionário lê as empresas, mesmo sem a tela de Estrutura")
    @WithMockUser(authorities = {"rh/employees:CONSULTAR"})
    void leituraDeReferenciaAceitaAsTresTelas() throws Exception {
        when(companyService.listAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/hr/companies"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("quem não é de RH nenhum não lê as empresas")
    @WithMockUser(authorities = {"stock/hub:CONSULTAR"})
    void quemNaoEhDoRhNaoLe() throws Exception {
        mockMvc.perform(get("/api/hr/companies"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(companyService);
    }

    // ─── Regra 2: RH × Portal do Funcionário ─────────────────────────────────

    /**
     * **O funcionário vê os próprios reembolsos, e só.**
     *
     * `/me` pertence ao Portal do Funcionário. Se ele exigisse a tela do RH,
     * ninguém acompanharia o próprio pedido — que é metade do motivo da tela
     * existir.
     */
    @Test
    @DisplayName("o funcionário lê os próprios reembolsos pelo portal")
    @WithMockUser(username = "ricardo",
                  authorities = {"documentos/rh/reimbursements:CONSULTAR"})
    void oPortalLeOsProprios() throws Exception {
        mockMvc.perform(get("/api/hr/reimbursements/me"))
                .andExpect(status().isOk());
    }

    /**
     * **E não vê os dos outros.**
     *
     * É o teste que separa as duas telas. Sem ele, alguém "simplifica" as duas
     * anotações para a mesma authority e a lista da empresa inteira vaza para
     * quem só devia ver a própria.
     */
    @Test
    @DisplayName("o portal do funcionário não abre a lista de todo mundo")
    @WithMockUser(username = "ricardo",
                  authorities = {"documentos/rh/reimbursements:CONSULTAR"})
    void oPortalNaoVeOsDosOutros() throws Exception {
        mockMvc.perform(get("/api/hr/reimbursements"))
                .andExpect(status().isForbidden());
    }

    /**
     * Pagar pesa mais que aprovar.
     *
     * Quem revisa o pedido tem `ALTERAR`; quem libera o dinheiro precisa de
     * `CONFIGURAR`. Se as duas fossem a mesma, separar as funções depois
     * exigiria mexer no código de novo.
     */
    @Test
    @DisplayName("quem aprova reembolso não paga")
    @WithMockUser(authorities = {"rh/reimbursements:ALTERAR"})
    void aprovarNaoEhPagar() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/hr/reimbursements/"
                                + java.util.UUID.randomUUID() + "/pay")
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"paidAt\":\"2026-08-27\"}"))
                .andExpect(status().isForbidden());
    }
}
