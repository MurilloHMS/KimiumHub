package com.proautokimium.api.domain.entities.humanResources;

import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.enums.humanResources.EmployeeDocumentStatus;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidRequestDataException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A situação do documento do funcionário e a substituição.
 *
 * JUnit puro, sem Spring nem Clock: {@code statusOn} recebe o dia, então cada
 * caso é uma data fixa — nenhum teste depende de quando roda.
 */
class EmployeeDocumentTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 29);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 29, 10, 0);

    private static Employee employee() {
        Employee employee = new Employee();
        employee.id = UUID.randomUUID();
        return employee;
    }

    private static EmployeeDocument document(Employee owner, EmployeeDocumentType type, LocalDate dueDate) {
        EmployeeDocument document = new EmployeeDocument();
        document.id = UUID.randomUUID();
        document.setEmployee(owner);
        document.setType(type);
        document.setDueDate(dueDate);
        return document;
    }

    private static EmployeeDocumentType typeWithAlerts(Integer... days) {
        EmployeeDocumentType type = EmployeeDocumentType.create("ASO", NOW);
        type.configureAlerts(List.of(days), true, List.of());
        return type;
    }

    @Nested
    class Status {

        @Test
        void semVencimentoNuncaVence() {
            assertThat(document(employee(), null, null).statusOn(TODAY))
                    .isEqualTo(EmployeeDocumentStatus.NO_DUE_DATE);
        }

        /** Ontem já é vencido; hoje ainda não — vence no fim do dia. */
        @Test
        void venceuOntemEstaVencido() {
            assertThat(document(employee(), null, TODAY.minusDays(1)).statusOn(TODAY))
                    .isEqualTo(EmployeeDocumentStatus.EXPIRED);
        }

        @Test
        void venceHojeAindaEstaVencendoNaoVencido() {
            assertThat(document(employee(), null, TODAY).statusOn(TODAY))
                    .isEqualTo(EmployeeDocumentStatus.EXPIRING);
        }

        /** Sem tipo (os documentos da V59), a janela padrão é de 30 dias. */
        @Test
        void semTipoAJanelaEDeTrintaDias() {
            assertThat(document(employee(), null, TODAY.plusDays(30)).statusOn(TODAY))
                    .isEqualTo(EmployeeDocumentStatus.EXPIRING);
            assertThat(document(employee(), null, TODAY.plusDays(31)).statusOn(TODAY))
                    .isEqualTo(EmployeeDocumentStatus.VALID);
        }

        /**
         * **A janela vem do tipo.** Uma NR avisada com 60 dias já está "vence em
         * breve" a 45 dias — com a janela fixa de 30, ela apareceria "válida" no
         * mesmo dia em que o responsável recebeu o aviso.
         */
        @Test
        void aJanelaEOMaiorDiaDeAvisoDoTipo() {
            EmployeeDocumentType nr = typeWithAlerts(60, 15);

            assertThat(document(employee(), nr, TODAY.plusDays(45)).statusOn(TODAY))
                    .isEqualTo(EmployeeDocumentStatus.EXPIRING);
            assertThat(document(employee(), nr, TODAY.plusDays(61)).statusOn(TODAY))
                    .isEqualTo(EmployeeDocumentStatus.VALID);
        }

        /** Um ASO velho e vencido não é pendência: já existe um novo no lugar. */
        @Test
        void substituidoVenceQualquerData() {
            Employee owner = employee();
            EmployeeDocument old = document(owner, null, TODAY.minusDays(10));
            old.replaceWith(document(owner, null, TODAY.plusYears(1)), NOW);

            assertThat(old.statusOn(TODAY)).isEqualTo(EmployeeDocumentStatus.REPLACED);
        }
    }

    @Nested
    class Substituicao {

        @Test
        void marcaQuemSubstituiuEQuando() {
            Employee owner = employee();
            EmployeeDocument old = document(owner, null, null);
            EmployeeDocument newer = document(owner, null, null);

            old.replaceWith(newer, NOW);

            assertThat(old.getReplacedBy()).isSameAs(newer);
            assertThat(old.getReplacedAt()).isEqualTo(NOW);
        }

        /** Substituir o ASO do João pelo da Maria deixaria o do João "resolvido". */
        @Test
        void naoSubstituiPorDocumentoDeOutroFuncionario() {
            EmployeeDocument old = document(employee(), null, TODAY);

            assertThatThrownBy(() -> old.replaceWith(document(employee(), null, null), NOW))
                    .isInstanceOf(InvalidRequestDataException.class);
            assertThat(old.getReplacedBy()).isNull();
        }

        @Test
        void naoSubstituiDuasVezes() {
            Employee owner = employee();
            EmployeeDocument old = document(owner, null, null);
            EmployeeDocument first = document(owner, null, null);
            old.replaceWith(first, NOW);

            assertThatThrownBy(() -> old.replaceWith(document(owner, null, null), NOW))
                    .isInstanceOf(InvalidRequestDataException.class);
            assertThat(old.getReplacedBy()).isSameAs(first);
        }

        @Test
        void naoSubstituiPorSiMesmo() {
            EmployeeDocument document = document(employee(), null, null);

            assertThatThrownBy(() -> document.replaceWith(document, NOW))
                    .isInstanceOf(InvalidRequestDataException.class);
        }
    }

    @Nested
    class Tipo {

        @Test
        void nomeEmBrancoERecusado() {
            assertThatThrownBy(() -> EmployeeDocumentType.create("  ", NOW))
                    .isInstanceOf(InvalidRequestDataException.class);
        }

        /** "0 dias antes" é o próprio vencimento, que tem a sua chave. */
        @Test
        void diaDeAvisoZeroOuNegativoERecusado() {
            EmployeeDocumentType type = EmployeeDocumentType.create("ASO", NOW);

            assertThatThrownBy(() -> type.configureAlerts(List.of(30, 0), true, List.of()))
                    .isInstanceOf(InvalidRequestDataException.class);
            assertThat(type.getAlertDaysBefore()).isEmpty();
        }

        @Test
        void configurarTrocaTudoDeUmaVez() {
            EmployeeDocumentType type = typeWithAlerts(30, 7);
            UUID responsible = UUID.randomUUID();

            type.configureAlerts(List.of(15), false, List.of(responsible));

            assertThat(type.getAlertDaysBefore()).containsExactly(15);
            assertThat(type.getRecipientEmployeeIds()).containsExactly(responsible);
            assertThat(type.isNotifyOnExpiry()).isFalse();
        }
    }
}
