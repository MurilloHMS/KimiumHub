package com.proautokimium.api.Infrastructure.services.partner;

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
 * Produção roda só o JRE, sem `javac`, e só CARREGA o `.jasper` compilado aqui
 * — o mesmo padrão do comprovante de reembolsos. Editou o `.jrxml`? Recompile:
 * <pre>
 * ./mvnw test -Dtest=PendingSiteAccessReportTemplateTest -Djasper.compile=true
 * </pre>
 */
class PendingSiteAccessReportTemplateTest {

    private static final String DIR = "src/main/resources/templates/reports/site_access/";
    private static final String JRXML = DIR + "pending_site_access.jrxml";
    private static final String JASPER = DIR + "pending_site_access.jasper";

    @Test
    @EnabledIfSystemProperty(named = "jasper.compile", matches = "true")
    @DisplayName("recompila o relatório dos pendentes (-Djasper.compile=true)")
    void recompila() throws Exception {
        JasperCompileManager.compileReportToFile(JRXML, JASPER);
        assertThat(Path.of(JASPER)).exists();
    }

    @Test
    @DisplayName("o .jasper commitado tem os mesmos parâmetros e campos do .jrxml")
    void jasperAcompanhaJrxml() throws Exception {
        JasperReport doFonte = JasperCompileManager.compileReport(JRXML);
        JasperReport commitado;
        try (InputStream in = getClass().getResourceAsStream(PendingSiteAccessReportService.TEMPLATE)) {
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
