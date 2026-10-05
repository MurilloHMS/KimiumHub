package com.proautokimium.api.domain.entities.humanResources;

import com.proautokimium.api.domain.enums.humanResources.RequestStatus;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidRequestDataException;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidStatusTransitionException;
import com.proautokimium.api.domain.valueObjects.humanResources.RequestField;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A solicitação do RH: nasce rascunho, e só é enviada com pelo menos um campo.
 */
class DocumentRequestTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 5, 9, 0);

    private static final RequestField RG =
            new RequestField("rg", "Foto do RG", null, "FILE", true, List.of(), null);

    @Test
    @DisplayName("o rascunho nasce em DRAFT, com o formulário vazio e quem criou")
    void draftStartsEmpty() {
        DocumentRequest request = DocumentRequest.draft("Envie seu RG", "rita", AGORA);

        assertThat(request.getStatus()).isEqualTo(RequestStatus.DRAFT);
        assertThat(request.getTitle()).isEqualTo("Envie seu RG");
        assertThat(request.getForm()).isEmpty();
        assertThat(request.getCreatedBy()).isEqualTo("rita");
        assertThat(request.getCreatedAt()).isEqualTo(AGORA);
        assertThat(request.getSentAt()).isNull();
    }

    // ── Agora é com você. Escreva os 4 testes abaixo, seguindo o modelo de cima. ──

    // 1. título em branco é recusado
    @Test
    @DisplayName("o titulo em branco é recusado.")
    void shouldRejectBlankTitle(){
        assertThrows(InvalidRequestDataException.class,
                () -> DocumentRequest.draft(" ", "rita", AGORA));
    }

    // 2. enviar sem nenhum campo é recusado, e continua rascunho
    @Test
    @DisplayName("enviar sem nenhum campo é recusado, e continua rascunho")
    void shouldRejectSendWithoutFields(){
        DocumentRequest request = DocumentRequest.draft("Envie seu RG", "rita", AGORA);

        assertThrows(InvalidRequestDataException.class,
                () -> request.send(AGORA));
        assertThat(request.getStatus()).isEqualTo(RequestStatus.DRAFT);
    }
    // 3. enviar abre a solicitação e grava quando
    @Test
    @DisplayName("enviar abre a solicitação e grava quando")
    void sendOpens(){
        DocumentRequest request = DocumentRequest.draft("Envie seu RG", "rita", AGORA);
        request.getForm().add(RG);

        request.send(AGORA.plusHours(1));

        assertThat(request.getStatus()).isEqualTo(RequestStatus.OPEN);
        assertThat(request.getSentAt()).isEqualTo(AGORA.plusHours(1));
    }
    // 4. enviar duas vezes é recusado
    @Test
    @DisplayName("enviar duas vezes é recusado")
    void sendTwiceRefused(){
        DocumentRequest request = DocumentRequest.draft("Envie seu RG", "rita", AGORA);
        request.getForm().add(RG);
        request.send(AGORA);

        assertThrows(InvalidStatusTransitionException.class,
                () -> request.send(AGORA));
    }

    // 5. encerrar deve fechar a solicitacao
    @Test
    @DisplayName("deve fechar uma solicitacao")
    void shouldCloseRequest(){
        DocumentRequest request = DocumentRequest.draft("Envie seu RG", "rita", AGORA);
        request.getForm().add(RG);
        request.send(AGORA.plusHours(1));
        request.close(AGORA.plusHours(1));

        assertThat(request.getStatus()).isEqualTo(RequestStatus.CLOSED);
        assertThat(request.getClosedAt()).isEqualTo(AGORA.plusHours(1));
    }

    // 6. deve recusar encerrar um rascunho
    @Test
    @DisplayName("deve recusar fechar um rascunho")
    void closeDraftReject(){
        DocumentRequest request = DocumentRequest.draft("Envie seu RG", "rita", AGORA);

        assertThrows(InvalidStatusTransitionException.class,
                () -> request.close(AGORA));

        assertThat(request.getStatus()).isEqualTo(RequestStatus.DRAFT);
    }

    // 7. deve recusar fechar duas vezes
    @Test
    @DisplayName("deve recusar fechar duas vezes")
    void shouldRejectClosedTwice(){
        DocumentRequest request = DocumentRequest.draft("Envie seu RG", "rita", AGORA);
        request.getForm().add(RG);
        request.send(AGORA.plusHours(1));
        request.close(AGORA.plusHours(1));

        assertThrows(InvalidStatusTransitionException.class,
                () -> request.close(AGORA));
    }
}
