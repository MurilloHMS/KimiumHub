package com.proautokimium.api.Infrastructure.services.reports.guide;

import com.proautokimium.api.domain.enums.guide.GuideField;
import com.proautokimium.api.domain.enums.guide.GuideImageSource;
import com.proautokimium.api.domain.valueObjects.guide.GuideLayoutDocument;
import com.proautokimium.api.domain.valueObjects.guide.GuideLayoutDocument.Band;
import com.proautokimium.api.domain.valueObjects.guide.GuideLayoutDocument.Block;
import com.proautokimium.api.domain.valueObjects.guide.GuideLayoutDocument.Column;
import com.proautokimium.api.domain.valueObjects.guide.GuideLayoutDocument.Element;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignElement;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.design.JRDesignFrame;
import net.sf.jasperreports.engine.design.JRDesignImage;
import net.sf.jasperreports.engine.design.JRDesignLine;
import net.sf.jasperreports.engine.design.JRDesignParameter;
import net.sf.jasperreports.engine.design.JRDesignRectangle;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JRDesignStyle;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.HorizontalImageAlignEnum;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
import net.sf.jasperreports.engine.type.ModeEnum;
import net.sf.jasperreports.engine.type.OrientationEnum;
import net.sf.jasperreports.engine.type.PositionTypeEnum;
import net.sf.jasperreports.engine.type.ScaleImageEnum;
import net.sf.jasperreports.engine.type.SplitTypeEnum;
import net.sf.jasperreports.engine.type.StretchTypeEnum;
import net.sf.jasperreports.engine.type.TextAdjustEnum;
import net.sf.jasperreports.engine.type.VerticalImageAlignEnum;
import net.sf.jasperreports.engine.type.VerticalTextAlignEnum;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Monta o relatório do guia a partir do layout que o designer desenhou.
 *
 * É o que o {@code guia_utilizacao.jrxml} fazia, só que o desenho vem do banco:
 * a página e as margens, o cabeçalho e o rodapé com os itens em posição livre,
 * e a tabela, uma coluna por {@code Column} e, dentro da célula, os blocos
 * empilhados de cima para baixo.
 *
 * <h2>Como a célula cresce</h2>
 * Cada coluna vira um {@code frame} da altura da linha. Os blocos ficam dentro,
 * com posição FLOAT: se a descrição não couber e esticar, o que vem embaixo
 * desce junto. Os frames são CONTAINER_HEIGHT, então a linha toda acompanha a
 * célula mais alta — é isso que mantém os separadores das colunas inteiros. O
 * fio que divide uma linha da outra também é FLOAT e desce com ela.
 *
 * Este é o único lugar que escreve expressão do Jasper. Os nomes dos
 * parâmetros moram aqui como constantes para o serviço preencher os mesmos.
 */
@Component
public class GuideJasperDesignBuilder {

    public static final String PARAM_TITLE = "GUIDE_TITLE";
    private static final String TITLE_PLACEHOLDER = "{titulo}";
    private static final int BLOCK_GAP = 2;
    private static final int CELL_PADDING_X = 2;

    public static String imageParameter(GuideImageSource source) {
        return "IMG_" + source.name();
    }

    public static String uploadedImageParameter(UUID imageId) {
        return "IMG_UPLOADED_" + imageId.toString().replace("-", "");
    }

    /** Os ids das imagens enviadas que o layout usa, para o serviço carregar. */
    public static Set<UUID> uploadedImageIds(GuideLayoutDocument layout) {
        Set<UUID> ids = new LinkedHashSet<>();
        for (Band band : List.of(layout.header(), layout.footer())) {
            for (Element e : elements(band)) {
                if ("IMAGE".equals(e.type()) && GuideImageSource.UPLOADED.name().equals(e.image()) && e.imageId() != null) {
                    ids.add(e.imageId());
                }
            }
        }
        return ids;
    }

    public JasperDesign build(GuideLayoutDocument layout) throws JRException {
        GuidePageSize size = GuidePageSize.of(layout.page());
        GuideLayoutDocument.Margins m = layout.page().margins();

        JasperDesign design = new JasperDesign();
        design.setName("guia_utilizacao_editavel");
        design.setLanguage("java");
        design.setPageWidth(size.width());
        design.setPageHeight(size.height());
        design.setOrientation(size.width() > size.height() ? OrientationEnum.LANDSCAPE : OrientationEnum.PORTRAIT);
        design.setColumnWidth(size.contentWidth());
        design.setTopMargin(m.top());
        design.setBottomMargin(m.bottom());
        design.setLeftMargin(m.left());
        design.setRightMargin(m.right());

        JRDesignStyle base = new JRDesignStyle();
        base.setName("base");
        base.setDefault(true);
        base.setFontName(layout.page().font() != null ? layout.page().font() : "DejaVu Sans");
        design.addStyle(base);

        declareParameters(design, layout);
        declareFields(design);

        design.setPageHeader(band(design, layout.header()));
        design.setColumnHeader(columnHeader(design, layout.table()));
        ((JRDesignSection) design.getDetailSection()).addBand(detail(design, layout.table()));
        design.setPageFooter(band(design, layout.footer()));
        return design;
    }

    // ── Declarações ─────────────────────────────────────────────────────────

    private void declareParameters(JasperDesign design, GuideLayoutDocument layout) throws JRException {
        addParameter(design, PARAM_TITLE, String.class);
        for (GuideImageSource source : GuideImageSource.values()) {
            if (source != GuideImageSource.UPLOADED) addParameter(design, imageParameter(source), Object.class);
        }
        for (UUID id : uploadedImageIds(layout)) {
            addParameter(design, uploadedImageParameter(id), Object.class);
        }
    }

    private static void addParameter(JasperDesign design, String name, Class<?> type) throws JRException {
        JRDesignParameter p = new JRDesignParameter();
        p.setName(name);
        p.setValueClass(type);
        design.addParameter(p);
    }

    private void declareFields(JasperDesign design) throws JRException {
        for (GuideField field : GuideField.values()) {
            JRDesignField f = new JRDesignField();
            f.setName(field.getRowField());
            f.setValueClass(switch (field.getKind()) {
                case TEXT -> String.class;
                case IMAGE -> Object.class;
                case IMAGE_LIST -> List.class;
            });
            design.addField(f);
        }
    }

    // ── Cabeçalho e rodapé ──────────────────────────────────────────────────

    private JRDesignBand band(JasperDesign design, Band source) {
        JRDesignBand band = new JRDesignBand();
        band.setHeight(source.height());
        band.setSplitType(SplitTypeEnum.STRETCH);
        for (Element e : elements(source)) {
            band.addElement(switch (e.type()) {
                case "TEXT" -> text(design, e);
                case "IMAGE" -> image(design, e);
                case "LINE" -> line(design, e);
                default -> rectangle(design, e);
            });
        }
        return band;
    }

    private JRDesignElement text(JasperDesign design, Element e) {
        String text = e.text() == null ? "" : e.text();
        boolean dynamic = text.contains(TITLE_PLACEHOLDER);
        if (dynamic) {
            JRDesignTextField field = new JRDesignTextField(design);
            field.setExpression(expression(javaString(text) + ".replace(" + javaString(TITLE_PLACEHOLDER) + ", $P{"
                    + PARAM_TITLE + "} != null ? $P{" + PARAM_TITLE + "} : \"GERAL\")"));
            styleText(field, e.fontSize(), Boolean.TRUE.equals(e.bold()), e.color(), e.align(), e.verticalAlign());
            place(field, e.x(), e.y(), e.width(), e.height());
            return field;
        }
        JRDesignStaticText label = new JRDesignStaticText(design);
        label.setText(text);
        styleText(label, e.fontSize(), Boolean.TRUE.equals(e.bold()), e.color(), e.align(), e.verticalAlign());
        place(label, e.x(), e.y(), e.width(), e.height());
        return label;
    }

    private JRDesignElement image(JasperDesign design, Element e) {
        GuideImageSource source = GuideImageSource.fromKey(e.image()).orElseThrow();
        String param = source == GuideImageSource.UPLOADED ? uploadedImageParameter(e.imageId()) : imageParameter(source);
        JRDesignImage image = new JRDesignImage(design);
        place(image, e.x(), e.y(), e.width(), e.height());
        image.setScaleImage(ScaleImageEnum.RETAIN_SHAPE);
        image.setHorizontalImageAlign(imageAlign(e.align()));
        image.setVerticalImageAlign(VerticalImageAlignEnum.MIDDLE);
        // O cabeçalho se repete em toda página: o cache evita decodificar a
        // mesma imagem de novo a cada folha.
        image.setUsingCache(true);
        image.setPrintWhenExpression(expression("$P{" + param + "} != null"));
        image.setExpression(expression("$P{" + param + "}"));
        return image;
    }

    private JRDesignElement rectangle(JasperDesign design, Element e) {
        JRDesignRectangle rect = new JRDesignRectangle(design);
        place(rect, e.x(), e.y(), e.width(), e.height());
        Color color = color(e.color(), Color.LIGHT_GRAY);
        rect.setForecolor(color);
        rect.setBackcolor(color);
        rect.setMode(ModeEnum.OPAQUE);
        rect.getLinePen().setLineWidth(0f);
        return rect;
    }

    /** O traço segue o lado mais comprido: 1 × 46 é vertical, 120 × 1 é horizontal. */
    private JRDesignElement line(JasperDesign design, Element e) {
        JRDesignLine line = new JRDesignLine(design);
        place(line, e.x(), e.y(), e.width(), e.height());
        line.setForecolor(color(e.color(), Color.LIGHT_GRAY));
        line.getLinePen().setLineWidth(0.8f);
        return line;
    }

    // ── Tabela ──────────────────────────────────────────────────────────────

    private JRDesignBand columnHeader(JasperDesign design, GuideLayoutDocument.Table table) {
        JRDesignBand band = new JRDesignBand();
        band.setHeight(table.headerHeight());
        if (table.headerHeight() == 0) return band;

        int total = table.columns().stream().mapToInt(Column::width).sum();
        Color background = color(table.headerBackground(), new Color(0x232E61));
        JRDesignRectangle bar = new JRDesignRectangle(design);
        place(bar, 0, 0, total, table.headerHeight());
        bar.setForecolor(background);
        bar.setBackcolor(background);
        bar.setMode(ModeEnum.OPAQUE);
        bar.getLinePen().setLineWidth(0f);
        band.addElement(bar);

        int x = 0;
        for (int i = 0; i < table.columns().size(); i++) {
            Column c = table.columns().get(i);
            JRDesignStaticText title = new JRDesignStaticText(design);
            title.setText(c.title() == null ? "" : c.title());
            styleText(title, table.headerFontSize(), true, table.headerColor(), "CENTER", "MIDDLE");
            place(title, x, 0, c.width(), table.headerHeight());
            band.addElement(title);
            if (i > 0) {
                JRDesignLine separator = new JRDesignLine(design);
                int inset = Math.min(3, table.headerHeight() / 4);
                place(separator, x, inset, 1, Math.max(1, table.headerHeight() - 2 * inset));
                separator.setForecolor(color(table.headerColor(), Color.WHITE));
                separator.getLinePen().setLineWidth(0.5f);
                band.addElement(separator);
            }
            x += c.width();
        }
        return band;
    }

    private JRDesignBand detail(JasperDesign design, GuideLayoutDocument.Table table) {
        int rowHeight = Math.max(table.minRowHeight(),
                table.columns().stream().mapToInt(GuideJasperDesignBuilder::stackHeight).max().orElse(0));

        JRDesignBand band = new JRDesignBand();
        band.setHeight(rowHeight + 1);
        // Uma linha nunca se parte entre duas páginas: o produto sairia com o
        // nome numa folha e a diluição na outra.
        band.setSplitType(SplitTypeEnum.PREVENT);

        int x = 0;
        for (int i = 0; i < table.columns().size(); i++) {
            Column c = table.columns().get(i);
            JRDesignFrame cell = new JRDesignFrame(design);
            place(cell, x, 0, c.width(), rowHeight);
            cell.setStretchType(StretchTypeEnum.CONTAINER_HEIGHT);
            if (i > 0) {
                cell.getLineBox().getLeftPen().setLineWidth(0.5f);
                cell.getLineBox().getLeftPen().setLineColor(color(table.separatorColor(), new Color(0xDDDDDD)));
            }
            int y = c.paddingTop();
            for (Block b : blocks(c)) {
                GuideField field = GuideField.fromKey(b.field()).orElseThrow();
                int width = c.width() - 2 * CELL_PADDING_X;
                if (field.getKind() == GuideField.Kind.IMAGE_LIST) {
                    addImageList(design, cell, field, b, CELL_PADDING_X, y, width);
                } else if (field.isImage()) {
                    cell.addElement(cellImage(design, "$F{" + field.getRowField() + "}", CELL_PADDING_X, y, width, b.height()));
                } else {
                    cell.addElement(cellText(design, field, b, CELL_PADDING_X, y, width));
                }
                y += b.height() + BLOCK_GAP;
            }
            band.addElement(cell);
            x += c.width();
        }

        JRDesignLine divider = new JRDesignLine(design);
        place(divider, 0, rowHeight, x, 1);
        divider.setPositionType(PositionTypeEnum.FLOAT);
        divider.setForecolor(color(table.dividerColor(), new Color(0xE0E4EA)));
        divider.getLinePen().setLineWidth(0.5f);
        band.addElement(divider);
        return band;
    }

    private JRDesignTextField cellText(JasperDesign design, GuideField field, Block b, int x, int y, int width) {
        String f = "$F{" + field.getRowField() + "}";
        String value = b.uppercase() ? f + ".toUpperCase()" : f;
        JRDesignTextField text = new JRDesignTextField(design);
        text.setExpression(expression("(" + f + " != null && !" + f + ".isBlank()) ? " + value + " : "
                + javaString(field.getFallback())));
        styleText(text, b.fontSize(), b.bold(), b.color(), b.align(), b.verticalAlign());
        place(text, x, y, width, b.height());
        text.setPositionType(PositionTypeEnum.FLOAT);
        text.setTextAdjust(TextAdjustEnum.STRETCH_HEIGHT);
        // Sem valor e sem marcador ("—"), o bloco some e o de baixo sobe.
        text.setRemoveLineWhenBlank(true);
        text.setBlankWhenNull(true);
        return text;
    }

    private void addImageList(JasperDesign design, JRDesignFrame cell, GuideField field, Block b, int x, int y, int width) {
        int count = b.count() == null ? 1 : b.count();
        int each = width / count;
        String f = "$F{" + field.getRowField() + "}";
        for (int i = 0; i < count; i++) {
            JRDesignImage image = cellImage(design, f + ".get(" + i + ")", x + i * each, y, each, b.height());
            image.setPrintWhenExpression(expression(f + " != null && " + f + ".size() > " + i));
            cell.addElement(image);
        }
    }

    private JRDesignImage cellImage(JasperDesign design, String valueExpression, int x, int y, int width, int height) {
        JRDesignImage image = new JRDesignImage(design);
        place(image, x, y, width, height);
        image.setPositionType(PositionTypeEnum.FLOAT);
        image.setScaleImage(ScaleImageEnum.RETAIN_SHAPE);
        image.setHorizontalImageAlign(HorizontalImageAlignEnum.CENTER);
        image.setVerticalImageAlign(VerticalImageAlignEnum.MIDDLE);
        image.setPrintWhenExpression(expression(valueExpression + " != null"));
        image.setExpression(expression(valueExpression));
        return image;
    }

    /** A altura que a coluna ocupa antes de qualquer texto esticar. */
    static int stackHeight(Column c) {
        int total = c.paddingTop();
        List<Block> blocks = blocks(c);
        for (Block b : blocks) total += b.height();
        return total + BLOCK_GAP * Math.max(0, blocks.size() - 1);
    }

    // ── Pequenas peças ──────────────────────────────────────────────────────

    private static void styleText(net.sf.jasperreports.engine.design.JRDesignTextElement text, Float fontSize,
                                  boolean bold, String color, String align, String verticalAlign) {
        text.setFontSize(fontSize == null ? 10f : fontSize);
        text.setBold(bold);
        text.setForecolor(color(color, Color.BLACK));
        text.setHorizontalTextAlign(switch (align == null ? "LEFT" : align) {
            case "CENTER" -> HorizontalTextAlignEnum.CENTER;
            case "RIGHT" -> HorizontalTextAlignEnum.RIGHT;
            default -> HorizontalTextAlignEnum.LEFT;
        });
        text.setVerticalTextAlign(switch (verticalAlign == null ? "MIDDLE" : verticalAlign) {
            case "TOP" -> VerticalTextAlignEnum.TOP;
            case "BOTTOM" -> VerticalTextAlignEnum.BOTTOM;
            default -> VerticalTextAlignEnum.MIDDLE;
        });
    }

    private static HorizontalImageAlignEnum imageAlign(String align) {
        if ("LEFT".equals(align)) return HorizontalImageAlignEnum.LEFT;
        if ("RIGHT".equals(align)) return HorizontalImageAlignEnum.RIGHT;
        return HorizontalImageAlignEnum.CENTER;
    }

    private static void place(JRDesignElement element, int x, int y, int width, int height) {
        element.setX(x);
        element.setY(y);
        element.setWidth(width);
        element.setHeight(height);
    }

    private static Color color(String hex, Color fallback) {
        if (hex == null || hex.length() != 7) return fallback;
        return new Color(Integer.parseInt(hex.substring(1), 16));
    }

    private static JRDesignExpression expression(String text) {
        return new JRDesignExpression(text);
    }

    /**
     * Texto do designer como literal Java. Ele vai parar dentro de uma
     * expressão compilada: uma aspa sem escape quebraria a compilação, e uma
     * barra invertida mudaria o texto.
     */
    static String javaString(String text) {
        StringBuilder out = new StringBuilder("\"");
        for (char ch : text.toCharArray()) {
            switch (ch) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (ch < 0x20 || ch > 0x7E) out.append(String.format("\\u%04x", (int) ch));
                    else out.append(ch);
                }
            }
        }
        return out.append('"').toString();
    }

    private static List<Element> elements(Band band) {
        return band == null || band.elements() == null ? List.of() : band.elements();
    }

    private static List<Block> blocks(Column c) {
        return c.blocks() == null ? List.of() : c.blocks();
    }
}
