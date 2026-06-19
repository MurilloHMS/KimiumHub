package com.proautokimium.api.controllers.prostock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.prostock.machine.MachineDTO;
import com.proautokimium.api.Application.DTOs.prostock.machine.MachineMovementDTO;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.machine.MachineService;
import com.proautokimium.api.Infrastructure.services.machine.RegisterService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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

    private final UUID machineId = UUID.randomUUID();
    private final UUID movementId = UUID.randomUUID();

    private MachineDTO buildMachineDto() {
        return new MachineDTO(machineId, "SYS001", "Máquina Teste", "Marca A", null, null, 10, true);
    }

    private MachineMovementDTO buildMovementDto() {
        return new MachineMovementDTO(movementId, LocalDateTime.now(), 5);
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

    @Test
    @DisplayName("POST api/machine - deve criar máquina e retornar 201")
    @WithMockUser
    void deveCriarMaquinaComSucesso() throws Exception {
        doNothing().when(service).save(any(MachineDTO.class));

        mockMvc.perform(post("/api/machine")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildMachineDto())))
                .andExpect(status().isCreated())
                .andExpect(content().string("Máquina Cadastrada com sucesso!"));
    }

    @Test
    @DisplayName("PUT api/machine - deve atualizar máquina e retornar 200")
    @WithMockUser
    void deveAtualizarMaquinaComSucesso() throws Exception {
        doNothing().when(service).update(any(MachineDTO.class));

        mockMvc.perform(put("/api/machine")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildMachineDto())))
                .andExpect(status().isOk())
                .andExpect(content().string("Máquina Atualizada com sucesso!"));
    }

    @Test
    @DisplayName("DELETE api/machine/{id} - deve deletar máquina e retornar 200")
    @WithMockUser
    void deveDeletarMaquinaComSucesso() throws Exception {
        doNothing().when(service).delete(machineId);

        mockMvc.perform(delete("/api/machine/{id}", machineId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Máquina Deletada com sucesso!"));
    }

    @Test
    @DisplayName("GET api/machine/movements/{id} - deve retornar movimentos de uma máquina")
    @WithMockUser
    void deveRetornarMovimentosDaMaquina() throws Exception {
        when(service.getMovementsByMachineId(machineId)).thenReturn(List.of(buildMovementDto()));

        mockMvc.perform(get("/api/machine/movements/{id}", machineId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST api/machine/movements/{id} - deve criar movimentação e retornar 201")
    @WithMockUser
    void deveCriarMovimentacaoComSucesso() throws Exception {
        doNothing().when(service).createMovement(any(MachineMovementDTO.class), any(UUID.class));

        mockMvc.perform(post("/api/machine/movements/{id}", machineId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildMovementDto())))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("PUT api/machine/movements/{id} - deve atualizar movimentação e retornar 200")
    @WithMockUser
    void deveAtualizarMovimentacaoComSucesso() throws Exception {
        doNothing().when(service).updateMovement(any(MachineMovementDTO.class), any(UUID.class));

        mockMvc.perform(put("/api/machine/movements/{id}", machineId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildMovementDto())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE api/machine/movements/{id} - deve deletar movimentação e retornar 200")
    @WithMockUser
    void deveDeletarMovimentacaoComSucesso() throws Exception {
        doNothing().when(service).deleteMovement(movementId);

        mockMvc.perform(delete("/api/machine/movements/{id}", movementId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Movimento da máquina Deletado com sucesso!"));
    }

    @Test
    @DisplayName("GET api/machine/register - deve retornar todos os registros")
    @WithMockUser
    void deveRetornarTodosOsRegistros() throws Exception {
        when(registerService.listarRegistros()).thenReturn(List.of());

        mockMvc.perform(get("/api/machine/register"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET api/machine/register/{id} - deve retornar registros por máquina")
    @WithMockUser
    void deveRetornarRegistrosPorMaquina() throws Exception {
        when(registerService.listarRegistrosPorMaquina(machineId)).thenReturn(List.of());

        mockMvc.perform(get("/api/machine/register/{id}", machineId))
                .andExpect(status().isOk());
    }
}
