package com.proautokimium.api.Infrastructure.services.email.delivery;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * O relatório de mensagens do SMTP Locaweb ({@code GET /v1/messages}).
 *
 * <p>A documentação publicada não traz o formato da resposta: os campos abaixo
 * foram medidos com o token de produção em 2026-10-07. O que importa:
 * <ul>
 *   <li>{@code x_smtplw} é o valor do cabeçalho X-SMTPLW que mandamos;</li>
 *   <li>não há {@code delivered_at}: o registro nasce já com o resultado, a
 *       menos de 1 s do envio, e nas mensagens não abertas o {@code updated_at}
 *       nunca muda depois. Então {@code created_at} é a hora da entrega. O
 *       {@code updated_at} NÃO serve: ele acompanha a abertura.</li>
 * </ul>
 */
@Component
public class LocawebReportClient {

    /** Medido funcionando; a documentação não diz o máximo. */
    static final int PAGE_SIZE = 50;

    private final RestClient http;
    private final String token;

    public LocawebReportClient(RestClient.Builder builder,
                               @Value("${smtp.report.api-url:https://api.smtplw.com.br}") String apiUrl,
                               @Value("${smtp.report.token:}") String token) {
        this.http = builder.baseUrl(apiUrl).build();
        this.token = token == null ? "" : token.trim();
    }

    /** Sem token, o rastreio fica desligado: testes e a API rodando em casa. */
    public boolean isEnabled() {
        return !token.isEmpty();
    }

    /** Uma página das mensagens do período (datas no fuso de São Paulo, como as da Locaweb). */
    public ReportPage messages(LocalDate from, LocalDate to, int page) {
        Response r = http.get()
                .uri(u -> u.path("/v1/messages")
                        .queryParam("status", "all")
                        .queryParam("start_date", from)
                        .queryParam("end_date", to)
                        .queryParam("page", page)
                        .queryParam("per", PAGE_SIZE)
                        .build())
                .header("x-auth-token", token)
                .retrieve()
                .body(Response.class);
        if (r == null || r.data() == null || r.data().messages() == null) return new ReportPage(List.of(), false);
        return new ReportPage(r.data().messages(), r.links() != null && r.links().next() != null);
    }

    public record ReportPage(List<ReportedMessage> messages, boolean hasNext) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReportedMessage(
            @JsonProperty("x_smtplw") String trackingTag,
            String status,
            @JsonProperty("created_at") OffsetDateTime createdAt,
            @JsonProperty("bounced_at") OffsetDateTime bouncedAt,
            @JsonProperty("bounce_code") String bounceCode,
            @JsonProperty("bounce_description") String bounceDescription) {

        /** "Entregue" foi o único status visto; "delivered" é o nome do filtro da API. */
        public boolean isDelivered() {
            return status != null && (status.equalsIgnoreCase("Entregue") || status.equalsIgnoreCase("delivered"));
        }

        public boolean isBounced() {
            return bouncedAt != null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Response(Data data, Links links) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Data(List<ReportedMessage> messages) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Links(String next) {}
}
