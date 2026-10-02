package com.proautokimium.api.Infrastructure.services.reports.guide;

import com.proautokimium.api.Application.DTOs.guide.GuideReportRequestDTO;
import com.proautokimium.api.Infrastructure.repositories.ProductWebSiteRepository;
import com.proautokimium.api.Infrastructure.services.storage.EquipmentImageStorageService;
import com.proautokimium.api.Infrastructure.services.storage.ProductImageStorageService;
import com.proautokimium.api.domain.entities.ProductWebsite;
import com.proautokimium.api.domain.valueObjects.guide.GuideLayoutDocument;
import com.proautokimium.api.domain.valueObjects.guide.GuideLayoutDocument.Column;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * O PDF de verdade, gerado pelo layout — sem banco e sem Spring.
 *
 * O teste mais importante é o primeiro: a semente da V115 tem que produzir o
 * mesmo guia que o {@code guia_utilizacao.jasper} produzia. Se não produzir,
 * o deploy muda o guia de todo cliente no dia em que sobe.
 */
class GuideReportServiceTest {

    private static final String TITLE = "Restaurante Sabor da Serra";

    private final ProductWebSiteRepository products = mock(ProductWebSiteRepository.class);
    private final GuideLayoutService layouts = mock(GuideLayoutService.class);
    private final Map<UUID, ProductWebsite> catalog = new HashMap<>();
    private GuideReportService service;

    @BeforeEach
    void setUp() {
        ProductImageStorageService productImages = mock(ProductImageStorageService.class);
        EquipmentImageStorageService equipmentImages = mock(EquipmentImageStorageService.class);
        when(productImages.searchFile(any())).thenReturn(Path.of("/nao/existe.png"));
        when(equipmentImages.searchFile(any())).thenReturn(Path.of("/nao/existe.png"));
        when(products.findById(any())).thenAnswer(inv -> Optional.ofNullable(catalog.get(inv.<UUID>getArgument(0))));

        service = new GuideReportService(products, productImages, equipmentImages, layouts,
                new GuideReportCompiler(new GuideJasperDesignBuilder()));
        ReflectionTestUtils.setField(service, "logoEmpresaPath", "classpath:/templates/images/logo.png");
        publish(GuideLayoutSeed.document());
    }

    @Test
    @DisplayName("a semente da V115 imprime as mesmas palavras que o guia_utilizacao.jasper antigo")
    void sementeIgualAoGuiaAntigo() throws Exception {
        List<UUID> ids = List.of(
                product("Detergente Alcalino Clorado", "10234", "#F2C94C", "Limpeza pesada de pisos",
                        "Remove gordura carbonizada de pisos e coifas. Não usar em alumínio.", "1:20", "5%", "Cozinha industrial"),
                product("Sanitizante Quaternário", null, "#5B8DEF", "Sanitização de superfícies",
                        null, null, "0,5%", "Áreas de manipulação"));

        byte[] novo = service.generate(new GuideReportRequestDTO(TITLE, ids), null);
        byte[] antigo = oldJasper(ids);

        assertThat(words(novo)).isEqualTo(words(antigo));
    }

    @Test
    @DisplayName("tirar uma coluna tira o título e o valor dela do PDF")
    void tirarColuna() throws Exception {
        GuideLayoutDocument seed = GuideLayoutSeed.document();
        List<Column> columns = new ArrayList<>(seed.table().columns());
        columns.removeIf(c -> c.title().equals("CONCENTRAÇÃO"));
        publish(withColumns(seed, columns));
        UUID id = product("Limpa Vidros", "10902", "#9AD0F5", "Vidros", "Pronto uso", "Pronto uso", "7,5%", "Salão");

        String text = text(service.generate(new GuideReportRequestDTO(TITLE, List.of(id)), null));

        assertThat(text).doesNotContain("CONCENTRAÇÃO").doesNotContain("7,5%");
        assertThat(text).contains("DILUIÇÃO").contains("LIMPA VIDROS");
    }

    @Test
    @DisplayName("aspas e barra invertida no texto do designer não quebram a compilação")
    void textoComCaracteresEspeciais() throws Exception {
        GuideLayoutDocument seed = GuideLayoutSeed.document();
        List<GuideLayoutDocument.Element> footer = new ArrayList<>(seed.footer().elements());
        footer.add(new GuideLayoutDocument.Element("TEXT", 0, 40, 330, 12, "Use \"luvas\" \\ sempre - {titulo}",
                7f, false, "#FFFFFF", "LEFT", "MIDDLE", null, null));
        publish(new GuideLayoutDocument(seed.page(), seed.header(), seed.table(),
                new GuideLayoutDocument.Band(seed.footer().height(), footer)));
        UUID id = product("Produto", "1", "#57C1AB", "Uso", null, "1:10", "1%", "Geral");

        String text = text(service.generate(new GuideReportRequestDTO(TITLE, List.of(id)), null));

        assertThat(text).contains("Use \"luvas\" \\ sempre - RESTAURANTE SABOR DA SERRA");
    }

    @Test
    @DisplayName("o compilador que funciona dentro do jar está no classpath")
    void compiladorDoJar() {
        // Sem o jasperreports-jdt, o Jasper compila com o javac, que não acha
        // as classes dentro do jar do Spring Boot: "package
        // net.sf.jasperreports.engine does not exist". Aqui, fora do jar, o
        // javac funciona — então a única rede é exigir a dependência.
        assertThat(classExists("net.sf.jasperreports.jdt.JRJdtCompiler"))
                .as("jasperreports-jdt no pom.xml — o guia é montado em tempo de execução")
                .isTrue();
    }

    // ── Montagem ────────────────────────────────────────────────────────────

    private void publish(GuideLayoutDocument layout) {
        try {
            String text = new ObjectMapper().writeValueAsString(layout);
            when(layouts.publishedLayout()).thenReturn(new GuideLayoutService.ParsedLayout(text, layout));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private UUID product(String name, String code, String color, String purpose, String description,
                         String dilution, String concentration, String area) {
        ProductWebsite p = new ProductWebsite();
        p.setName(name);
        p.setSystemCode(code);
        p.setCores(List.of(color));
        p.setFinalidade(purpose);
        p.setDescricao(description);
        p.setDiluicao(dilution);
        p.setConcentracao(concentration);
        p.setLocalUso(area);
        p.setEquipmentGuides(List.of());
        UUID id = UUID.randomUUID();
        catalog.put(id, p);
        return id;
    }

    private static GuideLayoutDocument withColumns(GuideLayoutDocument seed, List<Column> columns) {
        GuideLayoutDocument.Table t = seed.table();
        return new GuideLayoutDocument(seed.page(), seed.header(),
                new GuideLayoutDocument.Table(t.headerBackground(), t.headerColor(), t.headerFontSize(),
                        t.headerHeight(), t.minRowHeight(), t.dividerColor(), t.separatorColor(), columns),
                seed.footer());
    }

    /** O guia como era antes, pelo .jasper pré-compilado e com as linhas no formato dele. */
    private byte[] oldJasper(List<UUID> ids) throws Exception {
        List<OldRow> rows = ids.stream().map(catalog::get).map(OldRow::new).toList();
        Map<String, Object> params = new HashMap<>();
        params.put("TITULO_GUIA", TITLE.toUpperCase());
        params.put("LOGO_EMPRESA", getClass().getResourceAsStream("/templates/images/logo.png"));
        for (String icon : List.of("MASCARA", "OCULOS", "AVENTAL", "TOUCA", "LUVA", "BOTA")) {
            params.put("LOGO_" + icon, getClass().getResourceAsStream("/templates/images/icones-guia/" + icon.toLowerCase() + ".png"));
        }
        try (InputStream in = getClass().getResourceAsStream("/templates/reports/guide/guia_utilizacao.jasper")) {
            JasperReport report = (JasperReport) JRLoader.loadObject(in);
            return JasperExportManager.exportReportToPdf(JasperFillManager.fillReport(report, params,
                    rows.isEmpty() ? new JREmptyDataSource(0) : new JRBeanCollectionDataSource(rows)));
        }
    }

    private static List<String> words(byte[] pdf) throws Exception {
        return Arrays.stream(text(pdf).split("\\s+")).filter(w -> !w.isBlank()).sorted().toList();
    }

    private static String text(byte[] pdf) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /** A linha no formato do .jasper antigo, que declarava as imagens como InputStream. */
    public static class OldRow {
        private final ProductWebsite p;
        private final String coresHex;

        OldRow(ProductWebsite p) {
            this.p = p;
            this.coresHex = String.join(",", p.getCores());
        }

        public String getNome() { return p.getName(); }
        public String getSystemCode() { return p.getSystemCode(); }
        public InputStream getImagemUrl() { return null; }
        public String getCoresHex() { return coresHex; }
        public String getFinalidade() { return p.getFinalidade(); }
        public String getDescricao() { return p.getDescricao(); }
        public String getDiluicao() { return p.getDiluicao(); }
        public String getConcentracao() { return p.getConcentracao(); }
        public String getLocalUso() { return p.getLocalUso(); }
        public String getEquipamentos() { return null; }
        public List<?> getEquipImagens() { return List.of(); }
        public InputStream getCirculoCorImagem() {
            return com.proautokimium.api.Infrastructure.utils.ColorCircleRenderer.render(coresHex);
        }
        public String getCorNome() { return com.proautokimium.api.Infrastructure.utils.ColorNameUtil.toNames(coresHex); }
        public List<?> getEquipNomes() { return List.of(); }
    }
}
