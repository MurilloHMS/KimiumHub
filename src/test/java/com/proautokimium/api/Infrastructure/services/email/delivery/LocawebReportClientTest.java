package com.proautokimium.api.Infrastructure.services.email.delivery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * A resposta tem o formato medido na API de produção em 2026-10-07 (os
 * valores aqui são inventados). A documentação da Locaweb não traz exemplo.
 */
class LocawebReportClientTest {

    private static final String RESPOSTA = """
            {"data":{"messages":[
              {"id":25163,"subject":"Seu código","sender":"noreply@envios.proautokimium.com.br",
               "recipient":"ana@x.com","sent_at":"2026-10-07T14:59:15.625-03:00","account_id":1,
               "created_at":"2026-10-07T14:59:15.783-03:00","updated_at":"2026-10-07T15:00:20.268-03:00",
               "uid":"2b957042452dfc824d0121a11b9190b9-1791395955.64","status":"Entregue","bounce_code":null,
               "bounced_at":null,"pool":"1","x_smtplw":"6f1c-tag","is_spam":false,"date":"2026-10-07",
               "opened_at":"2026-10-07T15:00:20.000-03:00","hard_bounce":null,"api_message_id":null,
               "bounce_description":""}]},
             "links":{"self":"x","next":"https://api.smtplw.com.br/v1/messages?page=2","prev":null,"first":"x","last":"x"}}
            """;

    @Test
    @DisplayName("pede o período com o token no cabeçalho e lê a marca, o status e a hora do registro")
    void leAResposta() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.smtplw.com.br/v1/messages?status=all&start_date=2026-10-06&end_date=2026-10-07&page=2&per=50"))
                .andExpect(header("x-auth-token", "segredo"))
                .andRespond(withSuccess(RESPOSTA, MediaType.APPLICATION_JSON));
        LocawebReportClient client = new LocawebReportClient(builder, "https://api.smtplw.com.br", "segredo");

        LocawebReportClient.ReportPage page = client.messages(LocalDate.of(2026, 10, 6), LocalDate.of(2026, 10, 7), 2);

        assertThat(page.hasNext()).isTrue();
        assertThat(page.messages()).singleElement().satisfies(m -> {
            assertThat(m.trackingTag()).isEqualTo("6f1c-tag");
            assertThat(m.isDelivered()).isTrue();
            assertThat(m.isBounced()).isFalse();
            assertThat(m.createdAt()).isEqualTo(OffsetDateTime.parse("2026-10-07T14:59:15.783-03:00"));
        });
        server.verify();
    }

    @Test
    @DisplayName("sem token, desligado: os testes e a API em casa não chamam a Locaweb")
    void semTokenDesligado() {
        assertThat(new LocawebReportClient(RestClient.builder(), "https://api.smtplw.com.br", "").isEnabled()).isFalse();
        assertThat(new LocawebReportClient(RestClient.builder(), "https://api.smtplw.com.br", "  ").isEnabled()).isFalse();
        assertThat(new LocawebReportClient(RestClient.builder(), "https://api.smtplw.com.br", "t").isEnabled()).isTrue();
    }
}
