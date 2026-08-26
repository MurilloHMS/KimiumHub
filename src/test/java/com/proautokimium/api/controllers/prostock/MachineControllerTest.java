package com.proautokimium.api.controllers.prostock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Infrastructure.services.permission.PermissionService;
import com.proautokimium.api.Application.DTOs.prostock.machine.MachineDTO;
import com.proautokimium.api.Application.DTOs.prostock.machine.MachineDivergenceDTO;
import com.proautokimium.api.Application.DTOs.prostock.machine.ReconcileDTO;
import com.proautokimium.api.domain.exceptions.machine.ReconciliationMismatchException;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.machine.MachineAlertService;
import com.proautokimium.api.Infrastructure.services.machine.MachineService;
import com.proautokimium.api.Infrastructure.services.machine.MachineReconciliationService;
import com.proautokimium.api.Infrastructure.services.machine.RegisterService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MachineController.class)
@TestPropertySource(properties = {"server.port=0"})
@Import(SecurityConfiguration.class)
class MachineControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean MachineService service;
    @MockitoBean RegisterService registerService;
    @MockitoBean TokenService tokenService;
    // O SecurityFilter passa a somar as permissões de tela às roles.
    @MockitoBean PermissionService permissionService;
    @MockitoBean AuthenticationManager authenticationManager;
    @MockitoBean UserRepository userRepository;
    @MockitoBean MachineAlertService alertService;
    // Dependência nova do controller: sem ela o @WebMvcTest não monta o
    // contexto, e a falha aparece como "no qualifying bean" em todo teste da
    // classe — não só no que exercita a conciliação.
    @MockitoBean MachineReconciliationService reconciliationService;

    private final UUID machineId = UUID.randomUUID();

    private MachineDTO buildMachineDto() {
        return new MachineDTO(machineId, "SYS001", "Máquina Teste", "Marca A", null, null, 10, true);
    }

    @Test
    @DisplayName("GET api/machine - deve retornar lista de máquinas quando autenticado")
    @WithMockUser
    void deveRetornarMaquinasAutenticado() throws Exception {
        when(service.getAllMachines()).thenReturn(List.of(buildMachineDto()));

        mockMvc.perform(get("/api/machine"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET api/machine - deve retornar 403 sem autenticação")
    void deveRetornar403SemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/machine"))
                .andExpect(status().isForbidden());
    }

    // ─── POST api/machine/reconcile ──────────────────────────────────────────
    // O serviço tem os próprios testes; aqui o que se verifica é a borda: quem
    // pode chamar, o que o corpo precisa trazer, e com que status o erro sai.

    private String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    /**
     * Máquina saindo do galpão é patrimônio. O endpoint escreve nos dois lados
     * do estoque, e o teste existe para que ninguém o deixe aberto sem perceber.
     */
    @Test
    @DisplayName("POST api/machine/reconcile - deve retornar 403 sem autenticação")
    void reconcileExigeAutenticacao() throws Exception {
        ReconcileDTO dto = new ReconcileDTO("SYS001", 1, LocalDateTime.now(), List.of(), 1);

        mockMvc.perform(post("/api/machine/reconcile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(dto)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(reconciliationService);
    }

    /**
     * O sinal do `delta` é o contrato inteiro: negativo é saída.
     *
     * Se a desserialização perdesse o sinal, uma saída viraria entrada — e o
     * teste do serviço não pegaria, porque lá o DTO é construído em Java.
     */
    @Test
    @DisplayName("POST api/machine/reconcile - deve chamar o serviço com o delta negativo intacto")
    @WithMockUser
    void reconcileEntregaODtoAoServico() throws Exception {
        UUID registerId = UUID.randomUUID();
        ReconcileDTO dto = new ReconcileDTO("SYS001", -1, LocalDateTime.now(), List.of(registerId), 0);

        mockMvc.perform(post("/api/machine/reconcile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(dto)))
                .andExpect(status().isOk());

        ArgumentCaptor<ReconcileDTO> captor = ArgumentCaptor.forClass(ReconcileDTO.class);
        verify(reconciliationService).reconcile(captor.capture());

        assertThat(captor.getValue().delta()).isEqualTo(-1);
        assertThat(captor.getValue().registersToDeliver()).containsExactly(registerId);
    }

    @Test
    @DisplayName("POST api/machine/reconcile - deve retornar 400 sem código do produto")
    @WithMockUser
    void reconcileSemCodigoEhRecusado() throws Exception {
        ReconcileDTO dto = new ReconcileDTO("  ", 1, LocalDateTime.now(), List.of(), 1);

        mockMvc.perform(post("/api/machine/reconcile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(dto)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reconciliationService);
    }

    /**
     * **A conta não fechar é erro de quem lançou, não do servidor.**
     *
     * Sem o `DomainExceptionHandler` ligado nesta rota, a mesma recusa sairia
     * como 500 "Erro interno no servidor" — e quem está lançando não teria como
     * saber que faltou escolher uma programação. Já aconteceu neste projeto em
     * onze endpoints de uma vez.
     */
    @Test
    @DisplayName("POST api/machine/reconcile - conta que não fecha sai como 400, não 500")
    @WithMockUser
    void reconcileComContaAbertaSaiComo400() throws Exception {
        doThrow(new ReconciliationMismatchException("Saída de 2 precisa de 2 programações, e vieram 1."))
                .when(reconciliationService).reconcile(any());

        ReconcileDTO dto = new ReconcileDTO("SYS001", -2, LocalDateTime.now(),
                List.of(UUID.randomUUID()), 0);

        mockMvc.perform(post("/api/machine/reconcile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Saída de 2 precisa de 2 programações, e vieram 1."));
    }

    // ─── Os dois insights novos do Hub ───────────────────────────────────────

    /**
     * A divergência expõe quanta máquina a empresa tem. Fica atrás de login
     * como o resto — e o teste existe para ninguém deixar aberto sem perceber.
     */
    @Test
    @DisplayName("GET api/machine/divergences - deve retornar 403 sem autenticação")
    void divergenciasExigemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/machine/divergences"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(reconciliationService);
    }

    /**
     * As que batem vêm junto de propósito: sem elas, lista vazia seria
     * indistinguível de tela quebrada.
     */
    @Test
    @DisplayName("GET api/machine/divergences - devolve também as máquinas que batem")
    @WithMockUser
    void divergenciasIncluemAsQueBatem() throws Exception {
        when(reconciliationService.divergences()).thenReturn(List.of(
                new MachineDivergenceDTO(machineId, "MAQ-001", "Lavadora", 5, 4),
                new MachineDivergenceDTO(UUID.randomUUID(), "MAQ-002", "Esteira", 3, 3)));

        mockMvc.perform(get("/api/machine/divergences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Lavadora"));
    }

    /**
     * Sem `from` o endpoint traria o histórico inteiro numa tela que só quer o
     * mês. Obrigatório, e recusado com 400 — não com 500.
     */
    @Test
    @DisplayName("GET api/machine/register/schedule-changes - sem `from` é 400")
    @WithMockUser
    void adiamentosSemPeriodoSaoRecusados() throws Exception {
        mockMvc.perform(get("/api/machine/register/schedule-changes"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(registerService);
    }

    @Test
    @DisplayName("GET api/machine/register/schedule-changes - deve retornar 403 sem autenticação")
    void adiamentosExigemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/machine/register/schedule-changes?from=2026-09-01"))
                .andExpect(status().isForbidden());
    }

}
