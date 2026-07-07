package com.proautokimium.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.certificate.CertificateHolderDTO;
import com.proautokimium.api.Infrastructure.interfaces.certificate.CertificateGenerator;
import com.proautokimium.api.Infrastructure.repositories.CertificateHolderRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.domain.entities.CertificateHolder;
import com.proautokimium.api.domain.valueObjects.Email;
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

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CertificateController.class)
@TestPropertySource(properties = {
        "server.port=0"
})
@Import(SecurityConfiguration.class)
class CertificateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CertificateHolderRepository repository;

    @MockitoBean
    private CertificateGenerator generator;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @DisplayName("Deve lançar exceção se certificado já existir")
    void shouldThrowExceptionWhenCertificateAlreadyExists() throws Exception {

        CertificateHolder holder = new CertificateHolder(
                "pessoa",
                "11999999999",
                new Email("email@teste.com")
        );

        when(repository.findByEmail(any()))
                .thenReturn(Optional.of(holder));

        mockMvc.perform(post("/api/certificate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "name":"pessoa",
                        "cellphone":"11999999999",
                        "email":"email@teste.com"
                    }
                    """))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Deve retornar certificado sem validação")
    void createCertificateHolderWithoutValidation() throws Exception {
        CertificateHolderDTO dto = new CertificateHolderDTO("pessoa", "11999999999", new Email("email@teste.com"));

        byte[] pdf = "test-pdf".getBytes();
        when(generator.generateCertificate("PESSOA")).thenReturn(pdf);

        mockMvc.perform(post("/api/certificate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name":"pessoa",
                                "cellphone": "11999999999",
                                "email":"email@teste.com"
                            }
                        """)
                ).andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdf));
    }
}