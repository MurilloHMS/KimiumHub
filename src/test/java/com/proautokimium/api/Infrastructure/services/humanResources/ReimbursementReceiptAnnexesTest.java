package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Infrastructure.services.humanResources.ReimbursementReceiptAnnexes.ReceiptAnnex;
import com.proautokimium.api.Infrastructure.services.storage.ReimbursementStorageService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * **O comprovante cabe na página do anexo dele.**
 *
 * Em 2026-09-28 uma foto (enviada como PDF) virou três páginas no relatório: o
 * PDF entrava com uma folha de identificação e depois as páginas originais, no
 * tamanho original. Agora cada página do PDF é desenhada DENTRO da página do
 * anexo, abaixo do cabeçalho, reduzida para caber.
 */
class ReimbursementReceiptAnnexesTest {

    @TempDir Path disco;
    private ReimbursementReceiptAnnexes annexes;

    @BeforeEach
    void setUp() {
        ReimbursementStorageService storage = mock(ReimbursementStorageService.class);
        when(storage.resolve(anyString())).thenAnswer(inv -> disco.resolve(inv.<String>getArgument(0)));
        annexes = new ReimbursementReceiptAnnexes(storage);
    }

    private static byte[] relatorioDeUmaPagina() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PDDocument d = new PDDocument()) {
            d.addPage(new PDPage(PDRectangle.A4));
            d.save(out);
        }
        return out.toByteArray();
    }

    /** Um PDF com páginas do tamanho dado, como o "digitalizar" do celular gera. */
    private void pdf(String nome, PDRectangle... paginas) throws Exception {
        try (PDDocument d = new PDDocument()) {
            for (PDRectangle p : paginas) d.addPage(new PDPage(p));
            d.save(disco.resolve(nome).toFile());
        }
    }

    private static ReceiptAnnex anexo(String arquivo) {
        return new ReceiptAnnex("A-1", "Ana", "Cód. 1042", "03/09/2026", "Hospedagem", "Pago", "R$ 320,00",
                arquivo, arquivo);
    }

    @Test
    @DisplayName("PDF de uma página enorme vira UMA página de anexo, em A4")
    void pdfGrandeCabeNumaPagina() throws Exception {
        pdf("foto.pdf", new PDRectangle(3024, 4032)); // o tamanho de uma foto de celular, em pontos

        byte[] resultado = annexes.append(relatorioDeUmaPagina(), List.of(anexo("foto.pdf")), "rodapé");

        try (PDDocument d = Loader.loadPDF(resultado)) {
            assertThat(d.getNumberOfPages()).as("relatório + 1 anexo, e não + identificação + original").isEqualTo(2);
            PDRectangle pagina = d.getPage(1).getMediaBox();
            assertThat(pagina.getWidth()).isEqualTo(PDRectangle.A4.getWidth());
            assertThat(pagina.getHeight()).isEqualTo(PDRectangle.A4.getHeight());
            String texto = new PDFTextStripper().getText(d);
            assertThat(texto).contains("Anexo A-1").doesNotContain("anexadas a seguir");
        }
    }

    @Test
    @DisplayName("PDF de três páginas: uma página de anexo por página, cada uma com cabeçalho")
    void pdfDeVariasPaginas() throws Exception {
        pdf("nota.pdf", PDRectangle.LETTER, PDRectangle.A3, PDRectangle.A5);

        byte[] resultado = annexes.append(relatorioDeUmaPagina(), List.of(anexo("nota.pdf")), "rodapé");

        try (PDDocument d = Loader.loadPDF(resultado)) {
            assertThat(d.getNumberOfPages()).isEqualTo(1 + 3);
            for (int i = 1; i < d.getNumberOfPages(); i++) {
                assertThat(d.getPage(i).getMediaBox().getWidth()).isEqualTo(PDRectangle.A4.getWidth());
            }
            String texto = new PDFTextStripper().getText(d).replaceAll("\\s+", " ");
            assertThat(texto).contains("página 1 de 3", "página 2 de 3", "página 3 de 3");
        }
    }
}
