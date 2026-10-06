package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.DocumentRequestFileRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.EmployeeDocumentRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.EmployeeDocumentTypeRepository;
import com.proautokimium.api.domain.entities.humanResources.EmployeeDocument;
import com.proautokimium.api.domain.entities.humanResources.EmployeeDocumentType;
import com.proautokimium.api.domain.entities.humanResources.Company;
import com.proautokimium.api.domain.entities.humanResources.Department;
import com.proautokimium.api.domain.entities.humanResources.Team;
import org.mockito.ArgumentCaptor;
import com.proautokimium.api.Infrastructure.services.notification.NotificationService;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.enums.NotificationType;
import com.proautokimium.api.Infrastructure.services.storage.EmployeeDocumentStorageService;
import com.proautokimium.api.domain.entities.humanResources.DocumentRequestFile;
import org.springframework.mock.web.MockMultipartFile;
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
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verifyNoInteractions;
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
    @Mock UserRepository userRepository;
    @Mock DocumentRequestFileRepository fileRepository;
    @Mock EmployeeDocumentStorageService storage;
    @Mock EmployeeDocumentRepository employeeDocumentRepository;
    @Mock EmployeeDocumentTypeRepository employeeDocumentTypeRepository;
    @Mock NotificationService notificationService;

    DocumentRequestService service;

    @BeforeEach
    void setUp() {
        // Um relógio parado em AGORA: o serviço sempre vê a mesma hora.
        Clock clock = Clock.fixed(AGORA.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        service = new DocumentRequestService(requestRepository, recipientRepository, clock, employeeRepository, userRepository, fileRepository, storage,
                employeeDocumentRepository, employeeDocumentTypeRepository, notificationService);
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
        // findInvitable: quem pode receber (ativo e com login). "Todos" = esta lista inteira.
        when(employeeRepository.findInvitable()).thenReturn(List.of(funcionario(anaId), funcionario(brunoId)));
        // O save devolve o próprio objeto que recebeu, como o banco faria.
        when(requestRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        // AGE
        DocumentRequest result = service.send(requestId, true, Set.of(), Set.of(), Set.of());

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
                () -> service.send(requestId, true, Set.of(), Set.of(), Set.of()));

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
        when(employeeRepository.findInvitable()).thenReturn(List.of(funcionario(UUID.randomUUID())));

        assertThrows(InvalidStatusTransitionException.class,
                () -> service.send(requestId, true, Set.of(), Set.of(), Set.of()));

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

    // ── responder ──────────────────────────────────────────────────────────
    // O login "ana" não tem vínculo em users (o mock devolve vazio sozinho),
    // então o serviço cai no findByUsername, que o teste controla.

    @Test
    @DisplayName("o dono responde: grava as respostas, a hora e muda para enviado")
    void submitByOwnerSavesAnswers() {
        Employee ana = new Employee();
        DocumentRequestRecipient recipient = DocumentRequestRecipient.create(rascunhoComCampo(), ana, AGORA);
        UUID recipientId = UUID.randomUUID();
        when(recipientRepository.findById(recipientId)).thenReturn(Optional.of(recipient));
        when(employeeRepository.findByUsername("ana")).thenReturn(Optional.of(ana));
        when(recipientRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        DocumentRequestRecipient saved = service.submit(recipientId, "ana", Map.of("rg", "123"));

        assertThat(saved.getStatus()).isEqualTo(RecipientStatus.SUBMITTED);
        assertThat(saved.getAnswers()).containsEntry("rg", "123");
        assertThat(saved.getSubmittedAt()).isEqualTo(AGORA);
        verify(recipientRepository, times(1)).save(recipient);
    }

    @Test
    @DisplayName("outro funcionário tenta responder: 404, como se não existisse, e nada é salvo")
    void submitByAnotherEmployeeRefused() {
        Employee joao = new Employee();
        Employee ana = new Employee();
        DocumentRequestRecipient recipient = DocumentRequestRecipient.create(rascunhoComCampo(), joao, AGORA);
        UUID recipientId = UUID.randomUUID();
        when(recipientRepository.findById(recipientId)).thenReturn(Optional.of(recipient));
        when(employeeRepository.findByUsername("ana")).thenReturn(Optional.of(ana));

        assertThrows(DocumentRequestRecipientNotFoundException.class,
                () -> service.submit(recipientId, "ana", Map.of("rg", "123")));

        assertThat(recipient.getStatus()).isEqualTo(RecipientStatus.PENDING);
        verify(recipientRepository, never()).save(any());
    }

    @Test
    @DisplayName("responder uma resposta que não existe dá 404")
    void submitUnknownRecipient() {
        UUID recipientId = UUID.randomUUID();
        when(recipientRepository.findById(recipientId)).thenReturn(Optional.empty());

        assertThrows(DocumentRequestRecipientNotFoundException.class,
                () -> service.submit(recipientId, "ana", Map.of()));

        verify(recipientRepository, never()).save(any());
    }

    // ── anexar arquivo ─────────────────────────────────────────────────────
    // O disco é falso (mock): o teste confere o que o serviço PEDIU a ele.

    private static final byte[] PDF = "conteudo".getBytes();

    private MockMultipartFile rgPdf() {
        return new MockMultipartFile("file", "rg.pdf", "application/pdf", PDF);
    }

    /** A resposta da Ana, já encontrada pelo repositório. */
    private DocumentRequestRecipient respostaDaAna(Employee ana, UUID recipientId) {
        DocumentRequestRecipient recipient = DocumentRequestRecipient.create(rascunhoComCampo(), ana, AGORA);
        when(recipientRepository.findById(recipientId)).thenReturn(Optional.of(recipient));
        when(employeeRepository.findByUsername("ana")).thenReturn(Optional.of(ana));
        return recipient;
    }

    @Test
    @DisplayName("primeiro envio: grava no disco e no banco, sem substituir nada")
    void uploadFirstFile() throws Exception {
        UUID recipientId = UUID.randomUUID();
        DocumentRequestRecipient recipient = respostaDaAna(new Employee(), recipientId);
        when(fileRepository.findByDocumentRequestRecipientAndFieldKeyAndReplacedAtIsNull(recipient, "rg"))
                .thenReturn(Optional.empty());
        when(storage.save(any(), any(), eq("rg.pdf"))).thenReturn("0042/abc-rg.pdf");
        when(fileRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        DocumentRequestFile saved = service.upload(recipientId, "ana", "rg", rgPdf());

        assertThat(saved.getStoragePath()).isEqualTo("0042/abc-rg.pdf");
        assertThat(saved.getFieldKey()).isEqualTo("rg");
        assertThat(saved.getUploadedAt()).isEqualTo(AGORA);
        verify(fileRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("reenvio: o arquivo atual ganha a data de substituição, gravada antes do novo")
    void uploadReplacesCurrent() throws Exception {
        UUID recipientId = UUID.randomUUID();
        DocumentRequestRecipient recipient = respostaDaAna(new Employee(), recipientId);
        DocumentRequestFile old = DocumentRequestFile.create(recipient, "rg", "rg-borrado.jpg", "0042/old.jpg", AGORA.minusDays(1));
        when(fileRepository.findByDocumentRequestRecipientAndFieldKeyAndReplacedAtIsNull(recipient, "rg"))
                .thenReturn(Optional.of(old));
        when(storage.save(any(), any(), anyString())).thenReturn("0042/abc-rg.pdf");
        when(fileRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        service.upload(recipientId, "ana", "rg", rgPdf());

        assertThat(old.getReplacedAt()).isEqualTo(AGORA);
        verify(fileRepository).saveAndFlush(old);
    }

    @Test
    @DisplayName("campo que o formulário não pede como arquivo: 400, e o disco nem é tocado")
    void uploadUnknownFieldRefused() {
        UUID recipientId = UUID.randomUUID();
        respostaDaAna(new Employee(), recipientId);

        assertThrows(InvalidRequestDataException.class,
                () -> service.upload(recipientId, "ana", "qualquercoisa", rgPdf()));

        verifyNoInteractions(storage);
        verifyNoInteractions(fileRepository);
    }

    @Test
    @DisplayName("arquivo fora de PDF, JPG ou PNG: 400 antes do disco")
    void uploadWrongTypeRefused() {
        UUID recipientId = UUID.randomUUID();
        respostaDaAna(new Employee(), recipientId);
        MockMultipartFile exe = new MockMultipartFile("file", "virus.exe", "application/octet-stream", PDF);

        assertThrows(InvalidRequestDataException.class,
                () -> service.upload(recipientId, "ana", "rg", exe));

        verifyNoInteractions(storage);
    }

    @Test
    @DisplayName("outro funcionário tenta anexar: 404, e o disco nem é tocado")
    void uploadByAnotherEmployeeRefused() {
        UUID recipientId = UUID.randomUUID();
        DocumentRequestRecipient doJoao = DocumentRequestRecipient.create(rascunhoComCampo(), new Employee(), AGORA);
        when(recipientRepository.findById(recipientId)).thenReturn(Optional.of(doJoao));
        when(employeeRepository.findByUsername("ana")).thenReturn(Optional.of(new Employee()));

        assertThrows(DocumentRequestRecipientNotFoundException.class,
                () -> service.upload(recipientId, "ana", "rg", rgPdf()));

        verifyNoInteractions(storage);
    }

    @Test
    @DisplayName("o banco recusa: o arquivo gravado é apagado do disco, e a recusa sobe")
    void uploadRefusedByDatabaseDeletesFile() throws Exception {
        UUID recipientId = UUID.randomUUID();
        DocumentRequestRecipient recipient = respostaDaAna(new Employee(), recipientId);
        when(fileRepository.findByDocumentRequestRecipientAndFieldKeyAndReplacedAtIsNull(recipient, "rg"))
                .thenReturn(Optional.empty());
        when(storage.save(any(), any(), anyString())).thenReturn("0042/abc-rg.pdf");
        when(fileRepository.save(any())).thenThrow(new IllegalStateException("banco fora"));

        assertThrows(IllegalStateException.class,
                () -> service.upload(recipientId, "ana", "rg", rgPdf()));

        verify(storage).delete("0042/abc-rg.pdf");
    }

    // ── aprovar cria o documento do funcionário ────────────────────────────

    private static final UUID TIPO_RG = UUID.randomUUID();

    /** Uma resposta ENVIADA, de uma solicitação com o campo "rg" (com tipo) e "cracha" (sem tipo). */
    private DocumentRequestRecipient respostaEnviada(Employee dono, UUID recipientId) {
        DocumentRequest request = DocumentRequest.draft("Documentos de admissão", "rita", AGORA);
        request.getForm().add(new RequestField("rg", "Foto do RG", null, "FILE", true, List.of(), TIPO_RG));
        request.getForm().add(new RequestField("cracha", "Foto para o crachá", null, "FILE", true, List.of(), null));
        DocumentRequestRecipient recipient = DocumentRequestRecipient.create(request, dono, AGORA);
        recipient.submit(Map.of(), AGORA);
        when(recipientRepository.findById(recipientId)).thenReturn(Optional.of(recipient));
        return recipient;
    }

    @Test
    @DisplayName("aprovar: o arquivo de campo com tipo vira documento do funcionário, com o MESMO caminho")
    void approveCreatesEmployeeDocument() {
        Employee ana = new Employee();
        UUID recipientId = UUID.randomUUID();
        DocumentRequestRecipient recipient = respostaEnviada(ana, recipientId);
        DocumentRequestFile rg = DocumentRequestFile.create(recipient, "rg", "rg.pdf", "0042/abc-rg.pdf", AGORA);
        EmployeeDocumentType tipoRg = EmployeeDocumentType.create("RG", AGORA);
        when(fileRepository.findByDocumentRequestRecipientAndReplacedAtIsNull(recipient)).thenReturn(List.of(rg));
        when(employeeDocumentTypeRepository.findById(TIPO_RG)).thenReturn(Optional.of(tipoRg));
        when(employeeDocumentRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));
        when(recipientRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        service.approve(recipientId, "patricia");

        // ArgumentCaptor: "pegue o objeto que o serviço passou para o save", para olhar dentro.
        ArgumentCaptor<EmployeeDocument> salvo = ArgumentCaptor.forClass(EmployeeDocument.class);
        verify(employeeDocumentRepository).save(salvo.capture());
        EmployeeDocument document = salvo.getValue();
        assertThat(document.getEmployee()).isSameAs(ana);
        assertThat(document.getType()).isSameAs(tipoRg);
        assertThat(document.getTitle()).isEqualTo("Foto do RG");
        assertThat(document.getStoragePath()).isEqualTo("0042/abc-rg.pdf");
        assertThat(document.getContentType()).isEqualTo("application/pdf");
        assertThat(document.getUploadedAt()).isEqualTo(AGORA);
        assertThat(document.getUploadedBy()).isEqualTo("patricia");
        assertThat(rg.getEmployeeDocument()).isSameAs(document);
        verify(fileRepository).save(rg);
    }

    @Test
    @DisplayName("aprovar: campo sem tipo de documento (o crachá) não vira documento")
    void approveSkipsFieldWithoutType() {
        UUID recipientId = UUID.randomUUID();
        DocumentRequestRecipient recipient = respostaEnviada(new Employee(), recipientId);
        DocumentRequestFile cracha = DocumentRequestFile.create(recipient, "cracha", "eu.jpg", "0042/abc-eu.jpg", AGORA);
        when(fileRepository.findByDocumentRequestRecipientAndReplacedAtIsNull(recipient)).thenReturn(List.of(cracha));
        when(recipientRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        service.approve(recipientId, "patricia");

        verifyNoInteractions(employeeDocumentRepository);
        assertThat(cracha.getEmployeeDocument()).isNull();
    }

    @Test
    @DisplayName("aprovar: tipo apagado depois do envio vira documento sem tipo, sem recusar a aprovação")
    void approveWithDeletedTypeStillCreatesDocument() {
        UUID recipientId = UUID.randomUUID();
        DocumentRequestRecipient recipient = respostaEnviada(new Employee(), recipientId);
        DocumentRequestFile rg = DocumentRequestFile.create(recipient, "rg", "rg.png", "0042/abc-rg.png", AGORA);
        when(fileRepository.findByDocumentRequestRecipientAndReplacedAtIsNull(recipient)).thenReturn(List.of(rg));
        when(employeeDocumentTypeRepository.findById(TIPO_RG)).thenReturn(Optional.empty());
        when(employeeDocumentRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));
        when(recipientRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        DocumentRequestRecipient result = service.approve(recipientId, "patricia");

        assertThat(result.getStatus()).isEqualTo(RecipientStatus.APPROVED);
        assertThat(rg.getEmployeeDocument()).isNotNull();
        assertThat(rg.getEmployeeDocument().getType()).isNull();
        assertThat(rg.getEmployeeDocument().getContentType()).isEqualTo("image/png");
    }

    @Test
    @DisplayName("aprovar o que não foi enviado: recusado antes de criar qualquer documento")
    void approveNotSubmittedCreatesNothing() {
        UUID recipientId = UUID.randomUUID();
        DocumentRequestRecipient pendente = DocumentRequestRecipient.create(rascunhoComCampo(), new Employee(), AGORA);
        when(recipientRepository.findById(recipientId)).thenReturn(Optional.of(pendente));

        assertThrows(InvalidStatusTransitionException.class, () -> service.approve(recipientId, "patricia"));

        verifyNoInteractions(employeeDocumentRepository);
        verifyNoInteractions(fileRepository);
    }

    // ── público: quem recebe ───────────────────────────────────────────────
    // O id das entidades é um campo público da classe base; o teste preenche à mão.

    private static Employee funcionario(UUID id) {
        Employee e = new Employee();
        e.id = id;
        return e;
    }

    /** Envia a solicitação para o público dado e devolve os funcionários que viraram destinatário. */
    private List<Employee> enviarPara(List<Employee> elegiveis, Set<UUID> empresas, Set<UUID> setores, Set<UUID> pessoas) {
        UUID requestId = UUID.randomUUID();
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(rascunhoComCampo()));
        when(employeeRepository.findInvitable()).thenReturn(elegiveis);
        when(requestRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        service.send(requestId, false, empresas, setores, pessoas);

        ArgumentCaptor<DocumentRequestRecipient> criados = ArgumentCaptor.forClass(DocumentRequestRecipient.class);
        verify(recipientRepository, org.mockito.Mockito.atLeast(0)).save(criados.capture());
        return criados.getAllValues().stream().map(DocumentRequestRecipient::getEmployee).toList();
    }

    @Test
    @DisplayName("por empresa: entra quem é da empresa; quem não tem empresa fica de fora")
    void audienceByCompany() {
        Company kimium = new Company();
        kimium.id = UUID.randomUUID();
        Employee ana = funcionario(UUID.randomUUID());
        ana.setCompany(kimium);
        Employee semEmpresa = funcionario(UUID.randomUUID());

        List<Employee> recebem = enviarPara(List.of(ana, semEmpresa), Set.of(kimium.id), Set.of(), Set.of());

        assertThat(recebem).containsExactly(ana);
    }

    @Test
    @DisplayName("por setor: vale o setor da EQUIPE; quem não tem equipe fica de fora")
    void audienceByDepartmentThroughTeam() {
        Department financeiro = new Department();
        financeiro.id = UUID.randomUUID();
        Employee bruno = funcionario(UUID.randomUUID());
        bruno.setTeam(new Team("Contas a pagar", financeiro));
        Employee semEquipe = funcionario(UUID.randomUUID());

        List<Employee> recebem = enviarPara(List.of(bruno, semEquipe), Set.of(), Set.of(financeiro.id), Set.of());

        assertThat(recebem).containsExactly(bruno);
    }

    @Test
    @DisplayName("pelo nome, e quem entra por duas portas recebe uma vez só")
    void audienceByNameNoDuplicates() {
        Company kimium = new Company();
        kimium.id = UUID.randomUUID();
        Employee ana = funcionario(UUID.randomUUID());
        ana.setCompany(kimium);
        Employee carla = funcionario(UUID.randomUUID());
        Employee outro = funcionario(UUID.randomUUID());

        List<Employee> recebem = enviarPara(List.of(ana, carla, outro),
                Set.of(kimium.id), Set.of(), Set.of(ana.id, carla.id));

        assertThat(recebem).containsExactlyInAnyOrder(ana, carla);
    }

    @Test
    @DisplayName("\"escolher\" sem marcar nada é recusado, e a solicitação continua rascunho")
    void audienceNothingChosenRefused() {
        UUID requestId = UUID.randomUUID();
        DocumentRequest rascunho = rascunhoComCampo();
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(rascunho));

        assertThrows(InvalidRequestDataException.class,
                () -> service.send(requestId, false, Set.of(), Set.of(), Set.of()));

        assertThat(rascunho.getStatus()).isEqualTo(RequestStatus.DRAFT);
        verify(recipientRepository, never()).save(any());
    }

    @Test
    @DisplayName("público que não resulta em ninguém: recusado, e a solicitação continua rascunho")
    void audienceEmptyResultRefused() {
        UUID requestId = UUID.randomUUID();
        DocumentRequest rascunho = rascunhoComCampo();
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(rascunho));
        when(employeeRepository.findInvitable()).thenReturn(List.of(funcionario(UUID.randomUUID())));

        assertThrows(InvalidRequestDataException.class,
                () -> service.send(requestId, false, Set.of(UUID.randomUUID()), Set.of(), Set.of()));

        assertThat(rascunho.getStatus()).isEqualTo(RequestStatus.DRAFT);
        verify(recipientRepository, never()).save(any());
        verify(requestRepository, never()).save(any());
    }

    // ── avisos (o sino) ────────────────────────────────────────────────────

    private static User usuario(String login) {
        User user = new User();
        user.setLogin(login);
        return user;
    }

    @Test
    @DisplayName("enviar avisa cada destinatário, com o título da solicitação e o link da tela dele")
    void sendNotifiesEachRecipient() {
        UUID requestId = UUID.randomUUID();
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(rascunhoComCampo()));
        when(employeeRepository.findInvitable()).thenReturn(List.of(funcionario(UUID.randomUUID()), funcionario(UUID.randomUUID())));
        when(userRepository.findActiveByEmployeeIds(anyList())).thenReturn(List.of(usuario("ana"), usuario("bruno")));
        when(requestRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        service.send(requestId, true, Set.of(), Set.of(), Set.of());

        verify(notificationService).notify("ana", NotificationType.SOLICITACAO,
                "Nova solicitação do RH", "Envie seu RG", "/documentos/rh/requests");
        verify(notificationService).notify("bruno", NotificationType.SOLICITACAO,
                "Nova solicitação do RH", "Envie seu RG", "/documentos/rh/requests");
    }

    @Test
    @DisplayName("o sino falhar não desfaz o envio: a solicitação abre e é salva")
    void sendSurvivesNotificationFailure() {
        UUID requestId = UUID.randomUUID();
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(rascunhoComCampo()));
        when(employeeRepository.findInvitable()).thenReturn(List.of(funcionario(UUID.randomUUID())));
        when(userRepository.findActiveByEmployeeIds(anyList())).thenReturn(List.of(usuario("ana")));
        when(notificationService.notify(any(), any(), any(), any(), any())).thenThrow(new IllegalStateException("push fora"));
        when(requestRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        DocumentRequest result = service.send(requestId, true, Set.of(), Set.of(), Set.of());

        assertThat(result.getStatus()).isEqualTo(RequestStatus.OPEN);
        verify(requestRepository).save(result);
    }

    @Test
    @DisplayName("aprovar avisa o dono da resposta")
    void approveNotifiesOwner() {
        UUID recipientId = UUID.randomUUID();
        Employee ana = funcionario(UUID.randomUUID());
        respostaEnviada(ana, recipientId);
        when(userRepository.findByEmployee_Id(ana.id)).thenReturn(Optional.of(usuario("ana")));
        when(recipientRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        service.approve(recipientId, "patricia");

        verify(notificationService).notify("ana", NotificationType.SOLICITACAO,
                "Sua resposta foi aprovada", "Documentos de admissão", "/documentos/rh/requests");
    }

    @Test
    @DisplayName("devolver avisa o dono, com o motivo na mensagem")
    void giveBackNotifiesOwnerWithReason() {
        UUID recipientId = UUID.randomUUID();
        Employee ana = funcionario(UUID.randomUUID());
        respostaEnviada(ana, recipientId);
        when(userRepository.findByEmployee_Id(ana.id)).thenReturn(Optional.of(usuario("ana")));
        when(recipientRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        service.giveBack(recipientId, "patricia", "Foto do RG está cortada");

        verify(notificationService).notify("ana", NotificationType.SOLICITACAO,
                "Sua resposta foi devolvida", "Documentos de admissão: Foto do RG está cortada",
                "/documentos/rh/requests");
    }

    @Test
    @DisplayName("devolver sem motivo é recusado, e ninguém é avisado")
    void giveBackRefusedNotifiesNobody() {
        UUID recipientId = UUID.randomUUID();
        Employee ana = funcionario(UUID.randomUUID());
        respostaEnviada(ana, recipientId);
        // A Ana TEM login: sem isto, nenhum aviso sairia nem no código errado, e o teste não provaria nada.
        // lenient: no código certo esta busca nem acontece, e o Mockito reclamaria do "preparo sem uso".
        org.mockito.Mockito.lenient().when(userRepository.findByEmployee_Id(ana.id)).thenReturn(Optional.of(usuario("ana")));

        assertThrows(InvalidRequestDataException.class, () -> service.giveBack(recipientId, "patricia", " "));

        verifyNoInteractions(notificationService);
    }
}
