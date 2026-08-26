package com.proautokimium.api.controllers.prostock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.prostock.machine.MachineDTO;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.machine.MachineAlertService;
import com.proautokimium.api.Infrastructure.services.machine.MachineService;
import com.proautokimium.api.Infrastructure.services.machine.MachineReconciliationService;
import com.proautokimium.api.Infrastructure.services.machine.RegisterService;
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
import java.util.UUID;

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

}
