package com.proautokimium.api.Infrastructure.services.sankhya;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Infrastructure.exceptions.sankhya.SankhyaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class SankhyaLoginService {

    private final String host;
    private final String username;
    private final String password;
    private final RestClient http;
    private final ObjectMapper mapper;

    public SankhyaLoginService(
            @Value("${sankhya.host:}") String host,
            @Value("${sankhya.usuario:}") String username,
            @Value("${sankhya.senha:}") String password,
            RestClient.Builder builder,
            ObjectMapper mapper
    ){
        this.host = host;
        this.username = username;
        this.password = password;
        this.http =  builder.build();
        this.mapper = mapper;
    }

    public String authenticate(){
        if(host.isBlank() || username.isBlank() || password.isBlank()){
            throw new SankhyaException("Serviço não configurado: defina um host, usuário e senha.");
        }

        String body = """
            {"serviceName":"MobileLoginSP.login",
             "requestBody":{"NOMUSU":{"$":"%s"},"INTERNO":{"$":"%s"}}}
            """.formatted(username, password);

        ResponseEntity<String> response;

        try{
            response = http.post()
                    .uri(host + "/mge/service.sbr?serviceName=MobileLoginSP.login&outputType=json")
                    .contentType(MediaType.APPLICATION_JSON)
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
                throw new SankhyaException("O sankhya recusou o login: " + envelope.path("statusMessage").asText("sem mensagem"));
            }
        } catch (JsonProcessingException ex) {
            // Servidor de aplicacao caido, HTML de proxy, resposta truncada: o
            // corpo nao e JSON. Sem isto, quem chama recebe 500 com stack trace
            // em vez da mensagem — e todo o cuidado das outras falhas se perde
            // justamente no caso mais confuso de diagnosticar.
            throw new SankhyaException("O Sankhya respondeu algo que nao e JSON: " + ex.getOriginalMessage());
        }

        String header = response.getHeaders().get("Set-Cookie").stream()
                .filter(c -> c.startsWith("JSESSIONID="))
                .findFirst()
                .orElse(null);

        if(header == null){
            throw new SankhyaException("Respondeu sem o cabecalho esperado.");
        }

        return header.split(";",2)[0];
    }
}
