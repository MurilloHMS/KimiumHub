package com.proautokimium.api.Infrastructure.services.sankhya;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Infrastructure.exceptions.sankhya.SankhyaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * O login no Sankhya, sem falar com o Sankhya.
 *
 * **Por que fingir o ERP em vez de chamar o de verdade.** A instalação é
 * hospedada por terceiros. Um teste que depende dela falha quando a optecit
 * reinicia a máquina — e um teste que falha por motivo alheio ensina a equipe a
 * ignorar a suíte, que é o pior estrago possível.
 *
 * O que estes testes travam é o que já custou caro de descobrir:
 *
 * - o **formato do corpo**, que o ERP recusa com uma mensagem que fala de XML
 *   quando o problema é outro;
 * - que **HTTP 200 não é sucesso** — o envelope traz `status`, e `"0"` é falha;
 * - que a sessão vem no **cabeçalho**, e só o pedaço antes do `;` interessa.
 */
class SankhyaLoginServiceTest {

    private static final String HOST = "http://erp-de-mentira:50224";
    private static final String URL_LOGIN =
            HOST + "/mge/service.sbr?serviceName=MobileLoginSP.login&outputType=json";

    private MockRestServiceServer erp;

    /** Monta o serviço com um ERP de mentira no lugar da rede. */
    private SankhyaLoginService servico(String host, String usuario, String senha) {
        RestClient.Builder builder = RestClient.builder();
        erp = MockRestServiceServer.bindTo(builder).build();
        return new SankhyaLoginService(host, usuario, senha, builder, new ObjectMapper());
    }

    private SankhyaLoginService servico() {
        return servico(HOST, "integracao", "segredo");
    }

    private static String envelopeDeSucesso() {
        return """
               {"serviceName":"MobileLoginSP.login","status":"1",
                "responseBody":{"jsessionid":{"$":"abc123"}}}
               """;
    }

    private static HttpHeaders comCookie(String valor) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, valor);
        return headers;
    }

    // ─── O corpo ──────────────────────────────────────────────────────────────

    /**
     * **O teste que pega o erro que o ERP pegou.**
     *
     * A primeira versão mandou `{"login":{"usuario":...}}`, que é o formato de
     * um exemplo genérico. O Sankhya respondeu `status: "0"` reclamando de XML —
     * mensagem que não tem nada a ver com o defeito.
     */
    @Test
    @DisplayName("manda o envelope que o Sankhya espera, com NOMUSU e INTERNO")
    void mandaOEnvelopeCerto() {
        SankhyaLoginService servico = servico();

        erp.expect(requestTo(URL_LOGIN))
           .andExpect(method(HttpMethod.POST))
           .andExpect(jsonPath("$.serviceName").value("MobileLoginSP.login"))
           .andExpect(jsonPath("$.requestBody.NOMUSU['$']").value("integracao"))
           .andExpect(jsonPath("$.requestBody.INTERNO['$']").value("segredo"))
           .andRespond(withSuccess(envelopeDeSucesso(), MediaType.APPLICATION_JSON)
                   .headers(comCookie("JSESSIONID=abc123; path=/mge")));

        servico.authenticate();

        erp.verify();
    }

    @Test
    @DisplayName("manda como JSON, e não como formulário")
    void mandaComoJson() {
        SankhyaLoginService servico = servico();

        erp.expect(requestTo(URL_LOGIN))
           .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
           .andRespond(withSuccess(envelopeDeSucesso(), MediaType.APPLICATION_JSON)
                   .headers(comCookie("JSESSIONID=abc123; path=/mge")));

        servico.authenticate();

        erp.verify();
    }

    // ─── A sessão ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("devolve só o JSESSIONID, sem o path que vem junto")
    void devolveSoOCookie() {
        SankhyaLoginService servico = servico();

        erp.expect(requestTo(URL_LOGIN))
           .andRespond(withSuccess(envelopeDeSucesso(), MediaType.APPLICATION_JSON)
                   .headers(comCookie("JSESSIONID=abc123.master; path=/mge")));

        assertThat(servico.authenticate())
                .withFailMessage("o `; path=/mge` nao pode ir junto no header Cookie")
                .isEqualTo("JSESSIONID=abc123.master");
    }

    /**
     * O servidor pode mandar mais de um `Set-Cookie` na mesma resposta. Pegar o
     * primeiro daria o cookie errado — por isso a lista é filtrada.
     */
    @Test
    @DisplayName("acha o JSESSIONID mesmo quando não é o primeiro cookie")
    void achaEntreVariosCookies() {
        SankhyaLoginService servico = servico();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, "BIGipServer=algum-balanceador; path=/");
        headers.add(HttpHeaders.SET_COOKIE, "JSESSIONID=abc123; path=/mge");

        erp.expect(requestTo(URL_LOGIN))
           .andRespond(withSuccess(envelopeDeSucesso(), MediaType.APPLICATION_JSON).headers(headers));

        assertThat(servico.authenticate()).isEqualTo("JSESSIONID=abc123");
    }

    // ─── As falhas ────────────────────────────────────────────────────────────

    /**
     * **HTTP 200 não é sucesso.** O Sankhya responde 200 e conta o problema no
     * envelope. Sem esta checagem o código seguiria, não acharia cookie, e
     * culparia o cabeçalho — escondendo a mensagem que explica tudo.
     */
    @Test
    @DisplayName("status 0 vira erro com a mensagem do Sankhya, não do cabeçalho")
    void statusZeroViraErro() {
        SankhyaLoginService servico = servico();

        erp.expect(requestTo(URL_LOGIN))
           .andRespond(withSuccess("""
                   {"serviceName":"MobileLoginSP.login","status":"0",
                    "statusMessage":"Usuario ou senha invalidos."}
                   """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(servico::authenticate)
                .isInstanceOf(SankhyaException.class)
                .hasMessageContaining("Usuario ou senha invalidos")
                .withFailMessage("a mensagem do ERP tem que sobreviver ate quem chamou")
                .hasMessageNotContaining("cabecalho");
    }

    @Test
    @DisplayName("resposta que não é JSON vira erro legível, não stack trace")
    void respostaQueNaoEJson() {
        SankhyaLoginService servico = servico();

        erp.expect(requestTo(URL_LOGIN))
           .andRespond(withSuccess("<html><body>502 Bad Gateway</body></html>", MediaType.TEXT_HTML));

        assertThatThrownBy(servico::authenticate)
                .isInstanceOf(SankhyaException.class);
    }

    @Test
    @DisplayName("erro de rede vira SankhyaException dizendo qual host")
    void erroDeRede() {
        SankhyaLoginService servico = servico();

        erp.expect(requestTo(URL_LOGIN)).andRespond(withServerError());

        assertThatThrownBy(servico::authenticate)
                .isInstanceOf(SankhyaException.class)
                .hasMessageContaining(HOST);
    }

    // ─── A configuração ───────────────────────────────────────────────────────

    /**
     * Com `:` no default das propriedades, faltar variável de ambiente vira
     * string vazia — não `null`. A guarda precisa testar vazio, e precisa
     * disparar **antes** de qualquer requisição sair.
     */
    @Test
    @DisplayName("configuração vazia falha antes de chamar o ERP")
    void configuracaoVaziaNaoChamaNinguem() {
        SankhyaLoginService servico = servico("", "", "");

        assertThatThrownBy(servico::authenticate)
                .isInstanceOf(SankhyaException.class);

        // Nenhuma requisicao esperada, e nenhuma feita.
        erp.verify();
    }

    @Test
    @DisplayName("host preenchido mas usuário vazio também falha")
    void usuarioVazioTambemFalha() {
        SankhyaLoginService servico = servico(HOST, "", "segredo");

        assertThatThrownBy(servico::authenticate)
                .isInstanceOf(SankhyaException.class);

        erp.verify();
    }
}
