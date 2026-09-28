package com.proautokimium.api.Application.DTOs.humanResources.VacationRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * **O saldo informado no lançamento do RH não pode ser negativo.**
 *
 * O número digitado é o saldo DEPOIS do lançamento, gravado como está — e
 * nada impedia o RH de digitar -3. Um saldo negativo trava todo pedido futuro
 * da pessoa com "saldo insuficiente", sem ninguém entender por quê.
 *
 * A regra mora no DTO porque o `createByRh` só é chamado pelo controller, que
 * já valida com `@Valid`; o 400 volta com a mensagem do campo.
 */
class CreateVacationByRhDTOTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private static CreateVacationByRhDTO comSaldo(Integer saldo) {
        return new CreateVacationByRhDTO(UUID.randomUUID(),
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 10), saldo, null);
    }

    @Test
    @DisplayName("saldo informado negativo é recusado, com mensagem para a tela")
    void saldoNegativoEhRecusado() {
        Set<ConstraintViolation<CreateVacationByRhDTO>> erros = validator.validate(comSaldo(-3));

        assertThat(erros).hasSize(1);
        ConstraintViolation<CreateVacationByRhDTO> erro = erros.iterator().next();
        assertThat(erro.getPropertyPath().toString()).isEqualTo("vacationBalanceDays");
        assertThat(erro.getMessage()).isEqualTo("O saldo de férias não pode ser negativo");
    }

    /** Zero é um saldo — as últimas férias da pessoa. Não confundir com negativo. */
    @Test
    @DisplayName("saldo zero é aceito")
    void saldoZeroEhAceito() {
        assertThat(validator.validate(comSaldo(0))).isEmpty();
    }

    /** Em branco quer dizer "o sistema desconta" — não é erro. */
    @Test
    @DisplayName("saldo em branco é aceito")
    void saldoEmBrancoEhAceito() {
        assertThat(validator.validate(comSaldo(null))).isEmpty();
    }
}
