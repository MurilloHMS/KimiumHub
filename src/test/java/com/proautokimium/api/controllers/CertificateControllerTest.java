package com.proautokimium.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.certificate.CertificateBatchDTO;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

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

    // --- Lote (/batch) -------------------------------------------------------

    /**
     * Os outros dois endpoints de certificado sao publicos, e este nao e.
     *
     * Uma rota publica que aceita lista aceita tambem dez mil nomes, e cada
     * nome preenche um PDF. Seria derrubar o servidor com um `curl`.
     */
    @Test
    @DisplayName("POST /batch - usuario sem ADMIN nao gera lote")
    @WithMockUser(roles = "VENDEDOR")
    void batchDeveRecusarQuemNaoEhAdmin() throws Exception {
        mockMvc.perform(post("/api/certificate/batch")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(
                                new CertificateBatchDTO(List.of("Ana")))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /batch - sem autenticacao devolve 403")
    void batchDeveRecusarSemAutenticacao() throws Exception {
        mockMvc.perform(post("/api/certificate/batch")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(
                                new CertificateBatchDTO(List.of("Ana")))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /batch - ADMIN recebe o ZIP")
    @WithMockUser(roles = "ADMIN")
    void batchDeveDevolverOZipParaAdmin() throws Exception {
        when(generator.generateCertificatesZip(any())).thenReturn("zip".getBytes());

        mockMvc.perform(post("/api/certificate/batch")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(
                                new CertificateBatchDTO(List.of("Ana", "Bruno")))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("certificados.zip")));
    }

    /**
     * O teto de 200 existe porque o ZIP e montado em memoria e PDF com imagem
     * de fundo nao comprime -- o arquivo final e a soma dos PDFs.
     */
    @Test
    @DisplayName("POST /batch - acima de 200 nomes devolve 400")
    @WithMockUser(roles = "ADMIN")
    void batchDeveRecusarListaAcimaDoTeto() throws Exception {
        List<String> duzentosEUm = IntStream.rangeClosed(1, 201)
                .mapToObj(i -> "Pessoa " + i)
                .toList();

        mockMvc.perform(post("/api/certificate/batch")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(
                                new CertificateBatchDTO(duzentosEUm))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /batch - lista vazia devolve 400")
    @WithMockUser(roles = "ADMIN")
    void batchDeveRecusarListaVazia() throws Exception {
        mockMvc.perform(post("/api/certificate/batch")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(
                                new CertificateBatchDTO(Collections.emptyList()))))
                .andExpect(status().isBadRequest())
                // A mensagem da anotacao tem que chegar. Um handler que responde
                // 400 com "Dados invalidos." passaria no status e nao serviria
                // para nada -- e o formulario publico de contato depende disso.
                .andExpect(jsonPath("$.message").value("Envie pelo menos um nome"));
    }

    /** Linha em branco e erro de quem chamou, nao algo para o servidor adivinhar. */
    @Test
    @DisplayName("POST /batch - nome em branco na lista devolve 400")
    @WithMockUser(roles = "ADMIN")
    void batchDeveRecusarNomeEmBranco() throws Exception {
        mockMvc.perform(post("/api/certificate/batch")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(
                                new CertificateBatchDTO(List.of("Ana", "   ")))))
                .andExpect(status().isBadRequest());
    }
}
