package com.proautokimium.api.Infrastructure.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Infrastructure.exceptions.sankhya.SankhyaException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Traduz o `responseBody` do Sankhya em linhas.
 *
 * O ERP responde assim:
 *
 * <pre>
 * {"fieldsMetadata":[{"name":"codigo_cliente"},{"name":"faturamento_total"}],
 *  "rows":[[8781, 4861.07]]}
 * </pre>
 *
 * Colunas de um lado, valores do outro, casados pelo índice. Fica aqui, e não
 * dentro do serviço da newsletter, porque vale para qualquer consulta — a
 * mesma tradução já existe no front, em `sankhya-resultado.ts`.
 */
public final class SankhyaRows {

    private SankhyaRows() {
    }

    public static List<LinhaSankhya> emLinhas(String responseBody, ObjectMapper mapper) {
        JsonNode raiz;
        try {
            raiz = mapper.readTree(responseBody);
        } catch (JsonProcessingException e) {
            throw new SankhyaException("Não consegui ler a resposta do Sankhya: " + e.getOriginalMessage());
        }

        JsonNode colunas = raiz.path("fieldsMetadata");
        JsonNode linhas = raiz.path("rows");

        if (!colunas.isArray() || !linhas.isArray()) {
            return List.of();
        }

        List<String> nomes = new ArrayList<>(colunas.size());
        for (JsonNode coluna : colunas) {
            nomes.add(coluna.path("name").asText());
        }

        List<LinhaSankhya> resultado = new ArrayList<>(linhas.size());
        for (JsonNode valores : linhas) {
            // `LinkedHashMap` de propósito: preserva a ordem das colunas, que é
            // o que faz um dump de depuração sair na mesma ordem da consulta.
            Map<String, JsonNode> registro = new LinkedHashMap<>();

            for (int i = 0; i < nomes.size(); i++) {
                registro.put(nomes.get(i), i < valores.size() ? valores.get(i) : null);
            }

            resultado.add(new LinhaSankhya(registro));
        }

        return resultado;
    }
}
