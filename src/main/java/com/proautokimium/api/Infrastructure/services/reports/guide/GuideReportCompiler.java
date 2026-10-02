package com.proautokimium.api.Infrastructure.services.reports.guide;

import com.proautokimium.api.domain.valueObjects.guide.GuideLayoutDocument;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Compila o layout em relatório, e lembra do resultado.
 *
 * Compilar é a parte cara (centenas de milissegundos) e o resultado só depende
 * do documento. A chave é o hash do texto do documento: o publicado compila uma
 * vez, e a prévia do designer só recompila quando ele muda alguma coisa.
 *
 * Guarda no máximo {@value #MAX_ENTRIES}: cada ajuste do designer gera uma
 * prévia com um documento novo, e sem limite isso cresceria a tarde inteira.
 */
@Component
public class GuideReportCompiler {

    private static final int MAX_ENTRIES = 16;

    private final GuideJasperDesignBuilder builder;

    private final Map<String, JasperReport> cache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, JasperReport> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    public GuideReportCompiler(GuideJasperDesignBuilder builder) {
        this.builder = builder;
    }

    public JasperReport compile(String documentText, GuideLayoutDocument layout) throws JRException {
        String key = sha256(documentText);
        synchronized (cache) {
            JasperReport cached = cache.get(key);
            if (cached != null) return cached;
        }
        JasperReport report = JasperCompileManager.compileReport(builder.build(layout));
        synchronized (cache) {
            cache.put(key, report);
        }
        return report;
    }

    private static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
