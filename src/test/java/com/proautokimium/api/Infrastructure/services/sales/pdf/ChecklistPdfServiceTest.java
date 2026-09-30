package com.proautokimium.api.Infrastructure.services.sales.pdf;

import com.proautokimium.api.domain.entities.sales.Checklist;
import com.proautokimium.api.domain.valueObjects.sales.ChecklistContent;
import com.proautokimium.api.domain.valueObjects.sales.ChecklistFixtures;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/** O comprovante: as seções da planilha, os valores, e nada que derrube o PDF. */
class ChecklistPdfServiceTest {

    final ChecklistPdfService service = new ChecklistPdfService(
            Clock.fixed(Instant.parse("2026-09-30T13:00:00Z"), ZoneId.of("America/Sao_Paulo")));

    static Checklist checklist(ChecklistContent content) {
        LocalDateTime now = LocalDateTime.of(2026, 9, 30, 10, 0);
        return Checklist.submit(UUID.randomUUID(), "diego", "Diego Martins", content, true, now, now);
    }

    static String text(byte[] pdf) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    static int pages(byte[] pdf) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            return doc.getNumberOfPages();
        }
    }

    @Test
    @DisplayName("tem as seções e os títulos da planilha, com os valores do checklist")
    void sectionsAndValues() throws IOException {
        String text = text(service.generate(checklist(ChecklistFixtures.valid())));

        assertThat(text).contains("CHECKLIST DE VENDAS", "ENDEREÇOS", "DADOS CADASTRAIS / CONTRATO",
                "MÁQUINAS LAVADORAS", "EQUIPAMENTOS EM COMODATO",
                "COMUNICAÇÃO VISUAL E DILUIÇÃO DE IMPLANTAÇÃO DOS PRODUTOS", "PEDIDO: VENDA [X]");
        assertThat(text).contains("Mercado Central - Unid. 2", "Diego Martins", "Maria Aparecida Souza",
                "11.222.333/0001-81", "529.982.247-25", "13015-904", "Capô",
                "Diluidor padrão", "Lave sempre as mãos (3)", "preenchido sem internet");
        assertThat(text).doesNotContain("TIPO DA MESA", "Entrada e saída");
    }

    @Test
    @DisplayName("o total do pedido é o da planilha: embalagens × tamanho × preço + IPI")
    void orderTotal() throws IOException {
        String text = text(service.generate(checklist(ChecklistFixtures.valid())));
        assertThat(text).contains("R$ 680,21", "R$ 529,65", "R$ 1.209,86", "281 cliente");
    }

    @Test
    @DisplayName("emoji e caractere que a fonte não tem viram '?', em vez de derrubar o PDF")
    void unsupportedCharacters() throws IOException {
        var v = ChecklistFixtures.valid();
        var comodato = new ChecklistContent.Comodato(v.comodato().items(), List.of(), "Cliente pediu urgência 🙏 ✓");
        String text = text(service.generate(checklist(ChecklistFixtures.withComodato(v, comodato))));
        assertThat(text).contains("Cliente pediu urgência ? ?");
    }

    /**
     * Pedido dele (2026-09-30): o comprovante tenta caber em uma página. Um
     * checklist cheio — 3 máquinas, 7 itens de comodato, 8 de comunicação
     * visual, 3 produtos usados e 6 itens no pedido — cabe.
     */
    @Test
    @DisplayName("um checklist cheio cabe em uma página")
    void fullChecklistFitsOnePage() throws IOException {
        var v = ChecklistFixtures.valid();
        var installation = new ChecklistContent.Installation(true, true, List.of(
                new ChecklistContent.Machine("CAPO", null, 2, true),
                new ChecklistContent.Machine("ESTEIRA", null, 1, false),
                new ChecklistContent.Machine("OUTRA", "Lavadora de bandejas", 1, false)),
                "Instalar na segunda, depois das 14h", "2026-10-05");
        var comodato = new ChecklistContent.Comodato(
                IntStream.rangeClosed(1, 6).mapToObj(i -> new ChecklistContent.ComodatoItem(1000 + i,
                        "BOMBA PERISTALTICA PR " + i + " N", "Dosador " + i, i)).toList(),
                List.of(new ChecklistContent.ExtraItem("Mangueira de 10 metros", 2)), "Levar suporte extra");
        var visual = new ChecklistContent.Visual(
                IntStream.rangeClosed(1, 8).mapToObj(i -> new ChecklistContent.VisualItem(null, "Adesivo de boas práticas " + i, i)).toList(),
                List.of(new ChecklistContent.UsedProduct(197, "PROAUTO REMOCON. - 20 LT BB PRETA", 2, 4, "1:50"),
                        new ChecklistContent.UsedProduct(455, "POSEIDON - 7,5 KG GL NATURAL", 0, 3, "1:100"),
                        new ChecklistContent.UsedProduct(29, "KIMI AB200 - 5 LT GL NATURAL", 1, 0, "puro")));
        var items = IntStream.rangeClosed(1, 6).mapToObj(i -> new ChecklistContent.OrderItem(100 + i,
                "PRODUTO DE LIMPEZA NÚMERO " + i + " - 20 LT BB", "LT", new java.math.BigDecimal("20"), "20 LT", i,
                // O primeiro com o preço mudado pelo vendedor: a linha de "preços alterados" também precisa caber.
                new java.math.BigDecimal(i == 1 ? "9.9" : "10.5"), new java.math.BigDecimal("3.25"), 80, "GERAL",
                new java.math.BigDecimal("10.5"), null)).toList();
        var full = new ChecklistContent(v.customer(), v.mainAddress(), false, v.mainAddress(), v.unitContact(),
                installation, comodato, visual, new ChecklistContent.Order(true, "VENDA", items, null));

        byte[] pdf = service.generate(checklist(full));
        if (System.getenv("AMOSTRA_PDF") != null) java.nio.file.Files.write(java.nio.file.Path.of(System.getenv("AMOSTRA_PDF")), pdf);
        assertThat(pages(pdf)).isEqualTo(1);
    }

    /**
     * O vendedor pode mudar o preço de venda (pedido dele, 2026-09-30). O PDF
     * marca o item com "*" e diz o que a tabela dizia; o total usa o de venda.
     */
    @Test
    @DisplayName("preço mudado pelo vendedor: marcado, com o da tabela ao lado, e o total pelo de venda")
    void changedPrice() throws IOException {
        var v = ChecklistFixtures.valid();
        var i = v.order().items().get(0);
        var mudado = new ChecklistContent.OrderItem(i.productCode(), i.name(), i.unit(), i.packageSize(), i.packageLabel(),
                i.packages(), new java.math.BigDecimal("9.50"), i.ipiPercent(), i.priceTable(), i.priceSource(),
                i.tablePrice(), null);
        var content = new ChecklistContent(v.customer(), v.mainAddress(), true, null, v.unitContact(), v.installation(),
                v.comodato(), v.visual(), new ChecklistContent.Order(true, "VENDA", List.of(mudado), null));

        String text = text(service.generate(checklist(content)));

        // 3 × 20 LT × R$ 9,50 × 1,0325 = 588,53
        assertThat(text).contains("R$ 9,50 / LT *", "R$ 588,53", "PREÇOS ALTERADOS PELO VENDEDOR",
                "tabela R$ 10,98, vendido a R$ 9,50 / LT");
        assertThat(text).doesNotContain("DOCUMENTAÇÃO TÉCNICA");
    }

    /** Opcional, mas em destaque (pedido dele, 2026-09-30): logo abaixo do título, antes do cliente. */
    @Test
    @DisplayName("data da implantação em destaque antes do cliente; sem data, 'A definir'")
    void implantationDate() throws IOException {
        var v = ChecklistFixtures.valid();
        String text = text(service.generate(checklist(v)));
        assertThat(text).contains("DATA DA IMPLANTAÇÃO segunda-feira, 05/10/2026");
        assertThat(text.indexOf("DATA DA IMPLANTAÇÃO")).isLessThan(text.indexOf("CLIENTE"));

        var i = v.installation();
        var without = new ChecklistContent(v.customer(), v.mainAddress(), true, null, v.unitContact(),
                new ChecklistContent.Installation(i.withMaintenance(), i.needsMachine(), i.machines(), i.notes(), null),
                v.comodato(), v.visual(), v.order());
        assertThat(text(service.generate(checklist(without)))).contains("DATA DA IMPLANTAÇÃO A definir");
    }

    @Test
    @DisplayName("lista longa quebra página, e o rodapé numera")
    void pageBreak() throws IOException {
        var v = ChecklistFixtures.valid();
        var many = IntStream.rangeClosed(1, 80)
                .mapToObj(i -> new ChecklistContent.ComodatoItem(1000 + i, "EQUIPAMENTO " + i, "Equipamento " + i, 1))
                .toList();
        byte[] pdf = service.generate(checklist(ChecklistFixtures.withComodato(v,
                new ChecklistContent.Comodato(many, List.of(), null))));

        assertThat(pages(pdf)).isGreaterThan(1);
        String text = text(pdf);
        assertThat(text).contains("página 2", "Equipamento 80");
        // O cabeçalho da tabela se repete na página em que ela continua.
        String page2 = text.substring(text.indexOf("página 1"));
        assertThat(page2).contains("CÓDIGO EQUIPAMENTO QTD");
    }
}
