package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.humanResources.ReimbursementReportService;
import com.proautokimium.api.Infrastructure.services.permission.PermissionService;
import org.junit.jupiter.api.DisplayName;
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

import java.time.LocalDate;
import java.util.List;

import static com.proautokimium.api.domain.enums.humanResources.ReimbursementStatus.APPROVED;
import static com.proautokimium.api.domain.enums.humanResources.ReimbursementStatus.PAID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Quem emite o comprovante: só a tela do RH com BAIXAR. O documento traz nome
 * e valor de todo mundo — o Portal do Funcionário não pode chegar nele.
 */
@WebMvcTest(ReimbursementReportController.class)
@TestPropertySource(properties = {"server.port=0"})
@Import(SecurityConfiguration.class)
class ReimbursementReportControllerTest {

    private static final String URL =
            "/api/hr/reimbursements/report?from=2026-09-01&to=2026-09-30&status=PAID&status=APPROVED";

    @Autowired MockMvc mockMvc;

    @MockitoBean ReimbursementReportService service;
    @MockitoBean PermissionService permissionService;
    @MockitoBean UserRepository userRepository;
    @MockitoBean TokenService tokenService;
    @MockitoBean AuthenticationManager authenticationManager;

    @Test
    @DisplayName("RH com BAIXAR recebe o PDF, com os filtros repassados e nome de arquivo pelo período")
    @WithMockUser(username = "carla.rh", authorities = {"rh/reimbursements:BAIXAR"})
    void rhBaixa() throws Exception {
        when(service.generate(any(), any(), any(), any(), any())).thenReturn(new byte[]{'%', 'P', 'D', 'F'});

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"comprovante-reembolsos_2026-09-01_2026-09-30.pdf\""));

        verify(service).generate(eq(LocalDate.of(2026, 9, 1)), eq(LocalDate.of(2026, 9, 30)),
                eq(List.of(PAID, APPROVED)), isNull(), eq("carla.rh"));
    }

    @Test
    @DisplayName("quem só consulta a tela do RH não emite")
    @WithMockUser(authorities = {"rh/reimbursements:CONSULTAR"})
    void consultarNaoBaixa() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("o Portal do Funcionário não emite, nem com BAIXAR do próprio comprovante")
    @WithMockUser(authorities = {"documentos/rh/reimbursements:BAIXAR"})
    void portalNaoBaixa() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
}
