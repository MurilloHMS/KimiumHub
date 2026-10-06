package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.DocumentRequestRecipientRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.DocumentRequestRepository;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.DocumentRequest;
import com.proautokimium.api.domain.entities.humanResources.DocumentRequestRecipient;
import com.proautokimium.api.domain.enums.humanResources.RecipientStatus;
import com.proautokimium.api.domain.enums.humanResources.RequestStatus;
import com.proautokimium.api.domain.valueObjects.humanResources.RequestField;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.DocumentRequestNotFoundException;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.DocumentRequestRecipientNotFoundException;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidRequestDataException;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidStatusTransitionException;
import com.proautokimium.api.domain.exceptions.partners.EmployeeNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * O serviço das solicitações, sem banco: os repositórios são falsos (mocks).
 * O que se testa é o que o serviço FAZ com eles.
 */
@ExtendWith(MockitoExtension.class)               // liga o Mockito neste teste
class DocumentRequestServiceTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 5, 9, 0);

    // @Mock: o Mockito cria um repositório falso. Ele não faz nada sozinho;
    // só responde o que o teste mandar, e anota tudo que foi chamado nele.
    @Mock DocumentRequestRepository requestRepository;
    @Mock DocumentRequestRecipientRepository recipientRepository;
    @Mock EmployeeRepository employeeRepository;

    DocumentRequestService service;

    @BeforeEach
    void setUp() {
        // Um relógio parado em AGORA: o serviço sempre vê a mesma hora.
        Clock clock = Clock.fixed(AGORA.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        service = new DocumentRequestService(requestRepository, recipientRepository, clock, employeeRepository);
    }

    @Test
    @DisplayName("enviar abre a solicitação e cria um destinatário para cada funcionário")
    void sendCreatesOneRecipientPerEmployee() {
        // PREPARA
        UUID requestId = UUID.randomUUID();
        UUID anaId = UUID.randomUUID();
        UUID brunoId = UUID.randomUUID();
        DocumentRequest request = DocumentRequest.draft("Envie seu RG", "rita", AGORA);
        request.getForm().add(new RequestField("rg", "Foto do RG", null, "FILE", true, List.of(), null));

        // when(...).thenReturn(...): "quando chamarem isto, devolva aquilo".
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(employeeRepository.findById(anaId)).thenReturn(Optional.of(new Employee()));
        when(employeeRepository.findById(brunoId)).thenReturn(Optional.of(new Employee()));
        // O save devolve o próprio objeto que recebeu, como o banco faria.
        when(requestRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        // AGE
        DocumentRequest result = service.send(requestId, List.of(anaId, brunoId));

        // CONFERE
        assertThat(result.getStatus()).isEqualTo(RequestStatus.OPEN);
        assertThat(result.getSentAt()).isEqualTo(AGORA);
        // verify: "confira que o save foi chamado 2 vezes, com qualquer destinatário".
        verify(recipientRepository, times(2)).save(any(DocumentRequestRecipient.class));
    }

    /** Uma solicitação pronta para enviar: rascunho com um campo. */
    private DocumentRequest rascunhoComCampo() {
        DocumentRequest request = DocumentRequest.draft("Envie seu RG", "rita", AGORA);
        request.getForm().add(new RequestField("rg", "Foto do RG", null, "FILE", true, List.of(), null));
        return request;
    }

    @Test
    @DisplayName("criar o rascunho grava quem criou e a hora do relógio")
    void createDraftUsesLoginAndClock() {
        when(requestRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        DocumentRequest result = service.createDraft("Envie seu RG", "rita");

        assertThat(result.getStatus()).isEqualTo(RequestStatus.DRAFT);
        assertThat(result.getCreatedBy()).isEqualTo("rita");
        assertThat(result.getCreatedAt()).isEqualTo(AGORA);
    }

    @Test
    @DisplayName("enviar uma solicitação que não existe dá 404, e nada é gravado")
    void sendUnknownRequest() {
        UUID requestId = UUID.randomUUID();
        when(requestRepository.findById(requestId)).thenReturn(Optional.empty());

        assertThrows(DocumentRequestNotFoundException.class,
                () -> service.send(requestId, List.of(UUID.randomUUID())));

        verify(recipientRepository, never()).save(any());
        verify(requestRepository, never()).save(any());
    }

    /**
     * O 7º funcionário de 10 não existe: o serviço para ali. Os destinatários
     * gravados antes são desfeitos pelo @Transactional no banco de verdade;
     * aqui, o que se confere é que o serviço não continua nem salva a solicitação.
     */
    @Test
    @DisplayName("funcionário que não existe dá erro, e a solicitação não é salva")
    void sendUnknownEmployee() {
        UUID requestId = UUID.randomUUID();
        UUID anaId = UUID.randomUUID();
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(rascunhoComCampo()));
        when(employeeRepository.findById(anaId)).thenReturn(Optional.empty());

        assertThrows(EmployeeNotFoundException.class, () -> service.send(requestId, List.of(anaId)));

        verify(recipientRepository, never()).save(any());
        verify(requestRepository, never()).save(any());
    }

    @Test
    @DisplayName("enviar o que já foi enviado é recusado pela regra da entidade, e nada é gravado")
    void sendTwiceRefused() {
        UUID requestId = UUID.randomUUID();
        DocumentRequest jaEnviada = rascunhoComCampo();
        jaEnviada.send(AGORA);
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(jaEnviada));

        assertThrows(InvalidStatusTransitionException.class,
                () -> service.send(requestId, List.of(UUID.randomUUID())));

        verify(employeeRepository, never()).findById(any());
        verify(recipientRepository, never()).save(any());
    }

    @Test
    @DisplayName("grava quem aprovou e quando")
    void approveRecordsReviewer(){
        DocumentRequestRecipient recipient = DocumentRequestRecipient.create(rascunhoComCampo(), new Employee(), AGORA);
        recipient.submit(Map.of("tamanho", "M"), AGORA);

        UUID recipientId = UUID.randomUUID();
        when(recipientRepository.findById(recipientId)).thenReturn(Optional.of(recipient));

        when(recipientRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        DocumentRequestRecipient result = service.approve(recipientId, "patricia");

        assertThat(result.getStatus()).isEqualTo(RecipientStatus.APPROVED);
        assertThat(result.getReviewedBy()).isEqualTo("patricia");
        assertThat(result.getReviewedAt()).isEqualTo(AGORA);
    }

    @Test
    @DisplayName("aprovar uma resposta que não existe dá 404, e nada é gravado")
    void approveUnknownRecipient() {
        UUID recipientId = UUID.randomUUID();
        when(recipientRepository.findById(recipientId)).thenReturn(Optional.empty());

        assertThrows(DocumentRequestRecipientNotFoundException.class,
                () -> service.approve(recipientId, "patricia"));

        verify(recipientRepository, never()).save(any());
    }

    @Test
    @DisplayName("devolver grava o motivo e deixa RETURNED")
    void giveBackRecordsReason() {
        DocumentRequestRecipient recipient = DocumentRequestRecipient.create(rascunhoComCampo(), new Employee(), AGORA);
        recipient.submit(Map.of("tamanho", "M"), AGORA);
        UUID recipientId = UUID.randomUUID();
        when(recipientRepository.findById(recipientId)).thenReturn(Optional.of(recipient));
        when(recipientRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        DocumentRequestRecipient result = service.giveBack(recipientId, "patricia", "Faltou a calça");

        assertThat(result.getStatus()).isEqualTo(RecipientStatus.RETURNED);
        assertThat(result.getReturnReason()).isEqualTo("Faltou a calça");
        assertThat(result.getReviewedBy()).isEqualTo("patricia");
    }

    @Test
    @DisplayName("devolver sem motivo é recusado pela regra da entidade, e nada é gravado")
    void giveBackWithoutReasonRefused() {
        DocumentRequestRecipient recipient = DocumentRequestRecipient.create(rascunhoComCampo(), new Employee(), AGORA);
        recipient.submit(Map.of("tamanho", "M"), AGORA);
        UUID recipientId = UUID.randomUUID();
        when(recipientRepository.findById(recipientId)).thenReturn(Optional.of(recipient));

        assertThrows(InvalidRequestDataException.class,
                () -> service.giveBack(recipientId, "patricia", "   "));

        verify(recipientRepository, never()).save(any());
    }
}
