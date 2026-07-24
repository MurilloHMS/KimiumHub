package com.proautokimium.api.domain.entities.humanResources;

import com.proautokimium.api.domain.entities.Employee;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EquipmentAssignmentTest {

    private final Employee employee = new Employee();

    @Test
    @DisplayName("Ao ser entregue, fica marcado como com o funcionário (returnedAt nulo)")
    void aoEntregarFicaComOFuncionario() {
        EquipmentAssignment assignment = EquipmentAssignment.deliver(
                employee, "Celular", "iPhone 13 - IMEI 123456", LocalDate.of(2026, 7, 1), null
        );

        assertThat(assignment.isWithEmployee()).isTrue();
        assertThat(assignment.getReturnedAt()).isNull();
    }

    @Test
    @DisplayName("Ao devolver, deixa de estar com o funcionário")
    void aoDevolverDeixaDeEstarComOFuncionario() {
        EquipmentAssignment assignment = EquipmentAssignment.deliver(
                employee, "Celular", "iPhone 13", LocalDate.of(2026, 7, 1), null
        );

        assignment.markAsReturned(LocalDate.of(2026, 7, 20));

        assertThat(assignment.isWithEmployee()).isFalse();
        assertThat(assignment.getReturnedAt()).isEqualTo(LocalDate.of(2026, 7, 20));
    }

    @Test
    @DisplayName("Não deve devolver duas vezes")
    void naoDeveDevolverDuasVezes() {
        EquipmentAssignment assignment = EquipmentAssignment.deliver(
                employee, "Celular", "iPhone 13", LocalDate.of(2026, 7, 1), null
        );
        assignment.markAsReturned(LocalDate.of(2026, 7, 20));

        assertThrows(IllegalStateException.class, () -> assignment.markAsReturned(LocalDate.of(2026, 7, 25)));
    }

    @Test
    @DisplayName("Não deve devolver antes da data de entrega")
    void naoDeveDevolverAntesDaEntrega() {
        EquipmentAssignment assignment = EquipmentAssignment.deliver(
                employee, "Veículo", "Fiat Strada - ABC1234", LocalDate.of(2026, 7, 10), null
        );

        assertThrows(IllegalArgumentException.class, () -> assignment.markAsReturned(LocalDate.of(2026, 7, 1)));
    }
}
