package com.proautokimium.api.domain.entities.humanResources;

import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.enums.humanResources.RecipientStatus;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidRequestDataException;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidStatusTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Quem recebe a solicitação: PENDING → SUBMITTED → APPROVED | RETURNED → SUBMITTED.
 */
class DocumentRequestRecipientTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 5, 9, 0);

    private DocumentRequestRecipient recipient;

    /** Roda antes de cada teste: um destinatário novo, recém-criado. */
    @BeforeEach
    void setUp() {
        DocumentRequest request = DocumentRequest.draft("Envie seu RG", "rita", AGORA);
        recipient = DocumentRequestRecipient.create(request, new Employee(), AGORA);
    }

    @Test
    @DisplayName("nasce PENDING, com a data em que recebeu")
    void startsPending() {
        assertThat(recipient.getStatus()).isEqualTo(RecipientStatus.PENDING);
        assertThat(recipient.getAddedAt()).isEqualTo(AGORA);
    }

    @Test
    @DisplayName("responder deixa SUBMITTED e guarda as respostas")
    void submitStoresAnswers() {
        recipient.submit(Map.of("tamanho", "M"), AGORA.plusHours(1));

        assertThat(recipient.getStatus()).isEqualTo(RecipientStatus.SUBMITTED);
        assertThat(recipient.getAnswers()).containsEntry("tamanho", "M");
        assertThat(recipient.getSubmittedAt()).isEqualTo(AGORA.plusHours(1));
    }

    @Test
    @DisplayName("aprovar sem ter respondido é recusado")
    void approveBeforeSubmitRefused() {
        assertThrows(InvalidStatusTransitionException.class, () -> recipient.approve("rita", AGORA));
        assertThat(recipient.getStatus()).isEqualTo(RecipientStatus.PENDING);
    }

    @Test
    @DisplayName("aprovar o que foi respondido grava quem e quando")
    void approveRecordsReviewer() {
        recipient.submit(Map.of("tamanho", "M"), AGORA);

        recipient.approve("rita", AGORA.plusDays(1));

        assertThat(recipient.getStatus()).isEqualTo(RecipientStatus.APPROVED);
        assertThat(recipient.getReviewedBy()).isEqualTo("rita");
        assertThat(recipient.getReviewedAt()).isEqualTo(AGORA.plusDays(1));
    }

    @Test
    @DisplayName("devolver sem motivo é recusado, e continua SUBMITTED")
    void giveBackWithoutReasonRefused() {
        recipient.submit(Map.of("tamanho", "M"), AGORA);

        assertThrows(InvalidRequestDataException.class, () -> recipient.giveBack("rita", "   ", AGORA));
        assertThat(recipient.getStatus()).isEqualTo(RecipientStatus.SUBMITTED);
    }

    @Test
    @DisplayName("devolver o que já foi aprovado é recusado, mesmo com motivo")
    void giveBackAfterApproveRefused() {
        recipient.submit(Map.of("tamanho", "M"), AGORA);
        recipient.approve("rita", AGORA);

        assertThrows(InvalidStatusTransitionException.class, () -> recipient.giveBack("rita", "Foto borrada", AGORA));
        assertThat(recipient.getStatus()).isEqualTo(RecipientStatus.APPROVED);
    }

    @Test
    @DisplayName("devolvido pode responder de novo")
    void returnedCanSubmitAgain() {
        recipient.submit(Map.of("tamanho", "M"), AGORA);
        recipient.giveBack("rita", "Faltou a calça", AGORA);

        recipient.submit(Map.of("tamanho", "M", "calca", "42"), AGORA.plusDays(1));

        assertThat(recipient.getStatus()).isEqualTo(RecipientStatus.SUBMITTED);
        assertThat(recipient.getAnswers()).containsEntry("calca", "42");
    }

    // ── Registrar no lugar do funcionário (V123, "sem acesso") ──

    @Test
    @DisplayName("o RH registra a resposta: fica SUBMITTED, com as respostas e quem registrou")
    void registerOnBehalfRecordsRegistrar() {
        recipient.registerOnBehalf(Map.of("tamanho", "G"), "ana.rh", AGORA.plusHours(2));

        assertThat(recipient.getStatus()).isEqualTo(RecipientStatus.SUBMITTED);
        assertThat(recipient.getAnswers()).containsEntry("tamanho", "G");
        assertThat(recipient.getSubmittedAt()).isEqualTo(AGORA.plusHours(2));
        assertThat(recipient.getRegisteredBy()).isEqualTo("ana.rh");
    }

    @Test
    @DisplayName("registrar o que já foi respondido é recusado, e nada muda")
    void registerOnBehalfAfterSubmitRefused() {
        recipient.submit(Map.of("tamanho", "M"), AGORA);

        assertThrows(InvalidStatusTransitionException.class,
                () -> recipient.registerOnBehalf(Map.of("tamanho", "G"), "ana.rh", AGORA.plusHours(1)));
        assertThat(recipient.getAnswers()).containsEntry("tamanho", "M");
        assertThat(recipient.getRegisteredBy()).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("registrar sem dizer quem registrou é recusado: a linha precisa mostrar quem foi")
    void registerOnBehalfWithoutRegistrarRefused(String registrar) {
        assertThrows(InvalidRequestDataException.class,
                () -> recipient.registerOnBehalf(Map.of("tamanho", "G"), registrar, AGORA));
        assertThat(recipient.getStatus()).isEqualTo(RecipientStatus.PENDING);
    }

    @Test
    @DisplayName("o RH registrou, devolveu, e o funcionário respondeu pelo portal: a resposta passa a ser dele")
    void employeeAnswerClearsRegistrar() {
        recipient.registerOnBehalf(Map.of("tamanho", "G"), "ana.rh", AGORA);
        recipient.giveBack("rita", "Foto do RG ilegível", AGORA.plusHours(1));

        recipient.submit(Map.of("tamanho", "M"), AGORA.plusHours(2));

        assertThat(recipient.getRegisteredBy()).isNull();
    }
}
