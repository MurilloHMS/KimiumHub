package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.holerite.PayslipTypeDTOs.CreatePayslipTypeResult;
import com.proautokimium.api.Application.DTOs.holerite.PayslipTypeDTOs.PayslipTypeDTO;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.holerite.HoleriteService;
import com.proautokimium.api.Infrastructure.services.holerite.PayslipTypeService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Os tipos de holerite: o funcionário LÊ (a tela dele mostra o nome, não o
 * código), mas só o RH cria.
 */
@WebMvcTest(HoleriteController.class)
@TestPropertySource(properties = {"server.port=0"})
@Import(SecurityConfiguration.class)
class HoleriteTypesControllerTest {

    private static final String URL = "/api/holerite/types";

    @Autowired MockMvc mockMvc;

    @MockitoBean HoleriteService holeriteService;
    @MockitoBean PayslipTypeService payslipTypes;
    @MockitoBean PermissionService permissionService;
    @MockitoBean UserRepository userRepository;
    @MockitoBean TokenService tokenService;
    @MockitoBean AuthenticationManager authenticationManager;

    @Test
    @DisplayName("o funcionário lista os tipos")
    @WithMockUser(authorities = {"documentos/holerites:CONSULTAR"})
    void employeeLists() throws Exception {
        when(payslipTypes.list()).thenReturn(List.of(new PayslipTypeDTO("PLR", "PLR")));

        mockMvc.perform(get(URL)).andExpect(status().isOk()).andExpect(jsonPath("$[0].label").value("PLR"));
    }

    @Test
    @DisplayName("o funcionário não cria tipo")
    @WithMockUser(authorities = {"documentos/holerites:CONSULTAR"})
    void employeeCannotCreate() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"label\":\"Bônus\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(payslipTypes);
    }

    @Test
    @DisplayName("o RH cria: 201 com o tipo novo, e o login dele como autor")
    @WithMockUser(username = "carla.rh", authorities = {"rh/holerit:INCLUIR"})
    void hrCreates() throws Exception {
        when(payslipTypes.create(anyString(), anyString()))
                .thenReturn(new CreatePayslipTypeResult(new PayslipTypeDTO("BONUS", "Bônus"), true));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"label\":\"Bônus\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type.code").value("BONUS"));
        verify(payslipTypes).create(eq("Bônus"), eq("carla.rh"));
    }

    @Test
    @DisplayName("nome que já existia: 200 com o existente e created=false")
    @WithMockUser(authorities = {"rh/holerit:INCLUIR"})
    void existingReturns200() throws Exception {
        when(payslipTypes.create(anyString(), anyString()))
                .thenReturn(new CreatePayslipTypeResult(new PayslipTypeDTO("PLR", "PLR"), false));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"label\":\"plr\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(false));
    }

    @Test
    @DisplayName("nome vazio é 400, sem chegar ao serviço")
    @WithMockUser(authorities = {"rh/holerit:INCLUIR"})
    void blankLabel() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"label\":\"  \"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(payslipTypes);
    }
}
