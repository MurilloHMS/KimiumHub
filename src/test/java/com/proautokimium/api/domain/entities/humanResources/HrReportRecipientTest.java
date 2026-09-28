package com.proautokimium.api.domain.entities.humanResources;

import com.proautokimium.api.domain.exceptions.humanResources.InvalidRequestDataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HrReportRecipientTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 9, 28, 10, 0);

    /** "RH@..." e "rh@..." são a mesma caixa; o UNIQUE do banco compara texto. */
    @Test
    @DisplayName("o e-mail entra sem espaços e em minúsculas")
    void normaliza() {
        HrReportRecipient r = HrReportRecipient.create("  RH@ProautoKimium.com.BR ", "carla.rh", AGORA);

        assertThat(r.getEmail()).isEqualTo("rh@proautokimium.com.br");
        assertThat(r.getCreatedBy()).isEqualTo("carla.rh");
        assertThat(r.getCreatedAt()).isEqualTo(AGORA);
    }

    @Test
    @DisplayName("e-mail inválido é recusado com 400")
    void invalido() {
        assertThrows(InvalidRequestDataException.class, () -> HrReportRecipient.create("rh@", "x", AGORA));
        assertThrows(InvalidRequestDataException.class, () -> HrReportRecipient.create("   ", "x", AGORA));
        assertThrows(InvalidRequestDataException.class, () -> HrReportRecipient.create(null, "x", AGORA));
    }
}
