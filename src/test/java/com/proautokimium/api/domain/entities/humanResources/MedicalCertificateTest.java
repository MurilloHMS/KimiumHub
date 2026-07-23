package com.proautokimium.api.domain.entities.humanResources;

import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.enums.humanResources.SubmissionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MedicalCertificateTest {

    private final Employee employee = new Employee();
    private final LocalDateTime now = LocalDateTime.of(2026, 7, 23, 10, 0);

    @Test
    @DisplayName("Não deve enviar com data final antes da inicial")
    void naoDeveEnviarComDataFinalAntesDaInicial() {
        assertThrows(IllegalArgumentException.class, () -> MedicalCertificate.submit(
                employee, LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 10),
                SubmissionType.FILE, null, "atestado.pdf", "path", now
        ));
    }

    @Test
    @DisplayName("Não deve enviar foto sem confirmar legibilidade")
    void naoDeveEnviarFotoSemConfirmarLegibilidade() {
        assertThrows(IllegalArgumentException.class, () -> MedicalCertificate.submit(
                employee, LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 20),
                SubmissionType.PHOTO, null, "foto.jpg", "path", now
        ));
        assertThrows(IllegalArgumentException.class, () -> MedicalCertificate.submit(
                employee, LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 20),
                SubmissionType.PHOTO, false, "foto.jpg", "path", now
        ));
    }

    @Test
    @DisplayName("Deve enviar foto com legibilidade confirmada")
    void deveEnviarFotoComLegibilidadeConfirmada() {
        MedicalCertificate certificate = MedicalCertificate.submit(
                employee, LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 20),
                SubmissionType.PHOTO, true, "foto.jpg", "path", now
        );

        assertThat(certificate.getSubmissionType()).isEqualTo(SubmissionType.PHOTO);
    }

    @Test
    @DisplayName("Arquivo (não foto) não exige confirmação de legibilidade")
    void arquivoNaoExigeConfirmacaoDeLegibilidade() {
        MedicalCertificate certificate = MedicalCertificate.submit(
                employee, LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 20),
                SubmissionType.FILE, null, "atestado.pdf", "path", now
        );

        assertThat(certificate.getSubmissionType()).isEqualTo(SubmissionType.FILE);
    }

    @Test
    @DisplayName("daysCount conta os dias corridos, incluindo os dois extremos")
    void daysCountContaDiasCorridos() {
        MedicalCertificate certificate = MedicalCertificate.submit(
                employee, LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 22),
                SubmissionType.FILE, null, "atestado.pdf", "path", now
        );

        assertThat(certificate.getDaysCount()).isEqualTo(3);
    }
}
