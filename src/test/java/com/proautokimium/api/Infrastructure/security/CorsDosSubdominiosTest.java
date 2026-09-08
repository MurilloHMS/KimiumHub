package com.proautokimium.api.Infrastructure.security;

import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.services.permission.PermissionService;
import com.proautokimium.api.Infrastructure.services.sankhya.SankhyaQueryService;
import com.proautokimium.api.controllers.sankhya.SankhyaController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Quem o navegador deixa falar com esta API.
 *
 * Cada serviço da empresa vive num subdomínio próprio — `integracao.`, `cmv.`,
 * e os que vierem — nos dois domínios. Por isso a configuração usa
 * `setAllowedOriginPatterns` com curinga, e **não** `setAllowedOrigins`: só o
 * primeiro aceita curinga junto com `allowCredentials(true)`. Trocar um pelo
 * outro faz o navegador recusar toda resposta, e o erro que aparece no console
 * não fala de credenciais.
 *
 * **O caso que justifica este arquivo é o do domínio-armadilha.** Um curinga
 * escrito com pressa aceita `proautokimium.com.br.dominio-de-alguem.com`, que
 * termina igual e é de outra pessoa. Aqui isso fica travado.
 *
 * CORS é regra de navegador. O lote que consome o Sankhya não é navegador e
 * ignora tudo isto — quem protege aquele endpoint é a permissão, não daqui.
 */
@WebMvcTest(SankhyaController.class)
@TestPropertySource(properties = {"server.port=0"})
@Import(SecurityConfiguration.class)
class CorsDosSubdominiosTest {

    private static final String ROTA = "/api/sankhya/query";

    @Autowired MockMvc mockMvc;

    @MockitoBean SankhyaQueryService queryService;
    @MockitoBean PermissionService permissionService;
    @MockitoBean UserRepository userRepository;
    @MockitoBean TokenService tokenService;
    @MockitoBean AuthenticationManager authenticationManager;

    /** Simula o preflight que o navegador manda antes da requisição de verdade. */
    private void aceita(String origem) throws Exception {
        mockMvc.perform(options(ROTA)
                        .header("Origin", origem)
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", origem));
    }

    private void recusa(String origem) throws Exception {
        mockMvc.perform(options(ROTA)
                        .header("Origin", origem)
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    // ─── Os que precisam passar ───────────────────────────────────────────────

    @Test
    @DisplayName("qualquer serviço em .proautokimium.com.br entra")
    void subdominiosDoComBr() throws Exception {
        aceita("https://integracao.proautokimium.com.br");
        aceita("https://cmv.proautokimium.com.br");
        aceita("https://app.proautokimium.com.br");
    }

    @Test
    @DisplayName("qualquer serviço em .proautokimium.com entra")
    void subdominiosDoCom() throws Exception {
        aceita("https://integracao.proautokimium.com");
        aceita("https://cmv.proautokimium.com");
    }

    @Test
    @DisplayName("o domínio sem subdomínio também entra")
    void dominioRaiz() throws Exception {
        aceita("https://proautokimium.com.br");
        aceita("https://proautokimium.com");
    }

    // ─── Os que precisam ficar de fora ────────────────────────────────────────

    /**
     * **O teste que importa.** `proautokimium.com.br.dominio-de-alguem.com`
     * termina parecido e pertence a outra pessoa. Um curinga frouxo aceita.
     */
    @Test
    @DisplayName("domínio que só TERMINA parecido não entra")
    void dominioArmadilha() throws Exception {
        recusa("https://proautokimium.com.br.dominio-de-alguem.com");
        recusa("https://proautokimium.com.evil.com");
        recusa("https://naoeproautokimium.com.br");
    }

    @Test
    @DisplayName("domínio de fora não entra")
    void dominioDeFora() throws Exception {
        recusa("https://outroservico.com.br");
    }

    /**
     * Só `https` está na lista. Um serviço servido em `http` seria recusado —
     * o que é intencional, mas vale saber antes de subir algo sem TLS e passar
     * a tarde procurando o motivo.
     */
    @Test
    @DisplayName("o mesmo subdomínio em http não entra")
    void semTlsNaoEntra() throws Exception {
        recusa("http://integracao.proautokimium.com.br");
    }
}
