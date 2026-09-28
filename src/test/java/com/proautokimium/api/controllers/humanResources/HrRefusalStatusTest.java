package com.proautokimium.api.controllers.humanResources;

import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.humanResources.ReimbursementService;
import com.proautokimium.api.Infrastructure.services.permission.PermissionService;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.Reimbursement;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * **Uma recusa de regra chega à tela como recusa, não como "Erro interno".**
 *
 * O teste de entidade prova QUAL exceção sai; este prova o que a pessoa vê.
 * O serviço é mock, mas a exceção é a da entidade de verdade: pagar um
 * reembolso que ainda está pendente.
 */
@WebMvcTest(ReimbursementController.class)
@TestPropertySource(properties = {"server.port=0"})
@Import(SecurityConfiguration.class)
class HrRefusalStatusTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ReimbursementService reimbursementService;
    @MockitoBean PermissionService permissionService;
    @MockitoBean UserRepository userRepository;
    @MockitoBean TokenService tokenService;
    @MockitoBean AuthenticationManager authenticationManager;

    @Test
    @DisplayName("pagar reembolso pendente devolve 409 com a mensagem da regra")
    @WithMockUser(authorities = {"rh/reimbursements:CONFIGURAR"})
    void recusaDeEstadoEh409() throws Exception {
        Reimbursement pendente = Reimbursement.request(new Employee(), LocalDate.of(2026, 9, 1),
                new BigDecimal("50.00"), "Combustível", "visita", "comprovante.jpg", "/tmp/x",
                LocalDateTime.of(2026, 9, 1, 9, 0));
        when(reimbursementService.pay(any(), any())).thenAnswer(inv -> {
            pendente.pay(LocalDate.of(2026, 9, 5), LocalDateTime.of(2026, 9, 5, 9, 0));
            return null;
        });

        mockMvc.perform(post("/api/hr/reimbursements/" + UUID.randomUUID() + "/pay")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentDate\":\"2026-09-05\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Só é possível pagar um reembolso aprovado"));
    }
}
