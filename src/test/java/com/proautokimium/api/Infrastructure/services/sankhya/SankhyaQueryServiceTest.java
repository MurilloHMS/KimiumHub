package com.proautokimium.api.Infrastructure.services.sankhya;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Infrastructure.exceptions.sankhya.SankhyaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * A consulta ao Sankhya, sem falar com o Sankhya.
 *
 * **O desenho que estes testes travam é uma decisão dele:** o serviço faz login
 * a cada consulta, em vez de guardar a sessão. Custa um round-trip a mais e
 * elimina o problema de sessão vencida — uma sessão de dois segundos não
 * expira.
 *
 * Três dos casos abaixo existem porque o erro correspondente aconteceu de
 * verdade, e o ERP respondeu de um jeito que não apontava para a causa.
 */
class SankhyaQueryServiceTest {

    private static final String HOST = "http://erp-de-mentira:50224";
    private static final String URL =
            HOST + "/mge/service.sbr?serviceName=DbExplorerSP.executeQuery&outputType=json";

    private static final String COOKIE = "JSESSIONID=abc123.master";

    private MockRestServiceServer erp;
    private SankhyaLoginService login;
    private SankhyaQueryService servico;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder();
        erp = MockRestServiceServer.bindTo(builder).build();

        login = mock(SankhyaLoginService.class);
        when(login.authenticate()).thenReturn(COOKIE);

        servico = new SankhyaQueryService(HOST, login, builder, new ObjectMapper());
    }

    private static String respostaCom(String responseBody) {
        return """
               {"serviceName":"DbExplorerSP.executeQuery","status":"1",
                "transactionId":"ABC","pendingPrinting":"false",
                "responseBody":%s}
               """.formatted(responseBody);
    }

    private static final String CORPO_CONTAGEM = """
            {"fieldsMetadata":[{"name":"TOTAL","order":1,"userType":"I"}],"rows":[[8910]]}""";

    // ─── A sessão ─────────────────────────────────────────────────────────────

    /**
     * **O erro que escondeu a sessão por dois dias.**
     *
     * `.cookie("Cookie", valor)` monta um cookie CHAMADO `Cookie`, e o cabeçalho
     * sai como `Cookie: Cookie=JSESSIONID=...`. O Sankhya trata isso como sessão
     * ausente e responde `status: 3` — "Não autorizado", que parece problema de
     * permissão e não de transporte.
     */
    @Test
    @DisplayName("manda a sessão como cabeçalho Cookie, e não como cookie chamado Cookie")
    void mandaOCookieCerto() {
        erp.expect(requestTo(URL))
           .andExpect(method(HttpMethod.POST))
           .andExpect(header("Cookie", COOKIE))
           .andRespond(withSuccess(respostaCom(CORPO_CONTAGEM), MediaType.APPLICATION_JSON));

        servico.query("SELECT COUNT(*) FROM TGFPAR");

        erp.verify();
    }

    /** A decisão dele: sessão nova a cada consulta, em vez de guardada. */
    @Test
    @DisplayName("autentica a cada consulta")
    void autenticaSempre() {
        erp.expect(requestTo(URL))
           .andRespond(withSuccess(respostaCom(CORPO_CONTAGEM), MediaType.APPLICATION_JSON));

        servico.query("SELECT COUNT(*) FROM TGFPAR");

        verify(login, times(1)).authenticate();
    }

    // ─── O corpo ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("manda o envelope com o sql dentro de requestBody")
    void mandaOEnvelopeCerto() {
        erp.expect(requestTo(URL))
           .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
           .andExpect(jsonPath("$.serviceName").value("DbExplorerSP.executeQuery"))
           .andExpect(jsonPath("$.requestBody.sql").value("SELECT COUNT(*) FROM TGFPAR"))
           .andRespond(withSuccess(respostaCom(CORPO_CONTAGEM), MediaType.APPLICATION_JSON));

        servico.query("SELECT COUNT(*) FROM TGFPAR");

        erp.verify();
    }

    /**
     * **O teste que justifica montar o JSON com Jackson.**
     *
     * Com formatação de string, um SQL com aspas dentro produz JSON inválido — e
     * só produziria no dia em que alguém consultasse um parceiro com aspas no
     * nome. O ERP responderia "Unterminated object", que não diz nada sobre o
     * nome do parceiro.
     */
    @Test
    @DisplayName("SQL com aspas e barra continua sendo JSON válido")
    void escapaOSqlCorretamente() {
        String sqlHostil = "SELECT * FROM TGFPAR WHERE NOMEPARC = 'Joao \"Ze\" Ltda' AND X = 'a\\b'";

        erp.expect(requestTo(URL))
           // Se o JSON estivesse quebrado, o jsonPath nem conseguiria ler.
           .andExpect(jsonPath("$.requestBody.sql").value(sqlHostil))
           .andRespond(withSuccess(respostaCom(CORPO_CONTAGEM), MediaType.APPLICATION_JSON));

        servico.query(sqlHostil);

        erp.verify();
    }

    // ─── A resposta ───────────────────────────────────────────────────────────

    /**
     * Devolve o miolo, não o envelope: `transactionId` e `pendingPrinting` não
     * dizem nada para quem consome.
     */
    @Test
    @DisplayName("devolve só o responseBody, com metadata e rows")
    void devolveSoOMiolo() {
        erp.expect(requestTo(URL))
           .andRespond(withSuccess(respostaCom(CORPO_CONTAGEM), MediaType.APPLICATION_JSON));

        String resultado = servico.query("SELECT COUNT(*) FROM TGFPAR");

        assertThat(resultado).contains("fieldsMetadata").contains("8910");
        assertThat(resultado)
                .withFailMessage("o envelope de fora nao interessa a quem consome")
                .doesNotContain("transactionId")
                .doesNotContain("pendingPrinting");
    }

    // ─── As falhas ────────────────────────────────────────────────────────────

    /**
     * SQL errado responde **HTTP 200** com `status: "0"`. Sem conferir isso, uma
     * consulta quebrada voltaria como sucesso vazio.
     *
     * A mensagem é real: foi o que o ERP respondeu quando escrevi `ROWNUM` —
     * o banco é SQL Server, não Oracle.
     */
    @Test
    @DisplayName("SQL inválido vira erro com a mensagem do banco")
    void sqlInvalidoViraErro() {
        erp.expect(requestTo(URL))
           .andRespond(withSuccess("""
                   {"serviceName":"DbExplorerSP.executeQuery","status":"0",
                    "statusMessage":"Nome de coluna 'ROWNUM' invalido."}
                   """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> servico.query("SELECT * FROM TGFPAR WHERE ROWNUM <= 1"))
                .isInstanceOf(SankhyaException.class)
                .hasMessageContaining("ROWNUM")
                .withFailMessage("a mensagem tem que falar de consulta, nao de login")
                .hasMessageNotContaining("login");
    }

    @Test
    @DisplayName("sessão recusada vira erro, e não resultado vazio")
    void sessaoRecusadaViraErro() {
        erp.expect(requestTo(URL))
           .andRespond(withSuccess("""
                   {"serviceName":"DbExplorerSP.executeQuery","status":"3",
                    "statusMessage":"Nao autorizado."}
                   """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> servico.query("SELECT COUNT(*) FROM TGFPAR"))
                .isInstanceOf(SankhyaException.class)
                .hasMessageContaining("Nao autorizado");
    }

    /** Se o login falha, a consulta nem chega a sair. */
    @Test
    @DisplayName("falha no login não vira requisição de consulta")
    void falhaNoLoginNaoConsulta() {
        when(login.authenticate()).thenThrow(new SankhyaException("O Sankhya recusou o login: senha invalida."));

        assertThatThrownBy(() -> servico.query("SELECT COUNT(*) FROM TGFPAR"))
                .isInstanceOf(SankhyaException.class)
                .hasMessageContaining("recusou o login");

        // Nenhuma requisicao esperada, e nenhuma feita.
        erp.verify();
    }
}
