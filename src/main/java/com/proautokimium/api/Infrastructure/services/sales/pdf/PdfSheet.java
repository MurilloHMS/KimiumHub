package com.proautokimium.api.Infrastructure.services.sales.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Um formulário em PDF, desenhado de cima para baixo: título de seção, linha
 * de campos (rótulo em cima, valor embaixo) e tabela. Quebra página sozinho.
 *
 * <p>Existe para o comprovante do checklist ter o desenho da planilha sem um
 * {@code .jrxml}: o checklist tem cinco listas de tamanho variável, e cada uma
 * seria um subrelatório no Jasper, recompilado à mão a cada ajuste.
 *
 * <p>Fonte Helvetica padrão (WinAnsi): acento, "º" e "—" saem; o que a fonte
 * não tem (emoji digitado no celular) vira "?" em vez de derrubar o PDF.
 */
final class PdfSheet implements AutoCloseable {

    static final Color INK = new Color(0x1a, 0x20, 0x2c);
    static final Color MUTED = new Color(0x5b, 0x66, 0x78);
    static final Color BRAND = new Color(0x23, 0x2e, 0x61);
    static final Color LINE = new Color(0xcb, 0xd2, 0xdc);
    static final Color FILL = new Color(0xf1, 0xf3, 0xf7);

    // Medidas para o checklist caber em uma página (pedido dele, 2026-09-30).
    private static final float MARGIN = 26;
    private static final float LABEL_SIZE = 6.2f;
    private static final float VALUE_SIZE = 8.2f;
    private static final float CELL_PAD = 3.5f;

    private final PDDocument document = new PDDocument();
    private final PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private final String footer;

    private PDPageContentStream out;
    private float y;
    private int pages;

    PdfSheet(String footer) {
        this.footer = footer;
        newPage();
    }

    float width() {
        return PDRectangle.A4.getWidth() - 2 * MARGIN;
    }

    // ── Blocos ───────────────────────────────────────────────────────────────

    /** Cabeçalho da primeira página: título grande e uma linha de apoio. */
    void header(String title, String subtitle, String note) {
        text(bold, 13, BRAND, MARGIN, y - 13, title);
        y -= 17;
        text(regular, 8, MUTED, MARGIN, y - 8, subtitle);
        y -= 12;
        for (String line : wrap(note, regular, 6.8f, width())) {
            text(regular, 6.8f, MUTED, MARGIN, y - 6.8f, line);
            y -= 8.5f;
        }
        y -= 2;
    }

    /** Faixa com o nome da seção, como os títulos da planilha. */
    void section(String title) {
        ensure(34);
        y -= 4;
        fill(BRAND, MARGIN, y - 12, width(), 12);
        text(bold, 7.5f, Color.WHITE, MARGIN + 5, y - 8.8f, title.toUpperCase());
        y -= 12;
    }

    /** Uma linha de campos: rótulo pequeno em cima, valor embaixo, com borda. */
    void fields(Field... fields) {
        float total = 0;
        for (Field f : fields) total += f.weight();

        float[] widths = new float[fields.length];
        float height = 0;
        List<List<String>> lines = new ArrayList<>();
        for (int i = 0; i < fields.length; i++) {
            widths[i] = width() * fields[i].weight() / total;
            List<String> wrapped = wrap(show(fields[i].value()), regular, VALUE_SIZE, widths[i] - 2 * CELL_PAD);
            lines.add(wrapped);
            height = Math.max(height, 5 + LABEL_SIZE + wrapped.size() * (VALUE_SIZE + 1.8f) + 3);
        }

        ensure(height);
        float x = MARGIN;
        for (int i = 0; i < fields.length; i++) {
            box(x, y - height, widths[i], height, null);
            text(regular, LABEL_SIZE, MUTED, x + CELL_PAD, y - 2.5f - LABEL_SIZE, fields[i].label().toUpperCase());
            float ty = y - 4.5f - LABEL_SIZE - VALUE_SIZE;
            for (String line : lines.get(i)) {
                text(regular, VALUE_SIZE, INK, x + CELL_PAD, ty, line);
                ty -= VALUE_SIZE + 1.8f;
            }
            x += widths[i];
        }
        y -= height;
    }

    /** Tabela com cabeçalho cinza. Linha vazia vira "Nenhum item". */
    void table(String[] headers, float[] weights, List<String[]> rows) {
        table(headers, weights, rows, 0);
    }

    /**
     * {@code keepWithLast}: espaço que a última linha reserva para o que vem
     * logo depois (o TOTAL do pedido). Sem isso o total podia cair sozinho na
     * página seguinte — medido na primeira amostra.
     */
    void table(String[] headers, float[] weights, List<String[]> rows, float keepWithLast) {
        float total = 0;
        for (float w : weights) total += w;
        float[] widths = new float[weights.length];
        for (int i = 0; i < weights.length; i++) widths[i] = width() * weights[i] / total;

        tableRow(headers, widths, true);
        if (rows.isEmpty()) {
            tableRow(new String[]{"Nenhum item"}, new float[]{width()}, false);
            return;
        }
        for (int i = 0; i < rows.size(); i++) {
            // Quebrou a página no meio da tabela: o cabeçalho se repete, senão
            // quem lê a página 2 não sabe que coluna é o quê.
            if (tableRow(rows.get(i), widths, false, i == rows.size() - 1 ? keepWithLast : 0)) {
                tableRow(headers, widths, true);
                tableRow(rows.get(i), widths, false, 0);
            }
        }
    }

    private void tableRow(String[] cells, float[] widths, boolean head) {
        tableRow(cells, widths, head, 0);
    }

    /**
     * Desenha uma linha. Se ela não cabe, abre a página nova, NÃO desenha, e
     * devolve {@code true}: quem chamou põe o cabeçalho e manda a linha de novo.
     */
    private boolean tableRow(String[] cells, float[] widths, boolean head, float reserve) {
        PDType1Font font = head ? bold : regular;
        float size = head ? 6.3f : 7.8f;
        List<List<String>> lines = new ArrayList<>();
        float height = 0;
        for (int i = 0; i < cells.length; i++) {
            List<String> wrapped = wrap(head ? cells[i].toUpperCase() : show(cells[i]), font, size,
                    widths[i] - 2 * CELL_PAD);
            lines.add(wrapped);
            height = Math.max(height, 4.5f + wrapped.size() * (size + 1.8f));
        }
        boolean broke = !head && ensure(height + reserve);
        if (broke) {
            return true;
        }
        float x = MARGIN;
        for (int i = 0; i < cells.length; i++) {
            box(x, y - height, widths[i], height, head ? FILL : null);
            float ty = y - 2.5f - size;
            for (String line : lines.get(i)) {
                text(font, size, head ? MUTED : INK, x + CELL_PAD, ty, line);
                ty -= size + 1.8f;
            }
            x += widths[i];
        }
        y -= height;
        return false;
    }

    /** Linha de destaque à direita, como o TOTAL da planilha. */
    void total(String label, String value) {
        ensure(16);
        float w = width() * 0.4f;
        fill(FILL, MARGIN + width() - w, y - 15, w, 15);
        box(MARGIN + width() - w, y - 15, w, 15, null);
        text(bold, 7.5f, MUTED, MARGIN + width() - w + 5, y - 10, label.toUpperCase());
        float valueWidth = stringWidth(bold, 9.5f, value);
        text(bold, 9.5f, BRAND, MARGIN + width() - 5 - valueWidth, y - 10.5f, value);
        y -= 15;
    }

    void gap(float points) {
        y -= points;
    }

    byte[] finish() {
        try {
            drawFooter();
            out.close();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            document.save(bytes);
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        try {
            document.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    record Field(String label, String value, float weight) {
        static Field of(String label, String value) {
            return new Field(label, value, 1);
        }

        static Field of(String label, String value, float weight) {
            return new Field(label, value, weight);
        }
    }

    // ── Desenho ──────────────────────────────────────────────────────────────

    /** Abre página nova se não couber. Devolve se abriu. */
    private boolean ensure(float height) {
        if (y - height < MARGIN + 18) {
            drawFooter();
            try {
                out.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            newPage();
            return true;
        }
        return false;
    }

    private void newPage() {
        try {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            out = new PDPageContentStream(document, page);
            pages++;
            y = PDRectangle.A4.getHeight() - MARGIN;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void drawFooter() {
        String text = footer + " · página " + pages;
        text(regular, 7, MUTED, MARGIN, MARGIN - 4, text);
    }

    private void box(float x, float bottom, float w, float h, Color fillColor) {
        try {
            if (fillColor != null) {
                fill(fillColor, x, bottom, w, h);
            }
            out.setStrokingColor(LINE);
            out.setLineWidth(0.6f);
            out.addRect(x, bottom, w, h);
            out.stroke();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void fill(Color color, float x, float bottom, float w, float h) {
        try {
            out.setNonStrokingColor(color);
            out.addRect(x, bottom, w, h);
            out.fill();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void text(PDType1Font font, float size, Color color, float x, float baseline, String value) {
        try {
            out.beginText();
            out.setFont(font, size);
            out.setNonStrokingColor(color);
            out.newLineAtOffset(x, baseline);
            out.showText(encodable(font, value));
            out.endText();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ── Texto ────────────────────────────────────────────────────────────────

    private static String show(String value) {
        return value == null || value.isBlank() ? "—" : value.strip();
    }

    /** Quebra por palavra; palavra maior que a coluna é cortada em pedaços. */
    List<String> wrap(String value, PDType1Font font, float size, float maxWidth) {
        List<String> lines = new ArrayList<>();
        // Separa as linhas ANTES de trocar o que a fonte não desenha: senão o
        // próprio "\n" vira "?" e a célula perde a quebra.
        for (String raw : (value == null ? "" : value).split("\n")) {
            String paragraph = encodable(font, raw);
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (stringWidth(font, size, candidate) <= maxWidth) {
                    line.setLength(0);
                    line.append(candidate);
                    continue;
                }
                if (!line.isEmpty()) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                String rest = word;
                while (stringWidth(font, size, rest) > maxWidth && rest.length() > 1) {
                    int cut = rest.length() - 1;
                    while (cut > 1 && stringWidth(font, size, rest.substring(0, cut)) > maxWidth) cut--;
                    lines.add(rest.substring(0, cut));
                    rest = rest.substring(cut);
                }
                line.append(rest);
            }
            lines.add(line.toString());
        }
        return lines;
    }

    private float stringWidth(PDType1Font font, float size, String value) {
        try {
            return font.getStringWidth(encodable(font, value)) / 1000 * size;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** O que a fonte não sabe desenhar vira "?", em vez de derrubar o PDF inteiro. */
    private static String encodable(PDType1Font font, String value) {
        StringBuilder out = new StringBuilder(value.length());
        value.codePoints().forEach(cp -> {
            String ch = new String(Character.toChars(cp));
            try {
                font.encode(ch);
                out.append(ch);
            } catch (IOException | IllegalArgumentException e) {
                out.append(cp == '\t' ? " " : "?");
            }
        });
        return out.toString();
    }
}
