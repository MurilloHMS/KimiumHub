package com.proautokimium.api.Infrastructure.utils;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * Uma linha do Sankhya, já com nome de coluna.
 *
 * O ERP devolve as colunas numa lista e os valores noutra, na mesma ordem — não
 * são objetos com chave. Quem casa as duas é {@link SankhyaRows}; esta classe é
 * o resultado, e existe para o resto do código pedir `linha.decimal(...)` em vez
 * de andar com índices na mão.
 *
 * **Coluna ausente devolve o vazio do tipo, e não estoura.** As consultas da
 * newsletter são seis, cada uma com o seu conjunto de colunas; quem lê uma
 * coluna que aquela consulta não traz tem um erro de programação, mas derrubar
 * o lote inteiro de 913 clientes por causa disso seria pior que o defeito.
 */
public final class LinhaSankhya {

    private final Map<String, JsonNode> valores;

    LinhaSankhya(Map<String, JsonNode> valores) {
        this.valores = valores;
    }

    private JsonNode no(String coluna) {
        JsonNode valor = valores.get(coluna);
        return valor == null ? null : valor;
    }

    /** Texto já sem espaço nas pontas; `null` quando a coluna veio vazia. */
    public String texto(String coluna) {
        JsonNode valor = no(coluna);
        if (valor == null || valor.isNull()) {
            return null;
        }

        String texto = valor.asText().trim();
        return texto.isEmpty() ? null : texto;
    }

    public int inteiro(String coluna) {
        JsonNode valor = no(coluna);
        return valor == null || valor.isNull() ? 0 : valor.asInt();
    }

    public double decimal(String coluna) {
        JsonNode valor = no(coluna);
        return valor == null || valor.isNull() ? 0d : valor.asDouble();
    }

    /**
     * O Sankhya manda `bit` como 0 e 1, e às vezes como texto. `asBoolean()`
     * sozinho devolveria `false` para a string "1", que é o formato em que o
     * `mau_uso` chega.
     */
    public boolean booleano(String coluna) {
        JsonNode valor = no(coluna);
        if (valor == null || valor.isNull()) {
            return false;
        }

        if (valor.isNumber()) {
            return valor.asInt() != 0;
        }

        String texto = valor.asText().trim();
        return "1".equals(texto) || "true".equalsIgnoreCase(texto) || "S".equalsIgnoreCase(texto);
    }
}
