package com.proautokimium.api.controllers.humanResources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.humanResources.Department.CreateDepartmentRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Hierarchy.CreateHierarchyRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.PositionLevel.CreatePositionLevelRequestDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Team.CreateTeamRequestDTO;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.humanResources.DepartmentService;
import com.proautokimium.api.Infrastructure.services.humanResources.HierarchyService;
import com.proautokimium.api.Infrastructure.services.humanResources.PositionLevelService;
import com.proautokimium.api.Infrastructure.services.humanResources.TeamService;
import com.proautokimium.api.Infrastructure.services.permission.PermissionService;
import com.proautokimium.api.domain.enums.humanResources.SalaryAdjustmentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * **O contrato de alterar e excluir os cadastros de RH.**
 *
 * Os cinco cadastros — departamento, setor, hierarquia, cargo e nível — nasceram
 * só com POST e GET. Nome digitado errado ficava errado para sempre, e registro
 * criado por engano ficava na lista para sempre.
 *
 * Estes testes descrevem o que o front-end já espera, nas branches
 * `feat/org-structure-edit-delete` e `feat/position-level-edit`. Eles **falham
 * hoje com 405**, porque os mapeamentos ainda não existem — é essa a intenção:
 * o contrato deixa de ser prosa e vira algo que o build cobra.
 *
 * **Por que só status, e nada de `when(service.update(...))`:** stubar um método
 * que ainda não existe não compila, e um `src/test` que não compila trava a
 * suíte inteira — inclusive os 535 testes que já passam. Aqui só se afirma o
 * que atravessa o HTTP: a rota responde, e responde a quem tem permissão.
 *
 * O que estes testes **não** cobrem, e não dá para cobrir daqui: a regra de
 * recusar exclusão de registro em uso. Com o service mockado não há como
 * produzir esse 409 sem stubar o método que ele vai escrever. Isso é teste de
 * service, e vale escrever junto quando o método existir.
 */
@WebMvcTest({
        DepartmentController.class,
        TeamController.class,
        HierarchyController.class,
        PositionLevelController.class,
})
@TestPropertySource(properties = {"server.port=0"})
@Import(SecurityConfiguration.class)
class CadastrosDeRhAlterarExcluirTest {

    private static final String ESTRUTURA = "rh/organizational-structure";
    private static final String CARREIRA = "rh/career-structure";

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean DepartmentService departmentService;
    @MockitoBean TeamService teamService;
    @MockitoBean HierarchyService hierarchyService;
    @MockitoBean PositionLevelService positionLevelService;

    @MockitoBean PermissionService permissionService;
    @MockitoBean UserRepository userRepository;
    @MockitoBean TokenService tokenService;
    @MockitoBean AuthenticationManager authenticationManager;

    private String json(Object corpo) throws Exception {
        return objectMapper.writeValueAsString(corpo);
    }

    // ─── Departamento ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Departamento")
    class Departamento {

        private final CreateDepartmentRequestDTO CORPO = new CreateDepartmentRequestDTO("Industrial");

        @Test
        @DisplayName("PUT altera o departamento")
        @WithMockUser(authorities = ESTRUTURA + ":ALTERAR")
        void alterar() throws Exception {
            mockMvc.perform(put("/api/hr/departments/{id}", ID).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(CORPO)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("DELETE exclui o departamento")
        @WithMockUser(authorities = ESTRUTURA + ":EXCLUIR")
        void excluir() throws Exception {
            mockMvc.perform(delete("/api/hr/departments/{id}", ID).with(csrf()))
                    .andExpect(status().isNoContent());
        }

        /**
         * Quem só consulta não altera. A tela esconde o botão, mas esconder não
         * é proteger: a rota é chamável direto.
         */
        @Test
        @DisplayName("quem só consulta não altera")
        @WithMockUser(authorities = ESTRUTURA + ":CONSULTAR")
        void alterarExigePermissao() throws Exception {
            mockMvc.perform(put("/api/hr/departments/{id}", ID).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(CORPO)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("quem só consulta não exclui")
        @WithMockUser(authorities = ESTRUTURA + ":CONSULTAR")
        void excluirExigePermissao() throws Exception {
            mockMvc.perform(delete("/api/hr/departments/{id}", ID).with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    // ─── Setor ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Setor")
    class Setor {

        private final CreateTeamRequestDTO CORPO = new CreateTeamRequestDTO("Produção", ID);

        @Test
        @DisplayName("PUT altera o setor")
        @WithMockUser(authorities = ESTRUTURA + ":ALTERAR")
        void alterar() throws Exception {
            mockMvc.perform(put("/api/hr/teams/{id}", ID).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(CORPO)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("DELETE exclui o setor")
        @WithMockUser(authorities = ESTRUTURA + ":EXCLUIR")
        void excluir() throws Exception {
            mockMvc.perform(delete("/api/hr/teams/{id}", ID).with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("quem só consulta não altera")
        @WithMockUser(authorities = ESTRUTURA + ":CONSULTAR")
        void alterarExigePermissao() throws Exception {
            mockMvc.perform(put("/api/hr/teams/{id}", ID).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(CORPO)))
                    .andExpect(status().isForbidden());
        }
    }

    // ─── Hierarquia ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Hierarquia")
    class Hierarquia {

        private final CreateHierarchyRequestDTO CORPO = new CreateHierarchyRequestDTO("Gerente", 2);

        @Test
        @DisplayName("PUT altera a hierarquia")
        @WithMockUser(authorities = ESTRUTURA + ":ALTERAR")
        void alterar() throws Exception {
            mockMvc.perform(put("/api/hr/hierarchies/{id}", ID).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(CORPO)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("DELETE exclui a hierarquia")
        @WithMockUser(authorities = ESTRUTURA + ":EXCLUIR")
        void excluir() throws Exception {
            mockMvc.perform(delete("/api/hr/hierarchies/{id}", ID).with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("quem só consulta não exclui")
        @WithMockUser(authorities = ESTRUTURA + ":CONSULTAR")
        void excluirExigePermissao() throws Exception {
            mockMvc.perform(delete("/api/hr/hierarchies/{id}", ID).with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    // ─── Nível de cargo ───────────────────────────────────────────────────────

    /**
     * O nível tem permissão própria — `rh/career-structure`, e não
     * `rh/organizational-structure`. Quem mexe na estrutura da empresa não é
     * necessariamente quem mexe em salário.
     */
    @Nested
    @DisplayName("Nível de cargo")
    class Nivel {

        private final CreatePositionLevelRequestDTO CORPO = new CreatePositionLevelRequestDTO(
                "Júnior", 1, ID, SalaryAdjustmentType.FIXED, new BigDecimal("3000.00"), null);

        @Test
        @DisplayName("PUT altera o nível, e é assim que o valor base muda")
        @WithMockUser(authorities = CARREIRA + ":ALTERAR")
        void alterar() throws Exception {
            mockMvc.perform(put("/api/hr/position-levels/{id}", ID).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(CORPO)))
                    .andExpect(status().isOk());
        }

        /**
         * Permissão da estrutura não abre salário. São telas diferentes, e o
         * valor base é dado sensível.
         */
        @Test
        @DisplayName("permissão da Estrutura não serve para mexer em salário")
        @WithMockUser(authorities = ESTRUTURA + ":ALTERAR")
        void naoAceitaPermissaoDaEstrutura() throws Exception {
            mockMvc.perform(put("/api/hr/position-levels/{id}", ID).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(CORPO)))
                    .andExpect(status().isForbidden());
        }
    }
}
