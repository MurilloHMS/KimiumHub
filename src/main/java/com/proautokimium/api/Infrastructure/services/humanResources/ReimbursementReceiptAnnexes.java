package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Infrastructure.services.storage.ReimbursementStorageService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.LayerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import java.awt.geom.Rectangle2D;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Anexa os comprovantes ao PDF do relatório — só no comprovante de UM
 * funcionário, como decidido: com todos, o arquivo ficaria enorme.
 *
 * Cada comprovante vira:
 * <ul>
 *   <li><b>imagem</b>: uma página A4 com a identificação no topo e a imagem ajustada;</li>
 *   <li><b>PDF</b>: cada página vira uma página de anexo, desenhada dentro dela e reduzida para caber;</li>
 *   <li><b>arquivo sumido ou formato ilegível</b>: a folha de identificação dizendo
 *   isso. Registro órfão (linha sem arquivo) já aconteceu na galeria — um anexo
 *   quebrado não pode derrubar o documento inteiro.</li>
 * </ul>
 *
 * No fim, carimba "Página X de Y" em TODAS as páginas: a numeração do Jasper
 * só conhece as páginas do relatório.
 */
@Component
public class ReimbursementReceiptAnnexes {

    private static final Logger log = LoggerFactory.getLogger(ReimbursementReceiptAnnexes.class);

    /** A fonte do relatório, do jar de fontes do Jasper; se sumir, cai na Helvetica. */
    private static final String MONTSERRAT = "/fonts/family1778097886545/Montserrat/Montserrat-";

    private static final Color NAVY = new Color(0x23, 0x2E, 0x61);
    private static final Color INK = new Color(0x1B, 0x1F, 0x33);
    private static final Color MUTED = new Color(0x5D, 0x62, 0x78);
    private static final Color RULE = new Color(0xD9, 0xDC, 0xE6);

    private static final float MARGIN = 40f;

    private final ReimbursementStorageService storage;

    public ReimbursementReceiptAnnexes(ReimbursementStorageService storage) {
        this.storage = storage;
    }

    /** O que a folha de identificação mostra de cada comprovante. */
    public record ReceiptAnnex(String code, String employeeName, String employeeInfo, String expenseDate,
                               String category, String statusLabel, String amountLabel,
                               String originalFilename, String storagePath) {}

    public byte[] append(byte[] reportPdf, List<ReceiptAnnex> annexes, String footerLabel) throws IOException {
        List<PDDocument> opened = new ArrayList<>();
        try (PDDocument doc = Loader.loadPDF(reportPdf)) {
            Fonts fonts = loadFonts(doc);
            // Páginas do relatório já têm o rodapé do Jasper; as que vierem
            // depois desta, não — o carimbo põe o rodapé só nelas.
            int reportPages = doc.getNumberOfPages();

            for (ReceiptAnnex annex : annexes) {
                byte[] bytes = read(annex.storagePath());
                if (bytes == null) {
                    notePage(doc, fonts, annex, "O arquivo deste comprovante não foi encontrado no servidor.");
                } else if (isPdf(annex, bytes)) {
                    PDDocument source = Loader.loadPDF(bytes);
                    opened.add(source);
                    pdfPages(doc, fonts, annex, source);
                } else {
                    imagePage(doc, fonts, annex, bytes);
                }
            }

            stampPageNumbers(doc, fonts, footerLabel, reportPages);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } finally {
            for (PDDocument source : opened) {
                source.close();
            }
        }
    }

    // ── leitura ──────────────────────────────────────────────────────────────

    private byte[] read(String storagePath) {
        try {
            Path path = storage.resolve(storagePath);
            return Files.exists(path) ? Files.readAllBytes(path) : null;
        } catch (IOException | RuntimeException e) {
            log.warn("Comprovante ilegível no disco: {}", storagePath, e);
            return null;
        }
    }

    private static boolean isPdf(ReceiptAnnex annex, byte[] bytes) {
        String name = annex.originalFilename() == null ? "" : annex.originalFilename().toLowerCase(Locale.ROOT);
        boolean magic = bytes.length > 4 && bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F';
        return magic || name.endsWith(".pdf");
    }

    // ── páginas ──────────────────────────────────────────────────────────────

    /**
     * Cada página do PDF é desenhada DENTRO da página do anexo, abaixo do
     * cabeçalho, reduzida para caber — nunca ampliada.
     *
     * Antes o PDF entrava com uma folha de identificação e depois as páginas
     * originais no tamanho original: uma foto "digitalizada" pelo celular (uma
     * página do tamanho da foto) virava três páginas no relatório. Agora uma
     * página de PDF é uma página de anexo, como uma foto.
     *
     * {@code importPageAsForm} já aplica a rotação da página (/Rotate); por isso
     * o tamanho a caber é o da caixa DEPOIS da matriz do formulário.
     */
    private void pdfPages(PDDocument doc, Fonts fonts, ReceiptAnnex annex, PDDocument source) throws IOException {
        LayerUtility layers = new LayerUtility(doc);
        int total = source.getNumberOfPages();
        for (int i = 0; i < total; i++) {
            PDFormXObject form = layers.importPageAsForm(source, i);
            Rectangle2D bounds = form.getMatrix().createAffineTransform()
                    .createTransformedShape(form.getBBox().toGeneralPath()).getBounds2D();

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                String suffix = total > 1 ? " · página " + (i + 1) + " de " + total : "";
                float top = header(cs, fonts, page, annex, suffix);
                float boxW = page.getMediaBox().getWidth() - 2 * MARGIN;
                float boxH = top - MARGIN - 40;
                float scale = (float) Math.min(1.0, Math.min(boxW / bounds.getWidth(), boxH / bounds.getHeight()));
                float w = (float) bounds.getWidth() * scale;
                float h = (float) bounds.getHeight() * scale;
                float x = MARGIN + (boxW - w) / 2;
                float y = top - 16 - h;

                cs.saveGraphicsState();
                cs.transform(Matrix.getTranslateInstance(x - (float) bounds.getMinX() * scale,
                        y - (float) bounds.getMinY() * scale));
                cs.transform(Matrix.getScaleInstance(scale, scale));
                cs.drawForm(form);
                cs.restoreGraphicsState();
            }
        }
    }

    private void imagePage(PDDocument doc, Fonts fonts, ReceiptAnnex annex, byte[] bytes) throws IOException {
        PDImageXObject image;
        try {
            image = PDImageXObject.createFromByteArray(doc, bytes, annex.originalFilename());
        } catch (IOException | IllegalArgumentException e) {
            // HEIC, WEBP e afins: o celular manda, o PDFBox não lê.
            notePage(doc, fonts, annex, "O formato deste comprovante não pode ser incluído no PDF ("
                    + annex.originalFilename() + "). Consulte-o no KimiumHub.");
            return;
        }
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            float top = header(cs, fonts, page, annex);
            float boxW = page.getMediaBox().getWidth() - 2 * MARGIN;
            float boxH = top - MARGIN - 40;
            float scale = Math.min(boxW / image.getWidth(), boxH / image.getHeight());
            scale = Math.min(scale, 1f); // não amplia imagem pequena: fica borrada
            float w = image.getWidth() * scale;
            float h = image.getHeight() * scale;
            float x = MARGIN + (boxW - w) / 2;
            float y = top - 16 - h;
            cs.drawImage(image, x, y, w, h);
        }
    }

    private void notePage(PDDocument doc, Fonts fonts, ReceiptAnnex annex, String note) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            float top = header(cs, fonts, page, annex);
            write(cs, fonts.regular, 9, INK, MARGIN, top - 24, note);
        }
    }

    /** A identificação do anexo; devolve a altura onde o conteúdo pode começar. */
    private float header(PDPageContentStream cs, Fonts fonts, PDPage page, ReceiptAnnex annex) throws IOException {
        return header(cs, fonts, page, annex, "");
    }

    private float header(PDPageContentStream cs, Fonts fonts, PDPage page, ReceiptAnnex annex, String suffix)
            throws IOException {
        float width = page.getMediaBox().getWidth();
        float y = page.getMediaBox().getHeight() - MARGIN;

        write(cs, fonts.bold, 13, NAVY, MARGIN, y - 12, "Anexo " + annex.code() + " · Comprovante da despesa" + suffix);
        String who = annex.employeeName() + " · " + annex.employeeInfo();
        write(cs, fonts.regular, 8, MUTED, width - MARGIN - fonts.regular.getStringWidth(who) / 1000 * 8, y - 12, who);
        rule(cs, MARGIN, y - 20, width - 2 * MARGIN, NAVY, 2f);

        String[][] cells = {{"DESPESA", annex.expenseDate()}, {"CATEGORIA", annex.category()},
                {"STATUS", annex.statusLabel()}, {"VALOR SOLICITADO", annex.amountLabel()}};
        float cellW = (width - 2 * MARGIN) / cells.length;
        for (int i = 0; i < cells.length; i++) {
            float x = MARGIN + i * cellW + 7;
            write(cs, fonts.bold, 6.5f, MUTED, x, y - 38, cells[i][0]);
            write(cs, fonts.bold, 9, INK, x, y - 51, cells[i][1]);
        }
        rule(cs, MARGIN, y - 60, width - 2 * MARGIN, RULE, 0.6f);
        write(cs, fonts.regular, 7.5f, MUTED, MARGIN, y - 72, "Arquivo original: " + annex.originalFilename());
        return y - 80;
    }

    private void stampPageNumbers(PDDocument doc, Fonts fonts, String footerLabel, int reportPages)
            throws IOException {
        int total = doc.getNumberOfPages();
        for (int i = 0; i < total; i++) {
            PDPage page = doc.getPage(i);
            PDRectangle box = page.getMediaBox();
            String label = "Página " + (i + 1) + " de " + total;
            float size = 6.5f;
            try (PDPageContentStream cs = new PDPageContentStream(doc, page,
                    PDPageContentStream.AppendMode.APPEND, true, true)) {
                float x = box.getWidth() - MARGIN - fonts.regular.getStringWidth(label) / 1000 * size;
                write(cs, fonts.regular, size, MUTED, x, 38, label);
                if (i >= reportPages) {
                    write(cs, fonts.regular, size, MUTED, MARGIN, 38, footerLabel);
                }
            }
        }
    }

    // ── desenho ──────────────────────────────────────────────────────────────

    private static void write(PDPageContentStream cs, PDFont font, float size, Color color, float x, float y,
                              String text) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.setNonStrokingColor(color);
        cs.newLineAtOffset(x, y);
        cs.showText(text == null ? "—" : text);
        cs.endText();
    }

    private static void rule(PDPageContentStream cs, float x, float y, float w, Color color, float width)
            throws IOException {
        cs.setStrokingColor(color);
        cs.setLineWidth(width);
        cs.moveTo(x, y);
        cs.lineTo(x + w, y);
        cs.stroke();
    }

    private record Fonts(PDFont regular, PDFont bold) {}

    private Fonts loadFonts(PDDocument doc) {
        try (InputStream regular = getClass().getResourceAsStream(MONTSERRAT + "Regular.ttf");
             InputStream bold = getClass().getResourceAsStream(MONTSERRAT + "Bold.ttf")) {
            if (regular != null && bold != null) {
                return new Fonts(PDType0Font.load(doc, regular), PDType0Font.load(doc, bold));
            }
        } catch (IOException e) {
            log.warn("Montserrat não carregou para os anexos; usando Helvetica", e);
        }
        return new Fonts(new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD));
    }
}
