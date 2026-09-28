package com.proautokimium.api.Infrastructure.services.humanResources;

import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * **O `.jasper` commitado precisa acompanhar o `.jrxml`.**
 *
 * O container de produção roda só o JRE, sem `javac`, e compilar um template
 * do Jasper gera e compila Java — então o servidor só CARREGA o `.jasper`
 * compilado aqui (o mesmo padrão do abastecimento e do guia). Os testes rodam
 * no JDK, onde compilar funciona, e por isso não pegaram o 503 da primeira
 * emissão de verdade (2026-09-28).
 *
 * Editou o `.jrxml`? Recompile:
 * <pre>
 * ./mvnw test -Dtest=ReimbursementReportTemplateTest -Djasper.compile=true
 * </pre>
 */
class ReimbursementReportTemplateTest {

    private static final String DIR = "src/main/resources/templates/reports/reimbursements/";
    private static final String JRXML = DIR + "comprovante_reembolsos.jrxml";
    private static final String JASPER = DIR + "comprovante_reembolsos.jasper";

    /** Só roda quando pedido: grava o `.jasper` a partir do `.jrxml`. */
    @Test
    @EnabledIfSystemProperty(named = "jasper.compile", matches = "true")
    @DisplayName("recompila o comprovante (-Djasper.compile=true)")
    void recompila() throws Exception {
        JasperCompileManager.compileReportToFile(JRXML, JASPER);
        assertThat(Path.of(JASPER)).exists();
    }

    /**
     * Rede contra esquecer de recompilar: parâmetros e campos do `.jasper`
     * têm que ser os do `.jrxml`. Não pega mudança só de layout (posição,
     * cor) — essa aparece ao olhar o PDF.
     */
    @Test
    @DisplayName("o .jasper commitado tem os mesmos parâmetros e campos do .jrxml")
    void jasperAcompanhaJrxml() throws Exception {
        JasperReport doFonte = JasperCompileManager.compileReport(JRXML);
        JasperReport commitado;
        try (InputStream in = getClass().getResourceAsStream(ReimbursementReportService.TEMPLATE)) {
            assertThat(in).as("o .jasper precisa existir no classpath").isNotNull();
            commitado = (JasperReport) JRLoader.loadObject(in);
        }

        assertThat(nomes(commitado.getParameters())).isEqualTo(nomes(doFonte.getParameters()));
        assertThat(nomes(commitado.getFields())).isEqualTo(nomes(doFonte.getFields()));
    }

    private static Set<String> nomes(JRParameter[] params) {
        return Arrays.stream(params).filter(p -> !p.isSystemDefined()).map(JRParameter::getName)
                .collect(Collectors.toSet());
    }

    private static Set<String> nomes(JRField[] fields) {
        return Arrays.stream(fields).map(JRField::getName).collect(Collectors.toSet());
    }
}
