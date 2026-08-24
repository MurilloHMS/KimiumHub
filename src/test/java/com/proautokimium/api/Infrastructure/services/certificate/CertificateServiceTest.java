package com.proautokimium.api.Infrastructure.services.certificate;

import com.proautokimium.api.Infrastructure.exceptions.certificate.FailedToCreateCertificate;
import com.proautokimium.api.Infrastructure.services.reports.CertificateGeneratorReport;
import net.sf.jasperreports.engine.JasperReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificateServiceTest {

    @Mock
    private CertificateGeneratorReport certificateGeneratorReport;

    @InjectMocks
    private CertificateService certificateService;

    @Test
    @DisplayName("Deve retornar certificado criado")
    void shouldGenerateCertificateSuccessfully() {

        byte[] expected = "pdf".getBytes();

        when(certificateGeneratorReport.generate(any(Map.class), eq("certificate.jrxml")))
                .thenReturn(expected);

        byte[] result = certificateService.generateCertificate("Murillo");

        assertNotNull(result);
        assertArrayEquals(expected, result);

        verify(certificateGeneratorReport, times(1))
                .generate(any(Map.class), eq("certificate.jrxml"));
    }

    @Test
    @DisplayName("Deve lançar exceção quando imagem de background não for encontrada")
    void shouldThrowExceptionWhenBackgroundImageNotFound() {

        CertificateService service = new CertificateService() {
            @Override
            public byte[] generateCertificate(String name) {

                if (getClass().getResourceAsStream("/arquivo-inexistente.png") == null) {
                    throw new FailedToCreateCertificate("Imagem de background não encontrada");
                }

                return new byte[0];
            }
        };

        FailedToCreateCertificate exception = assertThrows(
                FailedToCreateCertificate.class,
                () -> service.generateCertificate("Murillo")
        );

        assertEquals("Imagem de background não encontrada", exception.getMessage());
    }

    // ─── Lote ────────────────────────────────────────────────────────────────

    /**
     * A razão de `compile` e `fill` serem métodos separados.
     *
     * Compilar o `.jrxml` é a parte cara; preencher é barata. Antes do lote,
     * cada certificado compilava o template de novo — 200 nomes, 200
     * compilações do mesmo arquivo. Nada quebrava, só demorava, e é o tipo de
     * regressão que volta calada numa "simplificação" futura.
     */
    @Test
    @DisplayName("Lote compila o template uma vez só, não uma vez por nome")
    void deveCompilarOTemplateUmaVezSoParaVariosNomes() {
        JasperReport template = mock(JasperReport.class);
        when(certificateGeneratorReport.compile("certificate.jrxml")).thenReturn(template);
        when(certificateGeneratorReport.fill(eq(template), any())).thenReturn("pdf".getBytes());

        certificateService.generateCertificatesZip(List.of("Ana", "Bruno", "Carla"));

        verify(certificateGeneratorReport, times(1)).compile("certificate.jrxml");
        verify(certificateGeneratorReport, times(3)).fill(eq(template), any());
    }

    /**
     * A armadilha silenciosa da imagem de fundo.
     *
     * `InputStream` só se lê uma vez. Se o mesmo stream fosse reaproveitado no
     * laço, o primeiro certificado sairia com fundo e os outros em branco —
     * sem exceção, sem log, sem nada. Este teste exige que cada preenchimento
     * receba um stream **próprio e ainda cheio**.
     */
    @Test
    @DisplayName("Cada preenchimento recebe o seu próprio stream de fundo, ainda por ler")
    void cadaPreenchimentoRecebeUmStreamDeFundoProprio() throws IOException {
        JasperReport template = mock(JasperReport.class);
        when(certificateGeneratorReport.compile("certificate.jrxml")).thenReturn(template);
        when(certificateGeneratorReport.fill(eq(template), any())).thenReturn("pdf".getBytes());

        certificateService.generateCertificatesZip(List.of("Ana", "Bruno"));

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(certificateGeneratorReport, times(2)).fill(eq(template), captor.capture());

        List<Object> streams = new ArrayList<>();
        for (Map<String, Object> params : captor.getAllValues()) {
            InputStream fundo = (InputStream) params.get("BACKGROUND_IMAGE");
            assertNotNull(fundo, "o fundo não chegou nos parâmetros");
            assertTrue(fundo.available() > 0, "o stream do fundo chegou vazio");
            streams.add(fundo);
        }

        assertNotSame(streams.get(0), streams.get(1), "os dois preenchimentos dividiram o mesmo stream");
    }

    @Test
    @DisplayName("Cada nome vira uma entrada no ZIP, em maiúsculas")
    void deveEntregarUmaEntradaNoZipParaCadaNome() throws IOException {
        JasperReport template = mock(JasperReport.class);
        when(certificateGeneratorReport.compile("certificate.jrxml")).thenReturn(template);
        when(certificateGeneratorReport.fill(eq(template), any())).thenReturn("pdf".getBytes());

        byte[] zip = certificateService.generateCertificatesZip(List.of("  Ana Souza ", "Bruno Lima"));

        assertEquals(List.of("ANA SOUZA.pdf", "BRUNO LIMA.pdf"), entradasDo(zip));
    }

    /**
     * Dois homônimos não podem derrubar o lote inteiro.
     *
     * `ZipOutputStream` lança exceção em entrada duplicada — não sobrescreve em
     * silêncio. Sem o `uniqueName`, duas "Ana Souza" na mesma turma matariam as
     * outras 198.
     */
    @Test
    @DisplayName("Homônimos viram entradas distintas, sem derrubar o lote")
    void homonimosNaoDerrubamOLote() throws IOException {
        JasperReport template = mock(JasperReport.class);
        when(certificateGeneratorReport.compile("certificate.jrxml")).thenReturn(template);
        when(certificateGeneratorReport.fill(eq(template), any())).thenReturn("pdf".getBytes());

        byte[] zip = certificateService.generateCertificatesZip(List.of("Ana Souza", "Ana Souza", "Ana Souza"));

        assertEquals(3, entradasDo(zip).size());
        assertEquals(3, entradasDo(zip).stream().distinct().count());
    }

    /**
     * Nome com barra viraria pasta dentro do ZIP: "MARIA / RH" não é um arquivo,
     * é a pasta "MARIA " com "RH.pdf" dentro. Vem de colar coluna de planilha.
     */
    @Test
    @DisplayName("Barra no nome não vira pasta dentro do ZIP")
    void nomeComBarraNaoViraPasta() throws IOException {
        JasperReport template = mock(JasperReport.class);
        when(certificateGeneratorReport.compile("certificate.jrxml")).thenReturn(template);
        when(certificateGeneratorReport.fill(eq(template), any())).thenReturn("pdf".getBytes());

        byte[] zip = certificateService.generateCertificatesZip(List.of("Maria / RH"));

        assertFalse(entradasDo(zip).get(0).contains("/"), "a barra sobreviveu e o ZIP ganhou uma pasta");
    }

    /** Lê os nomes das entradas de um ZIP em memória, na ordem em que foram gravadas. */
    private List<String> entradasDo(byte[] zip) throws IOException {
        List<String> nomes = new ArrayList<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entrada;
            while ((entrada = in.getNextEntry()) != null) {
                nomes.add(entrada.getName());
            }
        }
        return nomes;
    }
}
