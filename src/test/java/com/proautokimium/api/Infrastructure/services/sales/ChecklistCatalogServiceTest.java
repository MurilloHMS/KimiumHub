package com.proautokimium.api.Infrastructure.services.sales;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.proautokimium.api.Application.DTOs.sales.ChecklistCatalogDTO;
import com.proautokimium.api.Infrastructure.repositories.sales.ChecklistComodatoItemRepository;
import com.proautokimium.api.Infrastructure.repositories.sales.ChecklistVisualItemRepository;
import com.proautokimium.api.Infrastructure.utils.LinhaSankhya;
import com.proautokimium.api.Infrastructure.utils.SankhyaRows;
import com.proautokimium.api.domain.entities.sales.ChecklistComodatoItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/** O catálogo do celular montado a partir do Sankhya. */
class ChecklistCatalogServiceTest {

    final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    final ChecklistSankhyaQueryService sankhya = mock(ChecklistSankhyaQueryService.class);
    final ChecklistComodatoItemRepository comodato = mock(ChecklistComodatoItemRepository.class);
    final ChecklistVisualItemRepository visual = mock(ChecklistVisualItemRepository.class);
    final Clock clock = Clock.fixed(Instant.parse("2026-09-30T13:00:00Z"), ZoneId.of("America/Sao_Paulo"));

    ChecklistCatalogService service;

    List<LinhaSankhya> rows(List<Map<String, Object>> rows) {
        ObjectNode body = mapper.createObjectNode();
        ArrayNode meta = body.putArray("fieldsMetadata");
        List<String> columns = rows.isEmpty() ? List.of() : List.copyOf(rows.get(0).keySet());
        columns.forEach(c -> meta.addObject().put("name", c));
        ArrayNode data = body.putArray("rows");
        for (Map<String, Object> row : rows) {
            ArrayNode r = data.addArray();
            columns.forEach(c -> r.addPOJO(row.get(c)));
        }
        return SankhyaRows.emLinhas(body.toString(), mapper);
    }

    static Map<String, Object> customer(int code, int table) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("CODPARC", code);
        m.put("NOMEPARC", "CLIENTE " + code);
        m.put("CGC_CPF", "11222333000181");
        m.put("TIPOLOGRADOURO", "Avenida");
        m.put("LOGRADOURO", "FRANCISCO GLICERIO");
        m.put("CODTAB", table);
        return m;
    }

    static Map<String, Object> product(int code, String name, String usage, String unit, String alt, Double altQty) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("CODPROD", code);
        m.put("DESCRPROD", name);
        m.put("USOPROD", usage);
        m.put("CODGRUPOPROD", 100008001);
        m.put("CODVOL", unit);
        m.put("IPI", 3.25);
        m.put("EMBALAGEM", alt);
        m.put("QTDEMBALAGEM", altQty);
        return m;
    }

    static Map<String, Object> price(int table, int product, double value) {
        return Map.of("CODTAB", table, "NUTAB", table * 10, "CODPROD", product, "VLRVENDA", value);
    }

    @BeforeEach
    void setUp() {
        service = new ChecklistCatalogService(sankhya, comodato, visual, mapper, clock);
        when(sankhya.customers()).thenReturn(rows(List.of(customer(1, 281), customer(2, 0))));
        when(sankhya.products()).thenReturn(rows(List.of(
                product(197, "PROAUTO REMOCON. - 20 LT BB PRETA", "V", "LT", null, null),
                product(455, "POSEIDON - 7,5 KG GL NATURAL", "V", "KG", "GL", 7.5),
                product(29, "KIMI AB200 - 5 KG GL NATURAL", "V", "LT", null, null),
                product(1998, "DILUIDOR NTI - AZUL", "R", "UN", null, null),
                product(1107, "FRASCO GRADUADO 1 L", "E", "UN", null, null))));
        when(sankhya.prices()).thenReturn(rows(List.of(
                price(80, 197, 10.98), price(281, 197, 9.5), price(999, 197, 1.0), price(80, 1107, 2.0))));
        when(comodato.findAllByOrderBySortOrderAsc()).thenReturn(List.of(new ChecklistComodatoItem(1998, "Diluidor padrão", 10)));
        when(visual.findAllByOrderBySortOrderAscNameAsc()).thenReturn(List.of());
    }

    @Test
    @DisplayName("tabela 0 é 'sem tabela': o cliente vai para a geral (80)")
    void tableZeroIsNoTable() {
        var customers = service.catalog().customers();
        assertThat(customers).extracting(ChecklistCatalogDTO.Customer::priceTable).containsExactly(281, null);
        assertThat(customers.get(0).street()).isEqualTo("Avenida FRANCISCO GLICERIO");
    }

    @Test
    @DisplayName("preços: só das tabelas dos clientes e da 80, e só de produto de venda ou revenda")
    void pricesFiltered() {
        var prices = service.catalog().prices();
        assertThat(prices).extracting(ChecklistCatalogDTO.Price::table).containsExactlyInAnyOrder(80, 281);
        assertThat(prices).extracting(ChecklistCatalogDTO.Price::product).containsOnly(197);
    }

    @Test
    @DisplayName("embalagem: do Sankhya quando existe; do nome quando a unidade bate; nenhuma quando não bate")
    void packages() {
        Map<Integer, ChecklistCatalogDTO.Product> byCode = new java.util.HashMap<>();
        service.catalog().products().forEach(p -> byCode.put(p.code(), p));

        assertThat(byCode.get(455).packageSize()).isEqualByComparingTo("7.5");
        assertThat(byCode.get(455).packageFromName()).isFalse();
        assertThat(byCode.get(455).packageLabel()).isEqualTo("7,5 KG GL");

        assertThat(byCode.get(197).packageSize()).isEqualByComparingTo("20");
        assertThat(byCode.get(197).packageFromName()).isTrue();

        // "5 KG" no nome de um produto vendido por LT: multiplicar daria litro × preço de quilo.
        assertThat(byCode.get(29).packageSize()).isNull();
        assertThat(byCode.get(29).ipi()).isEqualByComparingTo(new BigDecimal("3.25"));
    }

    @Test
    @DisplayName("comodato: nome do Sankhya + nome popular daqui")
    void comodatoJoinsErpName() {
        var items = service.catalog().comodato();
        assertThat(items).singleElement().satisfies(i -> {
            assertThat(i.name()).isEqualTo("DILUIDOR NTI - AZUL");
            assertThat(i.popularName()).isEqualTo("Diluidor padrão");
            assertThat(i.active()).isTrue();
        });
    }

    @Test
    @DisplayName("o Sankhya é lido uma vez enquanto o retrato vale; os cadastros daqui mudam a versão na hora")
    void cachedAndVersioned() {
        String first = service.catalog().version();
        String again = service.catalog().version();
        assertThat(again).isEqualTo(first);
        verify(sankhya, times(1)).customers();

        when(comodato.findAllByOrderBySortOrderAsc()).thenReturn(List.of(
                new ChecklistComodatoItem(1998, "Diluidor padrão", 10),
                new ChecklistComodatoItem(1107, "Frasco 1 litro", 20)));
        assertThat(service.catalog().version()).isNotEqualTo(first);
        verify(sankhya, times(1)).customers();
    }
}
