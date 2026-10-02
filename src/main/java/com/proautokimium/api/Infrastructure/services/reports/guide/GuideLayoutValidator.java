package com.proautokimium.api.Infrastructure.services.reports.guide;

import com.proautokimium.api.Infrastructure.exceptions.guide.InvalidGuideLayoutException;
import com.proautokimium.api.domain.enums.guide.GuideField;
import com.proautokimium.api.domain.enums.guide.GuideImageSource;
import com.proautokimium.api.domain.valueObjects.guide.GuideLayoutDocument;
import com.proautokimium.api.domain.valueObjects.guide.GuideLayoutDocument.Band;
import com.proautokimium.api.domain.valueObjects.guide.GuideLayoutDocument.Block;
import com.proautokimium.api.domain.valueObjects.guide.GuideLayoutDocument.Column;
import com.proautokimium.api.domain.valueObjects.guide.GuideLayoutDocument.Element;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Recusa o layout que o Jasper não conseguiria montar, dizendo onde está o erro.
 *
 * Sem isto, um layout errado chegaria ao Jasper e voltaria como um
 * "JRException: band height exceeds..." num 500 — o designer não saberia qual
 * das oito colunas mexer. Aqui a mensagem é a frase que ele lê na tela.
 *
 * Roda ao salvar o rascunho, ao gerar a prévia e ao publicar.
 */
@Component
public class GuideLayoutValidator {

    /** As fontes que o PDF tem. Outra sairia trocada, calada. */
    public static final List<String> FONTS = List.of("DejaVu Sans", "DejaVu Serif", "DejaVu Sans Mono", "Montserrat");

    private static final Set<String> ALIGNS = Set.of("LEFT", "CENTER", "RIGHT");
    private static final Set<String> VERTICAL_ALIGNS = Set.of("TOP", "MIDDLE", "BOTTOM");
    private static final Set<String> ELEMENT_TYPES = Set.of("TEXT", "IMAGE", "RECTANGLE", "LINE");
    private static final Pattern COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private static final int MIN_COLUMN_WIDTH = 10;

    public void validate(GuideLayoutDocument layout) {
        if (layout == null || layout.page() == null || layout.page().margins() == null
                || layout.header() == null || layout.footer() == null || layout.table() == null) {
            throw invalid("O layout está incompleto: faltam página, cabeçalho, tabela ou rodapé");
        }
        GuidePageSize size = validatePage(layout.page());
        validateBand("cabeçalho", layout.header(), size);
        validateBand("rodapé", layout.footer(), size);
        validateTable(layout.table(), size);

        int fixed = layout.header().height() + layout.table().headerHeight()
                + layout.table().minRowHeight() + 1 + layout.footer().height();
        if (fixed > size.contentHeight()) {
            throw invalid("Cabeçalho, título da tabela, uma linha e rodapé somam " + fixed
                    + " pt, e a página tem " + size.contentHeight() + " pt de altura útil");
        }
    }

    private GuidePageSize validatePage(GuideLayoutDocument.Page page) {
        if (!GuidePageSize.LETTER.equals(page.format()) && !GuidePageSize.A4.equals(page.format())) {
            throw invalid("Formato de página desconhecido: " + page.format());
        }
        if (!GuidePageSize.LANDSCAPE.equals(page.orientation()) && !GuidePageSize.PORTRAIT.equals(page.orientation())) {
            throw invalid("Orientação desconhecida: " + page.orientation());
        }
        GuideLayoutDocument.Margins m = page.margins();
        if (m.top() < 0 || m.bottom() < 0 || m.left() < 0 || m.right() < 0) {
            throw invalid("Margem não pode ser negativa");
        }
        if (page.font() != null && !FONTS.contains(page.font())) {
            throw invalid("A fonte " + page.font() + " não existe no PDF. Use uma de: " + String.join(", ", FONTS));
        }
        GuidePageSize size = GuidePageSize.of(page);
        if (size.contentWidth() < 100 || size.contentHeight() < 100) {
            throw invalid("As margens deixam menos de 100 pt de área útil");
        }
        return size;
    }

    private void validateBand(String name, Band band, GuidePageSize size) {
        if (band.height() < 0) throw invalid("A altura do " + name + " não pode ser negativa");
        List<Element> elements = band.elements() == null ? List.of() : band.elements();
        for (int i = 0; i < elements.size(); i++) {
            Element e = elements.get(i);
            String where = "Item " + (i + 1) + " do " + name;
            if (e.type() == null || !ELEMENT_TYPES.contains(e.type())) {
                throw invalid(where + ": tipo desconhecido (" + e.type() + ")");
            }
            if (e.width() <= 0 || e.height() <= 0) throw invalid(where + ": largura e altura precisam ser maiores que zero");
            if (e.x() < 0 || e.y() < 0 || e.x() + e.width() > size.contentWidth() || e.y() + e.height() > band.height()) {
                throw invalid(where + ": está fora da faixa (" + size.contentWidth() + " × " + band.height() + " pt)");
            }
            switch (e.type()) {
                case "TEXT" -> {
                    if (e.text() == null) throw invalid(where + ": texto vazio");
                    validateFontSize(where, e.fontSize() == null ? 10f : e.fontSize());
                    validateAlign(where, e.align(), e.verticalAlign());
                    validateColor(where, e.color());
                }
                case "IMAGE" -> {
                    GuideImageSource source = GuideImageSource.fromKey(e.image())
                            .orElseThrow(() -> invalid(where + ": imagem desconhecida (" + e.image() + ")"));
                    if (source == GuideImageSource.UPLOADED && e.imageId() == null) {
                        throw invalid(where + ": imagem enviada sem o arquivo");
                    }
                    if (e.align() != null && !ALIGNS.contains(e.align())) throw invalid(where + ": alinhamento desconhecido");
                }
                default -> validateColor(where, e.color());
            }
        }
    }

    private void validateTable(GuideLayoutDocument.Table table, GuidePageSize size) {
        validateColor("Título da tabela", table.headerBackground());
        validateColor("Título da tabela", table.headerColor());
        validateColor("Divisória da tabela", table.dividerColor());
        validateColor("Separador das colunas", table.separatorColor());
        validateFontSize("Título da tabela", table.headerFontSize());
        if (table.headerHeight() < 0) throw invalid("A altura do título da tabela não pode ser negativa");
        if (table.minRowHeight() < 10) throw invalid("A linha precisa ter pelo menos 10 pt");

        List<Column> columns = table.columns() == null ? List.of() : table.columns();
        if (columns.isEmpty()) throw invalid("A tabela precisa de pelo menos uma coluna");

        int total = 0;
        for (Column c : columns) {
            String where = "Coluna " + label(c);
            if (c.width() < MIN_COLUMN_WIDTH) throw invalid(where + ": largura mínima é " + MIN_COLUMN_WIDTH + " pt");
            if (c.paddingTop() < 0) throw invalid(where + ": o respiro do topo não pode ser negativo");
            total += c.width();
            int stacked = c.paddingTop();
            List<Block> blocks = c.blocks() == null ? List.of() : c.blocks();
            for (Block b : blocks) {
                GuideField field = GuideField.fromKey(b.field())
                        .orElseThrow(() -> invalid(where + ": campo desconhecido (" + b.field() + ")"));
                String blockWhere = where + ", " + field.getLabel();
                if (b.height() <= 0) throw invalid(blockWhere + ": a altura precisa ser maior que zero");
                if (!field.isImage()) {
                    validateFontSize(blockWhere, b.fontSize());
                    validateAlign(blockWhere, b.align(), b.verticalAlign());
                    validateColor(blockWhere, b.color());
                }
                if (field == GuideField.EQUIPMENT_PHOTOS && b.count() != null && (b.count() < 1 || b.count() > 6)) {
                    throw invalid(blockWhere + ": de 1 a 6 fotos");
                }
                stacked += b.height() + 2;
            }
            if (stacked > size.contentHeight() / 2) {
                throw invalid(where + ": os blocos somam " + stacked + " pt, mais que meia página");
            }
        }
        if (total > size.contentWidth()) {
            throw invalid("As colunas somam " + total + " pt e a página tem " + size.contentWidth()
                    + " pt de largura útil. Tire " + (total - size.contentWidth()) + " pt de alguma coluna");
        }
    }

    private void validateFontSize(String where, float size) {
        if (size < 4 || size > 72) throw invalid(where + ": o tamanho da fonte vai de 4 a 72 pt");
    }

    private void validateAlign(String where, String align, String verticalAlign) {
        if (align != null && !ALIGNS.contains(align)) throw invalid(where + ": alinhamento desconhecido (" + align + ")");
        if (verticalAlign != null && !VERTICAL_ALIGNS.contains(verticalAlign)) {
            throw invalid(where + ": alinhamento vertical desconhecido (" + verticalAlign + ")");
        }
    }

    private void validateColor(String where, String color) {
        if (color != null && !COLOR.matcher(color).matches()) {
            throw invalid(where + ": cor inválida (" + color + "). Use o formato #RRGGBB");
        }
    }

    private static String label(Column c) {
        return c.title() == null || c.title().isBlank() ? "sem título" : "\"" + c.title() + "\"";
    }

    private static InvalidGuideLayoutException invalid(String message) {
        return new InvalidGuideLayoutException(message);
    }
}
