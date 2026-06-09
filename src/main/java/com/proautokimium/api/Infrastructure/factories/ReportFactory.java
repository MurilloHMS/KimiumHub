package com.proautokimium.api.Infrastructure.factories;

import com.proautokimium.api.Infrastructure.interfaces.reports.IReportFactory;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.util.JRLoader;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;

/**
 * Factory responsável pela geração de relatórios utilizando JasperReports.
 *
 * <p>
 * Esta classe permite gerar relatórios em diferentes formatos,
 * como PDF e imagem PNG, a partir de arquivos {@code .jrxml}.
 * </p>
 *
 * <p>
 * Os relatórios devem estar localizados em:
 * {@code /templates/reports/}
 * </p>
 */
@Component
public class ReportFactory implements IReportFactory {

    /**
     * Caminho base onde os relatórios JRXML estão armazenados.
     */
    private static final String REPORT_PATH = "/templates/reports/";

    /**
     * Gera um relatório em formato PDF.
     *
     * @param params parâmetros utilizados no preenchimento do relatório
     * @param reportLocation caminho relativo do arquivo JRXML
     *                       Exemplo: {@code assinaturas/relatorio.jrxml}
     *
     * @return relatório gerado em formato PDF como array de bytes
     *
     * @throws RuntimeException caso ocorra erro durante a geração do PDF
     */
    @Override
    public byte[] generatePdf(Map<String, Object> params, String reportLocation) {
        try {

            JasperPrint print = buildReport(params, reportLocation);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            JasperExportManager.exportReportToPdfStream(print, outputStream);

            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar PDF", e);
        }
    }

    /**
     * Gera um relatório em formato PDF.
     *
     * @param params parâmetros utilizados no preenchimento do relatório
     * @param reportLocation caminho relativo do arquivo JRXML
     *                       Exemplo: {@code assinaturas/relatorio.jrxml}
     *
     * @return relatório gerado em formato PDF como array de bytes
     *
     * @throws RuntimeException caso ocorra erro durante a geração do PDF
     */
    @Override
    public byte[] generatePdf(Map<String, Object> params, JRDataSource dataSource, String reportLocation) {
        try {

            JasperPrint print = buildReport(params,dataSource, reportLocation);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            JasperExportManager.exportReportToPdfStream(print, outputStream);

            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar PDF", e);
        }
    }

    /**
     * Gera um relatório em formato PNG.
     *
     * @param params parâmetros utilizados no preenchimento do relatório
     * @param reportLocation caminho relativo do arquivo JRXML
     *                       Exemplo: {@code assinaturas/relatorio.jrxml}
     *
     * @return imagem PNG gerada como array de bytes
     *
     * @throws RuntimeException caso ocorra erro durante a geração da imagem
     */
    @Override
    public byte[] generateImage(Map<String, Object> params, String reportLocation) {
        try {

            JasperPrint print = buildReport(params, reportLocation);

            BufferedImage image =
                    (BufferedImage) JasperPrintManager.printPageToImage(print, 0, 1f);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            ImageIO.write(image, "png", outputStream);

            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar imagem", e);
        }
    }

    /**
     * Compila e preenche um relatório JasperReports.
     *
     * <p>
     * Este método realiza:
     * </p>
     *
     * <ul>
     *     <li>Carregamento do arquivo JRXML</li>
     *     <li>Compilação do relatório</li>
     *     <li>Preenchimento dos parâmetros</li>
     * </ul>
     *
     * @param params parâmetros utilizados no relatório
     * @param reportLocation caminho relativo do arquivo JRXML
     *
     * @return instância preenchida de {@link JasperPrint}
     *
     * @throws Exception caso o arquivo não seja encontrado
     *                   ou ocorra erro na compilação/preenchimento
     */
    private JasperPrint buildReport(Map<String, Object> params, String reportLocation) throws Exception {

        try (InputStream jasperStream =
                     getClass().getResourceAsStream(REPORT_PATH + reportLocation)) {

            if (jasperStream == null) {
                throw new FileNotFoundException(
                        "Arquivo de relatório não encontrado: " + reportLocation
                );
            }

            JasperReport report = loadReport(reportLocation);

            return JasperFillManager.fillReport(
                    report,
                    params,
                    new JREmptyDataSource(1)
            );
        }
    }

    private JasperPrint buildReport( Map<String, Object> params, JRDataSource dataSource, String reportLocation ) throws Exception {
        try (InputStream jasperStream = getClass().getResourceAsStream(REPORT_PATH + reportLocation)) {
            if (jasperStream == null) {
                throw new FileNotFoundException( "Arquivo de relatório não encontrado: " + reportLocation );
            }

            JasperReport report = loadReport(reportLocation);
            return JasperFillManager.fillReport( report, params, dataSource );
        }
    }

    private JasperReport loadReport(String reportLocation) throws Exception {
        try(InputStream stream = getClass().getResourceAsStream(REPORT_PATH + reportLocation)){

            if(stream == null)
                throw new FileNotFoundException("Arquivo de relatório não encontrado: " + reportLocation);

            if(reportLocation.endsWith(".jasper"))
                return (JasperReport) JRLoader.loadObject(stream);

            if (reportLocation.endsWith(".jrxml"))
                return JasperCompileManager.compileReport(stream);

            throw new IllegalArgumentException("Formato de relatório não suportado: " + reportLocation);
        }
    }
}