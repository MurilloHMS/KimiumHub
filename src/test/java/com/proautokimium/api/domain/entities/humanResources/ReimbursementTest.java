package com.proautokimium.api.domain.entities.humanResources;

import com.proautokimium.api.domain.entities.Employee;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReimbursementTest {

    private final Employee employee = new Employee();
    private final Employee reviewer = new Employee();
    private final LocalDateTime now = LocalDateTime.of(2026, 7, 23, 10, 0);

    private Reimbursement newRequest() {
        return Reimbursement.request(
                employee, LocalDate.of(2026, 7, 20), new BigDecimal("150.00"),
                "Restaurante", "Almoço com cliente", "nota.jpg", "EMP001/nota.jpg", now
        );
    }

    @Test
    @DisplayName("Não deve criar reembolso com valor zero ou negativo")
    void naoDeveCriarComValorInvalido() {
        assertThrows(IllegalArgumentException.class, () -> Reimbursement.request(
                employee, LocalDate.of(2026, 7, 20), BigDecimal.ZERO,
                "Restaurante", "Almoço", "nota.jpg", "path", now
        ));
        assertThrows(IllegalArgumentException.class, () -> Reimbursement.request(
                employee, LocalDate.of(2026, 7, 20), new BigDecimal("-10"),
                "Restaurante", "Almoço", "nota.jpg", "path", now
        ));
    }

    @Test
    @DisplayName("Deve aprovar reembolso pendente")
    void deveAprovarPendente() {
        Reimbursement reimbursement = newRequest();

        reimbursement.approve(reviewer, "Ok, dentro da política", now.plusHours(1));

        assertThat(reimbursement.getStatus().name()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("Não deve reprovar sem motivo")
    void naoDeveReprovarSemMotivo() {
        Reimbursement reimbursement = newRequest();

        assertThrows(IllegalArgumentException.class, () -> reimbursement.reject(reviewer, null, now));
        assertThrows(IllegalArgumentException.class, () -> reimbursement.reject(reviewer, "   ", now));
    }

    @Test
    @DisplayName("Não deve pagar reembolso que ainda não foi aprovado")
    void naoDevePagarSemAprovar() {
        Reimbursement reimbursement = newRequest();

        assertThrows(IllegalStateException.class, () -> reimbursement.pay(LocalDate.of(2026, 8, 5), now));
    }

    @Test
    @DisplayName("Deve pagar reembolso aprovado e mudar status pra PAID")
    void devePagarReembolsoAprovado() {
        Reimbursement reimbursement = newRequest();
        reimbursement.approve(reviewer, "Ok", now);

        reimbursement.pay(LocalDate.of(2026, 8, 5), now.plusDays(1));

        assertThat(reimbursement.getStatus().name()).isEqualTo("PAID");
        assertThat(reimbursement.getPaymentDate()).isEqualTo(LocalDate.of(2026, 8, 5));
    }

    @Test
    @DisplayName("Não deve aprovar reembolso que já foi decidido")
    void naoDeveAprovarJaDecidido() {
        Reimbursement reimbursement = newRequest();
        reimbursement.reject(reviewer, "Fora da política", now);

        assertThrows(IllegalStateException.class, () -> reimbursement.approve(reviewer, "ok", now));
    }
}
