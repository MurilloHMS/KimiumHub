package com.proautokimium.api.Infrastructure.services.certificate;

import com.proautokimium.api.Infrastructure.exceptions.certificate.FailedToCreateCertificate;
import com.proautokimium.api.Infrastructure.services.reports.CertificateGeneratorReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

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
}