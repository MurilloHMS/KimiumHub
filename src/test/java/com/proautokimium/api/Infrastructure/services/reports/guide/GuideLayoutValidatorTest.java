package com.proautokimium.api.Infrastructure.services.reports.guide;

import com.proautokimium.api.Infrastructure.exceptions.guide.InvalidGuideLayoutException;
import com.proautokimium.api.domain.valueObjects.guide.GuideLayoutDocument;
import com.proautokimium.api.domain.valueObjects.guide.GuideLayoutDocument.Column;
import com.proautokimium.api.domain.valueObjects.guide.GuideLayoutDocument.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A mensagem do validador é o que o designer lê na tela. Ela precisa dizer
 * ONDE está o erro — "layout inválido" não diz qual das oito colunas mexer.
 */
class GuideLayoutValidatorTest {

    private final GuideLayoutValidator validator = new GuideLayoutValidator();
    private final GuideLayoutDocument seed = GuideLayoutSeed.document();

    @Test
    @DisplayName("a semente da V115 é válida — senão o guia de Contratos não sobe")
    void sementeValida() {
        assertThatCode(() -> validator.validate(seed)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("colunas mais largas que a página: diz o total e quanto tirar")
    void colunasLargasDemais() {
        List<Column> columns = new ArrayList<>(seed.table().columns());
        Column first = columns.get(0);
        columns.set(0, new Column(first.title(), first.width() + 25, first.paddingTop(), first.blocks()));

        assertThatThrownBy(() -> validator.validate(withColumns(columns)))
                .isInstanceOf(InvalidGuideLayoutException.class)
                .hasMessageContaining("705 pt")
                .hasMessageContaining("Tire 25 pt");
    }

    @Test
    @DisplayName("campo desconhecido na célula: diz qual coluna")
    void campoDesconhecido() {
        List<Column> columns = new ArrayList<>(seed.table().columns());
        Column dilution = columns.get(4);
        GuideLayoutDocument.Block block = dilution.blocks().get(0);
        columns.set(4, new Column(dilution.title(), dilution.width(), 0, List.of(new GuideLayoutDocument.Block(
                "PH", block.height(), block.fontSize(), block.bold(), false, "CENTER", "MIDDLE", "#000000", null))));

        assertThatThrownBy(() -> validator.validate(withColumns(columns)))
                .isInstanceOf(InvalidGuideLayoutException.class)
                .hasMessageContaining("\"DILUIÇÃO\"")
                .hasMessageContaining("PH");
    }

    @Test
    @DisplayName("item do rodapé que passa da faixa: diz qual item e de qual faixa")
    void itemForaDaFaixa() {
        List<Element> elements = new ArrayList<>(seed.footer().elements());
        Element first = elements.get(0);
        elements.set(0, new Element(first.type(), first.x(), 30, first.width(), first.height(), null, null, null,
                first.color(), null, null, null, null));
        GuideLayoutDocument layout = new GuideLayoutDocument(seed.page(), seed.header(), seed.table(),
                new GuideLayoutDocument.Band(seed.footer().height(), elements));

        assertThatThrownBy(() -> validator.validate(layout))
                .isInstanceOf(InvalidGuideLayoutException.class)
                .hasMessageContaining("Item 1 do rodapé");
    }

    @Test
    @DisplayName("fonte que o PDF não tem é recusada, em vez de sair trocada calada")
    void fonteQueNaoExiste() {
        GuideLayoutDocument.Page page = seed.page();
        GuideLayoutDocument layout = new GuideLayoutDocument(
                new GuideLayoutDocument.Page(page.format(), page.orientation(), page.margins(), "Comic Sans MS"),
                seed.header(), seed.table(), seed.footer());

        assertThatThrownBy(() -> validator.validate(layout))
                .isInstanceOf(InvalidGuideLayoutException.class)
                .hasMessageContaining("Comic Sans MS");
    }

    private GuideLayoutDocument withColumns(List<Column> columns) {
        GuideLayoutDocument.Table t = seed.table();
        return new GuideLayoutDocument(seed.page(), seed.header(),
                new GuideLayoutDocument.Table(t.headerBackground(), t.headerColor(), t.headerFontSize(),
                        t.headerHeight(), t.minRowHeight(), t.dividerColor(), t.separatorColor(), columns),
                seed.footer());
    }
}
