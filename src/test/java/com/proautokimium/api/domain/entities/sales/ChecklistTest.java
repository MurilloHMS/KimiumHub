package com.proautokimium.api.domain.entities.sales;

import com.proautokimium.api.domain.enums.sales.ChecklistStatus;
import com.proautokimium.api.domain.exceptions.sales.ChecklistTransitionException;
import com.proautokimium.api.domain.exceptions.sales.InvalidChecklistException;
import com.proautokimium.api.domain.valueObjects.sales.ChecklistContent;
import com.proautokimium.api.domain.valueObjects.sales.ChecklistFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O fluxo do checklist: enviado → aprovado ou devolvido; alteração pedida →
 * liberada ou negada. Cada recusa prova duas coisas: a exceção, e que a
 * situação não mudou.
 */
class ChecklistTest {

    static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 30, 10, 0);
    static final String SELLER = "diego";
    static final String REVIEWER = "fernanda";

    static Checklist submitted() {
        return Checklist.submit(UUID.randomUUID(), SELLER, "Diego Martins", ChecklistFixtures.valid(), true, NOW, NOW);
    }

    @Test
    @DisplayName("o envio grava o cabeçalho a partir do conteúdo e refaz o total do pedido")
    void submitFillsHeader() {
        Checklist c = submitted();

        assertThat(c.getStatus()).isEqualTo(ChecklistStatus.SUBMITTED);
        assertThat(c.getVersion()).isEqualTo(1);
        assertThat(c.isNew()).isTrue();
        assertThat(c.getCustomerCode()).isEqualTo(3661);
        assertThat(c.getCustomerDocument()).isEqualTo(ChecklistFixtures.CNPJ);
        assertThat(c.isHasOrder()).isTrue();
        assertThat(c.getOrderTotal()).isEqualByComparingTo("1209.86");
        assertThat(c.isFilledOffline()).isTrue();
    }

    @Test
    @DisplayName("checklist incompleto não é gravado, e a mensagem diz o que falta")
    void incompleteIsRefused() {
        ChecklistContent broken = ChecklistFixtures.withAddress(ChecklistFixtures.valid(), null);

        assertThatThrownBy(() -> Checklist.submit(UUID.randomUUID(), SELLER, "Diego", broken, false, null, NOW))
                .isInstanceOf(InvalidChecklistException.class)
                .hasMessageContaining("Etapa 2 — endereço principal: preencha o endereço.");
    }

    @Nested
    @DisplayName("Controladoria")
    class Review {

        @Test
        @DisplayName("aprova o que está aguardando análise")
        void approve() {
            Checklist c = submitted();
            c.approve(REVIEWER, null, NOW);
            assertThat(c.getStatus()).isEqualTo(ChecklistStatus.APPROVED);
            assertThat(c.getReviewedByLogin()).isEqualTo(REVIEWER);
        }

        @Test
        @DisplayName("não aprova duas vezes")
        void approveTwice() {
            Checklist c = submitted();
            c.approve(REVIEWER, null, NOW);
            assertThatThrownBy(() -> c.approve(REVIEWER, null, NOW)).isInstanceOf(ChecklistTransitionException.class);
            assertThat(c.getStatus()).isEqualTo(ChecklistStatus.APPROVED);
        }

        @Test
        @DisplayName("quem emitiu não analisa o próprio checklist")
        void notOwn() {
            Checklist c = submitted();
            assertThatThrownBy(() -> c.approve(SELLER, null, NOW))
                    .isInstanceOf(ChecklistTransitionException.class)
                    .hasMessage("Você não pode analisar um checklist emitido por você.");
            assertThat(c.getStatus()).isEqualTo(ChecklistStatus.SUBMITTED);
        }

        @Test
        @DisplayName("devolver exige o motivo, que é o que o vendedor vai ler")
        void returnNeedsReason() {
            Checklist c = submitted();
            assertThatThrownBy(() -> c.returnToSeller(REVIEWER, " ", NOW)).isInstanceOf(InvalidChecklistException.class);
            assertThat(c.getStatus()).isEqualTo(ChecklistStatus.SUBMITTED);

            c.returnToSeller(REVIEWER, "Faltou a IE", NOW);
            assertThat(c.getStatus()).isEqualTo(ChecklistStatus.RETURNED);
            assertThat(c.getReviewNotes()).isEqualTo("Faltou a IE");
        }
    }

    @Nested
    @DisplayName("Reenvio")
    class Resubmit {

        @Test
        @DisplayName("devolvido: o vendedor corrige e reenvia, e a versão sobe")
        void afterReturn() {
            Checklist c = submitted();
            c.returnToSeller(REVIEWER, "Faltou a IE", NOW);

            c.resubmit(SELLER, ChecklistFixtures.valid(), NOW.plusHours(1));

            assertThat(c.getStatus()).isEqualTo(ChecklistStatus.SUBMITTED);
            assertThat(c.getVersion()).isEqualTo(2);
        }

        @Test
        @DisplayName("enviado ou aprovado não se edita: precisa pedir alteração")
        void notEditableWithoutPermission() {
            Checklist c = submitted();
            assertThatThrownBy(() -> c.resubmit(SELLER, ChecklistFixtures.valid(), NOW))
                    .isInstanceOf(ChecklistTransitionException.class)
                    .hasMessageContaining("peça alteração à Controladoria");
            assertThat(c.getVersion()).isEqualTo(1);

            c.approve(REVIEWER, null, NOW);
            assertThatThrownBy(() -> c.resubmit(SELLER, ChecklistFixtures.valid(), NOW))
                    .isInstanceOf(ChecklistTransitionException.class);
            assertThat(c.getStatus()).isEqualTo(ChecklistStatus.APPROVED);
        }

        @Test
        @DisplayName("só quem emitiu reenvia")
        void onlyOwner() {
            Checklist c = submitted();
            c.returnToSeller(REVIEWER, "Faltou a IE", NOW);
            assertThatThrownBy(() -> c.resubmit("outro", ChecklistFixtures.valid(), NOW))
                    .isInstanceOf(ChecklistTransitionException.class);
            assertThat(c.getStatus()).isEqualTo(ChecklistStatus.RETURNED);
        }
    }

    @Nested
    @DisplayName("Pedido de alteração")
    class ChangeRequest {

        @Test
        @DisplayName("aprovado → pede → liberada → reenvia")
        void grantedFlow() {
            Checklist c = submitted();
            c.approve(REVIEWER, null, NOW);

            c.requestChange(SELLER, "Mais 2 diluidores", NOW);
            assertThat(c.getStatus()).isEqualTo(ChecklistStatus.CHANGE_REQUESTED);
            assertThat(c.getChangeReason()).isEqualTo("Mais 2 diluidores");

            c.grantChange(REVIEWER, null, NOW);
            assertThat(c.getStatus()).isEqualTo(ChecklistStatus.REOPENED);

            c.resubmit(SELLER, ChecklistFixtures.valid(), NOW);
            assertThat(c.getStatus()).isEqualTo(ChecklistStatus.SUBMITTED);
            assertThat(c.getVersion()).isEqualTo(2);
            assertThat(c.getChangeReason()).isNull();
        }

        @Test
        @DisplayName("negada volta para onde estava (aprovado continua aprovado)")
        void deniedGoesBack() {
            Checklist c = submitted();
            c.approve(REVIEWER, null, NOW);
            c.requestChange(SELLER, "Mais 2 diluidores", NOW);

            c.denyChange(REVIEWER, "Contrato já assinado", NOW);

            assertThat(c.getStatus()).isEqualTo(ChecklistStatus.APPROVED);
            assertThat(c.getReviewNotes()).isEqualTo("Contrato já assinado");
        }

        @Test
        @DisplayName("pedir exige o motivo, e não se pede duas vezes")
        void reasonAndOnce() {
            Checklist c = submitted();
            assertThatThrownBy(() -> c.requestChange(SELLER, "", NOW)).isInstanceOf(InvalidChecklistException.class);
            assertThat(c.getStatus()).isEqualTo(ChecklistStatus.SUBMITTED);

            c.requestChange(SELLER, "Trocar o e-mail", NOW);
            assertThatThrownBy(() -> c.requestChange(SELLER, "De novo", NOW))
                    .isInstanceOf(ChecklistTransitionException.class)
                    .hasMessageContaining("Já existe um pedido de alteração");
        }

        @Test
        @DisplayName("sem pedido aberto, não há o que liberar ou negar")
        void nothingToAnswer() {
            Checklist c = submitted();
            assertThatThrownBy(() -> c.grantChange(REVIEWER, null, NOW)).isInstanceOf(ChecklistTransitionException.class);
            assertThatThrownBy(() -> c.denyChange(REVIEWER, "não", NOW)).isInstanceOf(ChecklistTransitionException.class);
            assertThat(c.getStatus()).isEqualTo(ChecklistStatus.SUBMITTED);
        }
    }
}
