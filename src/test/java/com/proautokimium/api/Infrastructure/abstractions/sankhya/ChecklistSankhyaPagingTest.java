package com.proautokimium.api.Infrastructure.abstractions.sankhya;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.proautokimium.api.Infrastructure.services.sales.ChecklistSankhyaQueryService;
import com.proautokimium.api.Infrastructure.services.sankhya.SankhyaQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * O DbExplorer corta em 5.000 linhas e só avisa no {@code burstLimit}: 6.853
 * clientes voltavam como 5.000, sem erro (medido em 2026-09-29). As consultas
 * do checklist pedem 4.000 por vez e continuam da última chave — este teste
 * prova que a segunda página existe e parte do lugar certo.
 *
 * Mora neste pacote para chamar o {@code loadSql()} do leitor, que o Spring
 * chamaria no {@code @PostConstruct}.
 */
class ChecklistSankhyaPagingTest {

    final ObjectMapper mapper = new ObjectMapper();
    final SankhyaQueryService sankhya = mock(SankhyaQueryService.class);

    ChecklistSankhyaQueryService reader() {
        ChecklistSankhyaQueryService reader = new ChecklistSankhyaQueryService(sankhya, mapper);
        ((SankhyaSqlReader) reader).loadSql();
        return reader;
    }

    String page(List<String> columns, int rows, java.util.function.IntFunction<Object[]> row) {
        ObjectNode body = mapper.createObjectNode();
        ArrayNode meta = body.putArray("fieldsMetadata");
        columns.forEach(c -> meta.addObject().put("name", c));
        ArrayNode data = body.putArray("rows");
        for (int i = 0; i < rows; i++) {
            ArrayNode r = data.addArray();
            for (Object v : row.apply(i)) r.addPOJO(v);
        }
        return body.toString();
    }

    @Test
    @DisplayName("clientes: página cheia pede a próxima a partir do último CODPARC; página incompleta para")
    void customersPaginate() {
        when(sankhya.query(anyString()))
                .thenReturn(page(List.of("CODPARC"), 4000, i -> new Object[]{i + 1}))
                .thenReturn(page(List.of("CODPARC"), 2853, i -> new Object[]{4001 + i}));

        assertThat(reader().customers()).hasSize(6853);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(sankhya, times(2)).query(sql.capture());
        assertThat(sql.getAllValues().get(0)).startsWith("DECLARE @ULTIMO_CLIENTE INT = 0;");
        assertThat(sql.getAllValues().get(1)).startsWith("DECLARE @ULTIMO_CLIENTE INT = 4000;");
        // O DECLARE de mentira do arquivo foi trocado, e não somado.
        assertThat(sql.getAllValues().get(1)).containsOnlyOnce("DECLARE");
    }

    @Test
    @DisplayName("preços: a chave é (NUTAB, CODPROD), porque uma tabela sozinha passa de 5.000")
    void pricesPaginateByCompositeKey() {
        when(sankhya.query(anyString()))
                .thenReturn(page(List.of("CODTAB", "NUTAB", "CODPROD", "VLRVENDA"), 4000,
                        i -> new Object[]{9000, 818, i + 1, 1.5}))
                .thenReturn(page(List.of("CODTAB", "NUTAB", "CODPROD", "VLRVENDA"), 10,
                        i -> new Object[]{9000, 818, 4001 + i, 1.5}));

        assertThat(reader().prices()).hasSize(4010);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(sankhya, times(2)).query(sql.capture());
        assertThat(sql.getAllValues().get(1))
                .startsWith("DECLARE @ULTIMA_TABELA INT = 818, @ULTIMO_PRODUTO INT = 4000;");
    }

    @Test
    @DisplayName("página vazia na primeira chamada: uma consulta só")
    void emptyStops() {
        when(sankhya.query(anyString())).thenReturn(page(List.of("CODPROD"), 0, i -> new Object[]{}));
        assertThat(reader().products()).isEmpty();
        verify(sankhya, times(1)).query(anyString());
    }
}
