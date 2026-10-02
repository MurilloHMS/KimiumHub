package com.proautokimium.api.domain.entities.humanResources;

import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.enums.humanResources.MedicalCertificateStatus;
import com.proautokimium.api.domain.enums.humanResources.SubmissionType;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidRequestDataException;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidStatusTransitionException;
import com.proautokimium.api.domain.exceptions.humanResources.SelfReviewException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A conferência do atestado: o RH confirma ou recusa, e o recusado volta com
 * outro arquivo — quantas vezes precisar, até 30 dias depois de cada recusa.
 */
class MedicalCertificateReviewTest {

    private static final LocalDateTime ENVIO = LocalDateTime.of(2026, 10, 1, 8, 0);
    private static final LocalDateTime CONFERENCIA = LocalDateTime.of(2026, 10, 2, 9, 0);

    private final Employee dono = employee("Ana");
    private final Employee rh = employee("Rita");

    private MedicalCertificate atestado() {
        return MedicalCertificate.submit(dono, LocalDate.of(2026, 9, 29), LocalDate.of(2026, 9, 30),
                SubmissionType.PHOTO, true, "foto.jpg", "9001/foto.jpg", ENVIO);
    }

    private MedicalCertificate recusado() {
        MedicalCertificate c = atestado();
        c.reject(rh, "Foto borrada", CONFERENCIA);
        return c;
    }

    @Test
    @DisplayName("nasce em conferência")
    void startsPending() {
        assertThat(atestado().getStatus()).isEqualTo(MedicalCertificateStatus.PENDING);
    }

    @Nested
    class Conferencia {

        @Test
        @DisplayName("confirmar registra quem e quando")
        void confirm() {
            MedicalCertificate c = atestado();
            c.confirmReceipt(rh, "  ", CONFERENCIA);

            assertThat(c.getStatus()).isEqualTo(MedicalCertificateStatus.RECEIVED);
            assertThat(c.getReviewedBy()).isSameAs(rh);
            assertThat(c.getReviewedAt()).isEqualTo(CONFERENCIA);
            assertThat(c.getReviewNotes()).isNull();
        }

        @Test
        @DisplayName("recusar exige o motivo, e sem ele nada muda")
        void rejectNeedsReason() {
            MedicalCertificate c = atestado();
            assertThrows(InvalidRequestDataException.class, () -> c.reject(rh, " ", CONFERENCIA));
            assertThat(c.getStatus()).isEqualTo(MedicalCertificateStatus.PENDING);

            c.reject(rh, " Foto borrada ", CONFERENCIA);
            assertThat(c.getStatus()).isEqualTo(MedicalCertificateStatus.REJECTED);
            assertThat(c.getReviewNotes()).isEqualTo("Foto borrada");
        }

        @Test
        @DisplayName("só se confere o que está em conferência")
        void onlyPending() {
            MedicalCertificate recebido = atestado();
            recebido.confirmReceipt(rh, null, CONFERENCIA);
            assertThrows(InvalidStatusTransitionException.class, () -> recebido.reject(rh, "x", CONFERENCIA));
            assertThrows(InvalidStatusTransitionException.class, () -> recebido.confirmReceipt(rh, null, CONFERENCIA));
            assertThat(recebido.getStatus()).isEqualTo(MedicalCertificateStatus.RECEIVED);

            MedicalCertificate recusado = recusado();
            assertThrows(InvalidStatusTransitionException.class, () -> recusado.confirmReceipt(rh, null, CONFERENCIA));
            assertThat(recusado.getStatus()).isEqualTo(MedicalCertificateStatus.REJECTED);
        }

        @Test
        @DisplayName("quem é do RH não confere o próprio atestado, nem como outra instância")
        void noSelfReview() {
            MedicalCertificate c = atestado();
            Employee mesmaPessoa = employee("Ana");
            setId(mesmaPessoa, dono.getId());

            assertThrows(SelfReviewException.class, () -> c.confirmReceipt(dono, null, CONFERENCIA));
            assertThrows(SelfReviewException.class, () -> c.reject(mesmaPessoa, "x", CONFERENCIA));
            assertThat(c.getStatus()).isEqualTo(MedicalCertificateStatus.PENDING);
        }
    }

    @Nested
    class Reenvio {

        @Test
        @DisplayName("reenviar troca o arquivo, guarda o anterior com a recusa e volta à conferência")
        void resubmitKeepsTrail() {
            MedicalCertificate c = recusado();
            LocalDateTime reenvio = CONFERENCIA.plusDays(1);

            c.resubmit(SubmissionType.FILE, null, "atestado.pdf", "9001/atestado.pdf", " Agora em PDF ", reenvio);

            assertThat(c.getStatus()).isEqualTo(MedicalCertificateStatus.PENDING);
            assertThat(c.getStoragePath()).isEqualTo("9001/atestado.pdf");
            assertThat(c.getSubmissionType()).isEqualTo(SubmissionType.FILE);
            assertThat(c.getResubmittedAt()).isEqualTo(reenvio);
            assertThat(c.getResubmitComment()).isEqualTo("Agora em PDF");
            assertThat(c.getSubmittedAt()).isEqualTo(ENVIO);
            assertThat(c.getReviewedBy()).isNull();
            assertThat(c.getReviewNotes()).isNull();

            assertThat(c.getPreviousAttempts()).singleElement().satisfies(a -> {
                assertThat(a.getStoragePath()).isEqualTo("9001/foto.jpg");
                assertThat(a.getSubmissionType()).isEqualTo(SubmissionType.PHOTO);
                assertThat(a.getSubmittedAt()).isEqualTo(ENVIO);
                assertThat(a.getReviewedBy()).isSameAs(rh);
                assertThat(a.getReviewNotes()).isEqualTo("Foto borrada");
                assertThat(a.getComment()).isNull();
            });
        }

        @Test
        @DisplayName("quantas vezes precisar: cada recusa vira uma linha da trilha, com o comentário daquele envio")
        void manyTimes() {
            MedicalCertificate c = recusado();
            LocalDateTime segundo = CONFERENCIA.plusDays(1);
            c.resubmit(SubmissionType.FILE, null, "a2.pdf", "p2", "Segunda", segundo);
            c.reject(rh, "Falta o CID", segundo.plusHours(1));
            c.resubmit(SubmissionType.FILE, null, "a3.pdf", "p3", null, segundo.plusDays(2));

            assertThat(c.getPreviousAttempts()).hasSize(2);
            assertThat(c.getPreviousAttempts().get(1).getStoragePath()).isEqualTo("p2");
            assertThat(c.getPreviousAttempts().get(1).getSubmittedAt()).isEqualTo(segundo);
            assertThat(c.getPreviousAttempts().get(1).getComment()).isEqualTo("Segunda");
            assertThat(c.getPreviousAttempts().get(1).getReviewNotes()).isEqualTo("Falta o CID");
            assertThat(c.getResubmitComment()).isNull();
            assertThat(c.getStoragePath()).isEqualTo("p3");
        }

        @Test
        @DisplayName("só o recusado se reenvia")
        void onlyRejected() {
            MedicalCertificate pendente = atestado();
            assertThrows(InvalidStatusTransitionException.class, () ->
                    pendente.resubmit(SubmissionType.FILE, null, "a.pdf", "p", null, CONFERENCIA));
            assertThat(pendente.getPreviousAttempts()).isEmpty();
        }

        @Test
        @DisplayName("o prazo é de 30 dias depois da recusa: no 30º dia passa, depois não")
        void window() {
            MedicalCertificate noPrazo = recusado();
            assertThat(noPrazo.resubmitDeadline()).isEqualTo(CONFERENCIA.plusDays(30));
            noPrazo.resubmit(SubmissionType.FILE, null, "a.pdf", "p", null, CONFERENCIA.plusDays(30));
            assertThat(noPrazo.getStatus()).isEqualTo(MedicalCertificateStatus.PENDING);

            MedicalCertificate atrasado = recusado();
            assertThrows(InvalidStatusTransitionException.class, () -> atrasado.resubmit(
                    SubmissionType.FILE, null, "a.pdf", "p", null, CONFERENCIA.plusDays(30).plusMinutes(1)));
            assertThat(atrasado.getStatus()).isEqualTo(MedicalCertificateStatus.REJECTED);
            assertThat(atrasado.canResubmit(CONFERENCIA.plusDays(31))).isFalse();
        }

        @Test
        @DisplayName("foto reenviada também precisa da confirmação de legível")
        void photoNeedsLegible() {
            MedicalCertificate c = recusado();
            assertThrows(InvalidRequestDataException.class, () ->
                    c.resubmit(SubmissionType.PHOTO, false, "f.jpg", "p", null, CONFERENCIA));
            assertThat(c.getStatus()).isEqualTo(MedicalCertificateStatus.REJECTED);
            assertThat(c.getPreviousAttempts()).isEmpty();
        }
    }

    private static Employee employee(String name) {
        Employee e = new Employee();
        e.setName(name);
        setId(e, UUID.randomUUID());
        return e;
    }

    private static void setId(Employee e, UUID id) {
        try {
            Field f = com.proautokimium.api.domain.abstractions.Entity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(e, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
