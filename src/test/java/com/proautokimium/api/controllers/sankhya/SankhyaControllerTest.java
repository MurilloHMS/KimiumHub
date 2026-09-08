package com.proautokimium.api.controllers.sankhya;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.sankhya.SankhyaQueryDTO;
import com.proautokimium.api.Infrastructure.exceptions.sankhya.SankhyaException;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.SecurityConfiguration;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.permission.PermissionService;
import com.proautokimium.api.Infrastructure.services.sankhya.SankhyaQueryService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A porta da integração com o Sankhya.
 *
 * **É um endpoint que recebe SQL.** A fronteira de segurança real é o usuário
 * do Sankhya, que é só de leitura — mas a permissão daqui é o que decide QUEM
 * consegue chegar a ele, e esconder o botão na tela não protege rota nenhuma.
 *
 * O caso da consulta vazia existe por um motivo prático: sem `@Valid` no
 * parâmetro **e** `@NotBlank` no record — as duas metades da mesma coisa — a
 * string vazia desceria até o ERP, e o erro voltaria falando de SQL em vez de
 * falar do pedido.
 */
@WebMvcTest(SankhyaController.class)
@TestPropertySource(properties = {"server.port=0"})
@Import(SecurityConfiguration.class)
class SankhyaControllerTest {

    private static final String ROTA = "/api/sankhya/query";
    private static final String PERMISSAO = "integracao/sankhya:CONSULTAR";

    private static final String RESULTADO = """
            {"fieldsMetadata":[{"name":"TOTAL","order":1,"userType":"I"}],"rows":[[8910]]}""";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean SankhyaQueryService queryService;
    @MockitoBean PermissionService permissionService;
    @MockitoBean UserRepository userRepository;
    @MockitoBean TokenService tokenService;
    @MockitoBean AuthenticationManager authenticationManager;

    private String pedido(String sql) throws Exception {
        return objectMapper.writeValueAsString(new SankhyaQueryDTO(sql));
    }

    // ─── A rota ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("responde em /api/sankhya/query e devolve JSON")
    @WithMockUser(authorities = PERMISSAO)
    void consultaComPermissao() throws Exception {
        when(queryService.query(any())).thenReturn(RESULTADO);

        mockMvc.perform(post(ROTA).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido("SELECT COUNT(*) FROM TGFPAR")))
                .andExpect(status().isOk())
                // `produces` importa: sem ele, String sai como text/plain e a
                // biblioteca do outro lado pode recusar a parsear.
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(RESULTADO));
    }

    /**
     * O SQL vai no CORPO, e não na query string — decisão dele. Na URL ele
     * apareceria em log de acesso e em histórico de proxy, e o chamador teria
     * que URL-encodar a consulta inteira.
     */
    @Test
    @DisplayName("lê o SQL do corpo, e é ele que chega no serviço")
    @WithMockUser(authorities = PERMISSAO)
    void leOSqlDoCorpo() throws Exception {
        when(queryService.query(any())).thenReturn(RESULTADO);

        mockMvc.perform(post(ROTA).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido("SELECT TOP 10 CODPARC FROM TGFPAR")))
                .andExpect(status().isOk());

        verify(queryService).query("SELECT TOP 10 CODPARC FROM TGFPAR");
    }

    // ─── A permissão ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("sem a permissão, 403 — e o ERP nem é consultado")
    @WithMockUser(authorities = "rh/employees:CONSULTAR")
    void semPermissaoNaoPassa() throws Exception {
        mockMvc.perform(post(ROTA).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido("SELECT COUNT(*) FROM TGFPAR")))
                .andExpect(status().isForbidden());

        verify(queryService, never()).query(any());
    }

    @Test
    @DisplayName("sem autenticação nenhuma, não passa")
    void anonimoNaoPassa() throws Exception {
        mockMvc.perform(post(ROTA).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido("SELECT COUNT(*) FROM TGFPAR")))
                .andExpect(status().is4xxClientError());

        verify(queryService, never()).query(any());
    }

    // ─── O pedido ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("consulta vazia para aqui, e não no ERP")
    @WithMockUser(authorities = PERMISSAO)
    void consultaVaziaNaoDesce() throws Exception {
        mockMvc.perform(post(ROTA).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido("")))
                .andExpect(status().isBadRequest());

        verify(queryService, never()).query(any());
    }

    @Test
    @DisplayName("consulta só com espaços também para aqui")
    @WithMockUser(authorities = PERMISSAO)
    void consultaEmBrancoNaoDesce() throws Exception {
        mockMvc.perform(post(ROTA).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido("   ")))
                .andExpect(status().isBadRequest());

        verify(queryService, never()).query(any());
    }

    // ─── A falha do ERP ───────────────────────────────────────────────────────

    /**
     * O `DomainExceptionHandler` transforma a `SankhyaException` em resposta
     * com a mensagem dentro — é isso que faz o erro do ERP chegar legível em
     * quem chamou, em vez de virar stack trace.
     */
    @Test
    @DisplayName("erro do Sankhya vira resposta com a mensagem dele")
    @WithMockUser(authorities = PERMISSAO)
    void erroDoSankhyaViraResposta() throws Exception {
        when(queryService.query(any()))
                .thenThrow(new SankhyaException("Nome de coluna 'ROWNUM' invalido."));

        mockMvc.perform(post(ROTA).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido("SELECT * FROM TGFPAR WHERE ROWNUM <= 1")))
                .andExpect(status().is4xxClientError());
    }
}
