package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.holerite.HoleriteService;
import com.proautokimium.api.Infrastructure.services.humanResources.EmployeeDocumentService;
import com.proautokimium.api.Infrastructure.services.humanResources.MedicalCertificateService;
import com.proautokimium.api.Infrastructure.services.humanResources.ReimbursementService;
import com.proautokimium.api.Infrastructure.services.permission.PermissionService;
import com.proautokimium.api.controllers.HoleriteController;
import com.proautokimium.api.domain.entities.HoleriteDocumento;
import com.proautokimium.api.domain.entities.humanResources.EmployeeDocument;
import com.proautokimium.api.domain.entities.humanResources.MedicalCertificate;
import com.proautokimium.api.domain.entities.humanResources.Reimbursement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * **Quem baixa o arquivo de outra pessoa.**
 *
 * Os quatro downloads de RH (comprovante de reembolso, atestado, documento e
 * holerite) aceitam duas telas — a do RH e a do Portal do Funcionário — e quem
 * separa "vê de todos" de "vê só o seu" é o controller, ao calcular o `isRh`.
 *
 * Dois defeitos moravam nesse cálculo, e os testes de serviço não viam nenhum,
 * porque recebem o `isRh` já pronto:
 *
 * 1. `contains("ADMIN")` casa com `ROLE_ADMINISTRATIVO` — quem é do
 *    administrativo baixava atestado e holerite de qualquer um.
 * 2. O comprovante de reembolso passava `true` fixo — qualquer funcionário
 *    baixava o comprovante de qualquer outro.
 *
 * O serviço é mock, mas responde com a regra real: RH vê tudo, o dono vê o seu.
 * O que se testa é a decisão do controller.
 */
@WebMvcTest({ReimbursementController.class, MedicalCertificateController.class,
             EmployeeDocumentController.class, HoleriteController.class})
@TestPropertySource(properties = {"server.port=0"})
@Import(SecurityConfiguration.class)
class FileDownloadAccessTest {

    private static final String DONO = "ana";
    private static final String OUTRO = "joao";

    @Autowired MockMvc mockMvc;

    @MockitoBean ReimbursementService reimbursementService;
    @MockitoBean MedicalCertificateService medicalCertificateService;
    @MockitoBean EmployeeDocumentService employeeDocumentService;
    @MockitoBean HoleriteService holeriteService;
    @MockitoBean PermissionService permissionService;
    @MockitoBean UserRepository userRepository;
    @MockitoBean TokenService tokenService;
    @MockitoBean AuthenticationManager authenticationManager;

    /** A regra do serviço, reproduzida: RH vê tudo, o dono vê o seu. */
    private static boolean regraReal(String login, boolean isRh) {
        return isRh || DONO.equals(login);
    }

    @BeforeEach
    void servicosComRegraReal() throws Exception {
        when(reimbursementService.buscar(any())).thenReturn(Optional.of(mock(Reimbursement.class)));
        when(reimbursementService.podeAcessar(any(), anyString(), anyBoolean()))
                .thenAnswer(inv -> regraReal(inv.getArgument(1), inv.getArgument(2)));
        when(reimbursementService.lerComprovante(any())).thenReturn(new byte[]{1});

        when(medicalCertificateService.buscar(any())).thenReturn(Optional.of(mock(MedicalCertificate.class)));
        when(medicalCertificateService.podeAcessar(any(), anyString(), anyBoolean()))
                .thenAnswer(inv -> regraReal(inv.getArgument(1), inv.getArgument(2)));
        when(medicalCertificateService.lerArquivo(any())).thenReturn(new byte[]{1});

        when(employeeDocumentService.find(any())).thenReturn(Optional.of(mock(EmployeeDocument.class)));
        when(employeeDocumentService.canAccess(any(), anyString(), anyBoolean()))
                .thenAnswer(inv -> regraReal(inv.getArgument(1), inv.getArgument(2)));
        when(employeeDocumentService.readFile(any())).thenReturn(new byte[]{1});

        when(holeriteService.buscar(any())).thenReturn(Optional.of(mock(HoleriteDocumento.class)));
        when(holeriteService.podeAcessar(any(), anyString(), anyBoolean()))
                .thenAnswer(inv -> regraReal(inv.getArgument(1), inv.getArgument(2)));
        when(holeriteService.lerArquivo(any())).thenReturn(new byte[]{1});
    }

    private static String reembolso() { return "/api/hr/reimbursements/" + UUID.randomUUID() + "/receipt"; }
    private static String atestado()  { return "/api/hr/medical-certificates/" + UUID.randomUUID() + "/file"; }
    private static String documento() { return "/api/hr/employee-documents/" + UUID.randomUUID() + "/arquivo"; }
    private static String holerite()  { return "/api/holerite/" + UUID.randomUUID() + "/arquivo"; }

    // ─── Defeito 1: ROLE_ADMINISTRATIVO não é ADMIN ──────────────────────────

    @Nested
    @DisplayName("quem é do administrativo não baixa arquivo dos outros")
    class Administrativo {

        @Test
        @DisplayName("atestado")
        @WithMockUser(username = OUTRO, authorities = {"ROLE_ADMINISTRATIVO", "documentos/rh/medical-certificates:BAIXAR"})
        void atestado() throws Exception {
            mockMvc.perform(get(FileDownloadAccessTest.atestado())).andExpect(status().isForbidden());
            verify(medicalCertificateService, never()).lerArquivo(any());
        }

        @Test
        @DisplayName("documento")
        @WithMockUser(username = OUTRO, authorities = {"ROLE_ADMINISTRATIVO", "documentos/rh/documents:BAIXAR"})
        void documento() throws Exception {
            // 404, e não 403: para quem não é dono, o documento "não existe" —
            // um 403 confirmaria que o id é de alguém.
            mockMvc.perform(get(FileDownloadAccessTest.documento())).andExpect(status().isNotFound());
            verify(employeeDocumentService, never()).readFile(any());
        }

        @Test
        @DisplayName("holerite")
        @WithMockUser(username = OUTRO, authorities = {"ROLE_ADMINISTRATIVO", "documentos/holerites:BAIXAR"})
        void holerite() throws Exception {
            mockMvc.perform(get(FileDownloadAccessTest.holerite())).andExpect(status().isForbidden());
            verify(holeriteService, never()).lerArquivo(any());
        }

        @Test
        @DisplayName("comprovante de reembolso")
        @WithMockUser(username = OUTRO, authorities = {"ROLE_ADMINISTRATIVO", "documentos/rh/reimbursements:BAIXAR"})
        void reembolso() throws Exception {
            mockMvc.perform(get(FileDownloadAccessTest.reembolso())).andExpect(status().isForbidden());
            verify(reimbursementService, never()).lerComprovante(any());
        }
    }

    // ─── Defeito 2: o comprovante com `true` fixo ────────────────────────────

    @Test
    @DisplayName("pelo portal, o funcionário não baixa o comprovante de outro")
    @WithMockUser(username = OUTRO, authorities = {"documentos/rh/reimbursements:BAIXAR"})
    void portalNaoBaixaComprovanteDeOutro() throws Exception {
        mockMvc.perform(get(reembolso())).andExpect(status().isForbidden());
        verify(reimbursementService, never()).lerComprovante(any());
    }

    // ─── O que decide é a tela do RH, não a role ─────────────────────────────

    /**
     * Com as permissões por tela, quem tem a tela do RH pode não ter role de RH
     * nenhuma. É a permissão que diz "vê de todos".
     */
    @Nested
    @DisplayName("quem tem a tela do RH baixa de qualquer um, mesmo sem role")
    class TelaDoRh {

        @Test
        @DisplayName("atestado")
        @WithMockUser(username = OUTRO, authorities = {"rh/medical-certificates:BAIXAR"})
        void atestado() throws Exception {
            mockMvc.perform(get(FileDownloadAccessTest.atestado())).andExpect(status().isOk());
        }

        @Test
        @DisplayName("documento")
        @WithMockUser(username = OUTRO, authorities = {"rh/employee-documents:BAIXAR"})
        void documento() throws Exception {
            mockMvc.perform(get(FileDownloadAccessTest.documento())).andExpect(status().isOk());
        }

        /**
         * A tela de Funcionários não baixa mais documento: a permissão mudou para
         * a tela própria (V110), que abre para os mesmos modelos RH e ADMIN.
         */
        @Test
        @DisplayName("documento: a tela de Funcionários não basta mais")
        @WithMockUser(username = OUTRO, authorities = {"rh/employees:BAIXAR"})
        void documentoPelaTelaAntiga() throws Exception {
            mockMvc.perform(get(FileDownloadAccessTest.documento())).andExpect(status().isForbidden());
            verify(employeeDocumentService, never()).readFile(any());
        }

        @Test
        @DisplayName("holerite")
        @WithMockUser(username = OUTRO, authorities = {"rh/holerit:BAIXAR"})
        void holerite() throws Exception {
            mockMvc.perform(get(FileDownloadAccessTest.holerite())).andExpect(status().isOk());
        }

        @Test
        @DisplayName("comprovante de reembolso")
        @WithMockUser(username = OUTRO, authorities = {"rh/reimbursements:BAIXAR"})
        void reembolso() throws Exception {
            mockMvc.perform(get(FileDownloadAccessTest.reembolso())).andExpect(status().isOk());
        }
    }

    // ─── E o dono continua baixando o seu ────────────────────────────────────

    /**
     * Rede contra a correção exagerada: fechar para todo mundo também faria os
     * testes acima passarem.
     */
    @Nested
    @DisplayName("o dono baixa o próprio arquivo pelo portal")
    class Dono {

        @Test
        @DisplayName("atestado")
        @WithMockUser(username = DONO, authorities = {"documentos/rh/medical-certificates:BAIXAR"})
        void atestado() throws Exception {
            mockMvc.perform(get(FileDownloadAccessTest.atestado())).andExpect(status().isOk());
        }

        @Test
        @DisplayName("holerite")
        @WithMockUser(username = DONO, authorities = {"documentos/holerites:BAIXAR"})
        void holerite() throws Exception {
            mockMvc.perform(get(FileDownloadAccessTest.holerite())).andExpect(status().isOk());
        }

        @Test
        @DisplayName("comprovante de reembolso")
        @WithMockUser(username = DONO, authorities = {"documentos/rh/reimbursements:BAIXAR"})
        void reembolso() throws Exception {
            mockMvc.perform(get(FileDownloadAccessTest.reembolso())).andExpect(status().isOk());
        }

        /** O caso que faltava desde a auditoria: o dono baixando o próprio documento. */
        @Test
        @DisplayName("documento")
        @WithMockUser(username = DONO, authorities = {"documentos/rh/documents:BAIXAR"})
        void documento() throws Exception {
            mockMvc.perform(get(FileDownloadAccessTest.documento())).andExpect(status().isOk());
        }
    }
}
