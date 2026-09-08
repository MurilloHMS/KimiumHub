package com.proautokimium.api.Infrastructure.services.sankhya;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.proautokimium.api.Infrastructure.exceptions.sankhya.SankhyaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class SankhyaQueryService {

    private final SankhyaLoginService sankhyaLoginService;
    private final RestClient http;
    private final ObjectMapper mapper;
    private final String host;

    public SankhyaQueryService(
            @Value("${sankhya.host:}") String host,
            SankhyaLoginService sankhyaLoginService,
            RestClient.Builder builder,
            ObjectMapper mapper) {
        this.sankhyaLoginService = sankhyaLoginService;
        this.http = builder.build();
        this.mapper = mapper;
        this.host = host;
    }

    public String query(String query){
        String cookie = sankhyaLoginService.authenticate();

        // Montado pelo Jackson, e nao por formatacao de string: um SQL com
        // aspas dentro — `WHERE NOME = 'Joao "Ze" Ltda'` — quebraria o JSON, e
        // so quebraria no dia em que alguem consultasse esse parceiro.
        String body;
        try {
            ObjectNode requestBody = mapper.createObjectNode();
            requestBody.put("sql", query);

            ObjectNode envelope = mapper.createObjectNode();
            envelope.put("serviceName", "DbExplorerSP.executeQuery");
            envelope.set("requestBody", requestBody);

            body = mapper.writeValueAsString(envelope);
        } catch (JsonProcessingException ex) {
            throw new SankhyaException("Nao consegui montar a requisicao: " + ex.getOriginalMessage());
        }

        ResponseEntity<String> response;

        try{
            response = http.post()
                    .uri(host + "/mge/service.sbr?serviceName=DbExplorerSP.executeQuery&outputType=json")
                    .contentType(MediaType.APPLICATION_JSON)
                    // `.header` e nao `.cookie`: o valor ja vem completo do
                    // authenticate(), como "JSESSIONID=abc". Com `.cookie` o
                    // cabecalho sairia "Cookie: Cookie=JSESSIONID=abc", e o
                    // Sankhya responderia "Nao autorizado" — o mesmo erro que
                    // escondeu a sessao por dois dias.
                    .header("Cookie", cookie)
                    .body(body)
                    .retrieve()
                    .toEntity(String.class);
        }catch (RestClientException e){
            // 6. Aqui caem rede fora, DNS, conexao recusada, timeout.
            //    O outro lado nem respondeu — a nossa mensagem e tudo que sobra.
            throw new SankhyaException("Não alcancei " + host + ": " + e.getMessage());
        }

        try {
            JsonNode envelope = mapper.readTree(response.getBody());

            if(!"1".equals(envelope.path("status").asText())){
                throw new SankhyaException("O Sankhya recusou a consulta: " + envelope.path("statusMessage").asText("sem mensagem"));
            }

            // Devolve so o `responseBody` — `fieldsMetadata` e `rows`. O
            // envelope de fora carrega `transactionId` e `pendingPrinting`,
            // que nao dizem nada para quem consome.
            return envelope.path("responseBody").toString();

        } catch (JsonProcessingException ex) {
            throw new SankhyaException("O Sankhya respondeu algo que nao e JSON: " + ex.getOriginalMessage());
        }
    }
}
