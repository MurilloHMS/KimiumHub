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
import static org.mockito.Mockito.clearInvocations;
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
        // findByAtivoTrue: quem pode receber (todo ativo, com login ou sem). "Todos" = esta lista inteira.
        when(employeeRepository.findByAtivoTrue()).thenReturn(List.of(funcionario(anaId), funcionario(brunoId)));
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

    /** Uma solicitação já enviada: é só nela que alguém responde. Destinatário nunca existe num rascunho. */
    private DocumentRequest abertaComCampo() {
        DocumentRequest request = rascunhoComCampo();
        request.send(AGORA);
        return request;
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
        when(employeeRepository.findByAtivoTrue()).thenReturn(List.of(funcionario(UUID.randomUUID())));

        assertThrows(InvalidStatusTransitionException.class,
                () -> service.send(requestId, true, Set.of(), Set.of(), Set.of()));

        verify(recipientRepository, never()).save(any());
    }

    @Test
    @DisplayName("grava quem aprovou e quando")
    void approveRecordsReviewer(){
        DocumentRequestRecipient recipient = DocumentRequestRecipient.create(abertaComCampo(), new Employee(), AGORA);
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
        DocumentRequestRecipient recipient = DocumentRequestRecipient.create(abertaComCampo(), new Employee(), AGORA);
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
        DocumentRequestRecipient recipient = DocumentRequestRecipient.create(abertaComCampo(), new Employee(), AGORA);
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
        DocumentRequestRecipient recipient = DocumentRequestRecipient.create(abertaComCampo(), ana, AGORA);
        UUID recipientId = UUID.randomUUID();
        when(recipientRepository.findById(recipientId)).thenReturn(Optional.of(recipient));
        when(employeeRepository.findByUsername("ana")).thenReturn(Optional.of(ana));
        when(recipientRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        // O RG (arquivo obrigatório) já subiu; a resposta de texto do campo "rg" é ignorada: campo de arquivo não leva texto.
        when(fileRepository.findByDocumentRequestRecipientAndReplacedAtIsNull(recipient))
                .thenReturn(List.of(DocumentRequestFile.create(recipient, "rg", "rg.pdf", "0042/rg.pdf", AGORA)));

        DocumentRequestRecipient saved = service.submit(recipientId, "ana", Map.of("rg", "123"));

        assertThat(saved.getStatus()).isEqualTo(RecipientStatus.SUBMITTED);
        assertThat(saved.getAnswers()).doesNotContainKey("rg");
        assertThat(saved.getSubmittedAt()).isEqualTo(AGORA);
        verify(recipientRepository, times(1)).save(recipient);
    }

    @Test
    @DisplayName("outro funcionário tenta responder: 404, como se não existisse, e nada é salvo")
    void submitByAnotherEmployeeRefused() {
        Employee joao = new Employee();
        Employee ana = new Employee();
        DocumentRequestRecipient recipient = DocumentRequestRecipient.create(abertaComCampo(), joao, AGORA);
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
        DocumentRequestRecipient recipient = DocumentRequestRecipient.create(abertaComCampo(), ana, AGORA);
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
        DocumentRequestRecipient doJoao = DocumentRequestRecipient.create(abertaComCampo(), new Employee(), AGORA);
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
        DocumentRequestRecipient pendente = DocumentRequestRecipient.create(abertaComCampo(), new Employee(), AGORA);
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
        when(employeeRepository.findByAtivoTrue()).thenReturn(elegiveis);
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
        when(employeeRepository.findByAtivoTrue()).thenReturn(List.of(funcionario(UUID.randomUUID())));

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
        when(employeeRepository.findByAtivoTrue()).thenReturn(List.of(funcionario(UUID.randomUUID()), funcionario(UUID.randomUUID())));
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
        when(employeeRepository.findByAtivoTrue()).thenReturn(List.of(funcionario(UUID.randomUUID())));
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

    // ── passo 6: responder com regras, avisar o RH, leituras e download ─────

    /** Resposta da Ana numa solicitação ABERTA com um texto obrigatório e um arquivo opcional. */
    private DocumentRequestRecipient respostaComTexto(Employee ana, UUID recipientId) {
        DocumentRequest request = DocumentRequest.draft("Uniforme", "rita", AGORA);
        request.updateDraft("Uniforme", null, null, List.of(
                new RequestField("camisa", "Tamanho da camisa", null, "CHOICE", true, List.of("P", "M", "G"), null),
                new RequestField("foto", "Foto", null, "FILE", false, List.of(), null)));
        request.send(AGORA);
        DocumentRequestRecipient recipient = DocumentRequestRecipient.create(request, ana, AGORA);
        when(recipientRepository.findById(recipientId)).thenReturn(Optional.of(recipient));
        when(employeeRepository.findByUsername("ana")).thenReturn(Optional.of(ana));
        return recipient;
    }

    @Test
    @DisplayName("responder sem um campo obrigatório é recusado, e nada é salvo")
    void submitMissingRequiredRefused() {
        UUID recipientId = UUID.randomUUID();
        DocumentRequestRecipient recipient = respostaComTexto(new Employee(), recipientId);

        InvalidRequestDataException e = assertThrows(InvalidRequestDataException.class,
                () -> service.submit(recipientId, "ana", Map.of("camisa", "  ")));

        assertThat(e.getMessage()).contains("Tamanho da camisa");
        assertThat(recipient.getStatus()).isEqualTo(RecipientStatus.PENDING);
        verify(recipientRepository, never()).save(any());
    }

    @Test
    @DisplayName("arquivo obrigatório que não subiu: recusado com o nome do campo")
    void submitMissingRequiredFileRefused() {
        UUID recipientId = UUID.randomUUID();
        DocumentRequestRecipient recipient = DocumentRequestRecipient.create(abertaComCampo(), new Employee(), AGORA);
        when(recipientRepository.findById(recipientId)).thenReturn(Optional.of(recipient));
        when(employeeRepository.findByUsername("ana")).thenReturn(Optional.of(recipient.getEmployee()));
        when(fileRepository.findByDocumentRequestRecipientAndReplacedAtIsNull(recipient)).thenReturn(List.of());

        InvalidRequestDataException e = assertThrows(InvalidRequestDataException.class,
                () -> service.submit(recipientId, "ana", Map.of()));

        assertThat(e.getMessage()).contains("Foto do RG");
        verify(recipientRepository, never()).save(any());
    }

    @Test
    @DisplayName("responder guarda só os campos do formulário: chave inventada é descartada")
    void submitDropsUnknownKeys() {
        UUID recipientId = UUID.randomUUID();
        respostaComTexto(new Employee(), recipientId);
        when(recipientRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        DocumentRequestRecipient saved = service.submit(recipientId, "ana", Map.of("camisa", "M", "salario", "999"));

        assertThat(saved.getAnswers()).containsExactlyEntriesOf(Map.of("camisa", "M"));
    }

    @Test
    @DisplayName("responder avisa quem confere no RH, menos quem respondeu")
    void submitNotifiesReviewersButNotSelf() {
        UUID recipientId = UUID.randomUUID();
        Employee ana = new Employee();
        ana.setName("Ana");
        respostaComTexto(ana, recipientId);
        when(recipientRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));
        when(userRepository.findActiveLoginsAllowed("rh/document-requests", "ALTERAR")).thenReturn(List.of("rita", "ana"));

        service.submit(recipientId, "ana", Map.of("camisa", "M"));

        verify(notificationService).notify("rita", NotificationType.SOLICITACAO, "Ana respondeu", "Uniforme", "/rh/pendencias");
        verify(notificationService, never()).notify(eq("ana"), any(), any(), any(), any());
    }

    @Test
    @DisplayName("solicitação encerrada não aceita resposta nem arquivo")
    void closedRequestRefusesAnswers() {
        UUID recipientId = UUID.randomUUID();
        DocumentRequestRecipient recipient = respostaComTexto(new Employee(), recipientId);
        recipient.getDocumentRequest().close(AGORA);

        assertThrows(InvalidStatusTransitionException.class,
                () -> service.submit(recipientId, "ana", Map.of("camisa", "M")));
        assertThrows(InvalidStatusTransitionException.class,
                () -> service.upload(recipientId, "ana", "foto", rgPdf()));
        verifyNoInteractions(storage);
        verify(recipientRepository, never()).save(any());
    }

    @Test
    @DisplayName("resposta já enviada não aceita arquivo novo: trocaria o que o RH está conferindo")
    void uploadAfterSubmitRefused() {
        UUID recipientId = UUID.randomUUID();
        DocumentRequestRecipient recipient = respostaComTexto(new Employee(), recipientId);
        recipient.submit(Map.of("camisa", "M"), AGORA);

        assertThrows(InvalidStatusTransitionException.class,
                () -> service.upload(recipientId, "ana", "foto", rgPdf()));
        verifyNoInteractions(storage);
    }

    @Test
    @DisplayName("editar e encerrar passam pela regra da entidade e salvam")
    void updateDraftAndClose() {
        UUID requestId = UUID.randomUUID();
        DocumentRequest request = rascunhoComCampo();
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(requestRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        service.updateDraft(requestId, "Admissão", null, null,
                List.of(new RequestField("cpf", "CPF", null, "SHORT_TEXT", true, List.of(), null)));
        assertThat(request.getTitle()).isEqualTo("Admissão");

        assertThrows(InvalidStatusTransitionException.class, () -> service.close(requestId));
        request.send(AGORA);
        service.close(requestId);
        assertThat(request.getStatus()).isEqualTo(RequestStatus.CLOSED);
        assertThat(request.getClosedAt()).isEqualTo(AGORA);
    }

    @Test
    @DisplayName("a lista do RH traz os contadores de cada solicitação, de uma consulta agrupada")
    void listRequestsWithCounts() {
        DocumentRequest aberta = abertaComCampo();
        aberta.id = UUID.randomUUID();
        DocumentRequest rascunho = rascunhoComCampo();
        rascunho.id = UUID.randomUUID();
        when(requestRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(aberta, rascunho));
        when(recipientRepository.countByRequestAndStatus()).thenReturn(List.of(
                contagem(aberta.id, RecipientStatus.PENDING, 3),
                contagem(aberta.id, RecipientStatus.SUBMITTED, 2),
                contagem(aberta.id, RecipientStatus.APPROVED, 1)));

        var lista = service.listRequests();

        assertThat(lista.get(0).counts()).isEqualTo(new com.proautokimium.api.Application.DTOs.humanResources.DocumentRequest.DocumentRequestDTO.Counts(6, 3, 2, 1, 0));
        assertThat(lista.get(1).counts().total()).isZero();
    }

    private static DocumentRequestRecipientRepository.StatusCount contagem(UUID requestId, RecipientStatus status, long total) {
        return new DocumentRequestRecipientRepository.StatusCount() {
            public UUID getRequestId() { return requestId; }
            public RecipientStatus getStatus() { return status; }
            public long getTotal() { return total; }
        };
    }

    @Test
    @DisplayName("\"minhas solicitações\" vêm do login, com os arquivos atuais de cada resposta")
    void listMineFromLogin() {
        Employee ana = funcionario(UUID.randomUUID());
        ana.setName("Ana");
        DocumentRequestRecipient minha = DocumentRequestRecipient.create(abertaComCampo(), ana, AGORA);
        minha.id = UUID.randomUUID();
        DocumentRequestFile rg = DocumentRequestFile.create(minha, "rg", "rg.pdf", "0042/rg.pdf", AGORA);
        when(employeeRepository.findByUsername("ana")).thenReturn(Optional.of(ana));
        when(recipientRepository.findByEmployeeOrderByAddedAtDesc(ana)).thenReturn(List.of(minha));
        when(fileRepository.findByDocumentRequestRecipientInAndReplacedAtIsNull(List.of(minha))).thenReturn(List.of(rg));

        var lista = service.listMine("ana");

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).employeeName()).isEqualTo("Ana");
        assertThat(lista.get(0).requestTitle()).isEqualTo("Envie seu RG");
        assertThat(lista.get(0).files()).extracting(f -> f.fieldKey()).containsExactly("rg");
    }

    @Test
    @DisplayName("baixar arquivo de outra pessoa: 404 para quem não é dono nem confere")
    void readFileOfAnotherEmployeeRefused() {
        UUID fileId = UUID.randomUUID();
        DocumentRequestRecipient doJoao = DocumentRequestRecipient.create(abertaComCampo(), funcionario(UUID.randomUUID()), AGORA);
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(
                DocumentRequestFile.create(doJoao, "rg", "rg.pdf", "0042/rg.pdf", AGORA)));
        when(employeeRepository.findByUsername("ana")).thenReturn(Optional.of(funcionario(UUID.randomUUID())));

        assertThrows(com.proautokimium.api.Infrastructure.exceptions.humanResources.DocumentRequestFileNotFoundException.class,
                () -> service.readFile(fileId, "ana", false));
        verifyNoInteractions(storage);
    }

    @Test
    @DisplayName("quem confere baixa qualquer arquivo; registro sem arquivo no disco vira 404, não 500")
    void readFileAsReviewer(@org.junit.jupiter.api.io.TempDir java.nio.file.Path pasta) throws Exception {
        UUID fileId = UUID.randomUUID();
        DocumentRequestRecipient doJoao = DocumentRequestRecipient.create(abertaComCampo(), funcionario(UUID.randomUUID()), AGORA);
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(
                DocumentRequestFile.create(doJoao, "rg", "rg.pdf", "0042/rg.pdf", AGORA)));
        java.nio.file.Path arquivo = java.nio.file.Files.write(pasta.resolve("rg.pdf"), PDF);
        when(storage.resolve("0042/rg.pdf")).thenReturn(arquivo);

        var content = service.readFile(fileId, "rita", true);

        assertThat(content.bytes()).isEqualTo(PDF);
        assertThat(content.contentType()).isEqualTo("application/pdf");
        verify(employeeRepository, never()).findByUsername(any());

        java.nio.file.Files.delete(arquivo);
        assertThrows(com.proautokimium.api.Infrastructure.exceptions.humanResources.DocumentRequestFileNotFoundException.class,
                () -> service.readFile(fileId, "rita", true));
    }

    // ── extras da v1: acrescentar, lembrar, duplicar, excluir, modelo ───────

    @Test
    @DisplayName("acrescentar gente: quem já recebeu fica de fora, e só os novos são avisados")
    void addRecipientsSkipsWhoAlreadyHas() {
        UUID requestId = UUID.randomUUID();
        DocumentRequest aberta = abertaComCampo();
        Employee ana = funcionario(UUID.randomUUID());
        Employee novato = funcionario(UUID.randomUUID());
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(aberta));
        when(recipientRepository.findByDocumentRequestOrderByAddedAtDesc(aberta))
                .thenReturn(List.of(DocumentRequestRecipient.create(aberta, ana, AGORA)));
        when(employeeRepository.findByAtivoTrue()).thenReturn(List.of(ana, novato));
        when(userRepository.findActiveByEmployeeIds(List.of(novato.id))).thenReturn(List.of(usuario("novato")));

        int added = service.addRecipients(requestId, true, Set.of(), Set.of(), Set.of());

        assertThat(added).isEqualTo(1);
        ArgumentCaptor<DocumentRequestRecipient> criado = ArgumentCaptor.forClass(DocumentRequestRecipient.class);
        verify(recipientRepository).save(criado.capture());
        assertThat(criado.getValue().getEmployee()).isSameAs(novato);
        verify(notificationService).notify(eq("novato"), eq(NotificationType.SOLICITACAO), any(), any(), any());
    }

    @Test
    @DisplayName("acrescentar quando todos já receberam: recusado com o motivo; e rascunho não recebe gente")
    void addRecipientsRefusals() {
        UUID requestId = UUID.randomUUID();
        DocumentRequest aberta = abertaComCampo();
        Employee ana = funcionario(UUID.randomUUID());
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(aberta));
        when(recipientRepository.findByDocumentRequestOrderByAddedAtDesc(aberta))
                .thenReturn(List.of(DocumentRequestRecipient.create(aberta, ana, AGORA)));
        when(employeeRepository.findByAtivoTrue()).thenReturn(List.of(ana));

        assertThrows(InvalidRequestDataException.class,
                () -> service.addRecipients(requestId, true, Set.of(), Set.of(), Set.of()));

        UUID draftId = UUID.randomUUID();
        when(requestRepository.findById(draftId)).thenReturn(Optional.of(rascunhoComCampo()));
        assertThrows(InvalidStatusTransitionException.class,
                () -> service.addRecipients(draftId, true, Set.of(), Set.of(), Set.of()));
        verify(recipientRepository, never()).save(any());
    }

    @Test
    @DisplayName("lembrar: avisa a pendente e a devolvida; quem já enviou ou foi aprovado não")
    void remindPendingAndReturned() {
        UUID requestId = UUID.randomUUID();
        DocumentRequest aberta = abertaComCampo();
        Employee pendente = funcionario(UUID.randomUUID());
        Employee devolvida = funcionario(UUID.randomUUID());
        Employee enviou = funcionario(UUID.randomUUID());
        DocumentRequestRecipient rDevolvida = DocumentRequestRecipient.create(aberta, devolvida, AGORA);
        rDevolvida.submit(Map.of(), AGORA);
        rDevolvida.giveBack("rita", "Foto cortada", AGORA);
        DocumentRequestRecipient rEnviou = DocumentRequestRecipient.create(aberta, enviou, AGORA);
        rEnviou.submit(Map.of(), AGORA);
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(aberta));
        when(recipientRepository.findByDocumentRequestOrderByAddedAtDesc(aberta)).thenReturn(List.of(
                DocumentRequestRecipient.create(aberta, pendente, AGORA), rDevolvida, rEnviou));
        when(userRepository.findActiveByEmployeeIds(anyList())).thenReturn(List.of(usuario("a"), usuario("b")));

        int reminded = service.remindPending(requestId);

        assertThat(reminded).isEqualTo(2);
        ArgumentCaptor<List<UUID>> ids = ArgumentCaptor.forClass(List.class);
        verify(userRepository).findActiveByEmployeeIds(ids.capture());
        assertThat(ids.getValue()).containsExactlyInAnyOrder(pendente.id, devolvida.id);
    }

    @Test
    @DisplayName("duplicar cria rascunho com o mesmo formulário, sem prazo e sem modelo")
    void duplicateCopiesForm() {
        UUID requestId = UUID.randomUUID();
        DocumentRequest original = DocumentRequest.draft("Uniforme", "rita", AGORA);
        original.updateDraft("Uniforme", "Escolha o tamanho", java.time.LocalDate.of(2026, 10, 20),
                List.of(new RequestField("camisa", "Camisa", null, "CHOICE", true, List.of("P", "M"), null)));
        original.attachTemplate("modelo.pdf", "solicitacoes-modelos/abc-modelo.pdf");
        original.send(AGORA);
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(original));
        when(requestRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        DocumentRequest copy = service.duplicate(requestId, "patricia");

        assertThat(copy.getStatus()).isEqualTo(RequestStatus.DRAFT);
        assertThat(copy.getTitle()).isEqualTo("Cópia de Uniforme");
        assertThat(copy.getCreatedBy()).isEqualTo("patricia");
        assertThat(copy.getInstructions()).isEqualTo("Escolha o tamanho");
        assertThat(copy.getForm()).isEqualTo(original.getForm()).isNotSameAs(original.getForm());
        assertThat(copy.getDueDate()).isNull();
        assertThat(copy.getTemplatePath()).isNull();
    }

    @Test
    @DisplayName("excluir: só rascunho, e o modelo sai do disco junto")
    void deleteDraftOnly() throws Exception {
        UUID draftId = UUID.randomUUID();
        DocumentRequest rascunho = rascunhoComCampo();
        rascunho.attachTemplate("modelo.pdf", "solicitacoes-modelos/abc-modelo.pdf");
        when(requestRepository.findById(draftId)).thenReturn(Optional.of(rascunho));

        service.deleteDraft(draftId);

        verify(requestRepository).delete(rascunho);
        verify(storage).delete("solicitacoes-modelos/abc-modelo.pdf");

        UUID openId = UUID.randomUUID();
        when(requestRepository.findById(openId)).thenReturn(Optional.of(abertaComCampo()));
        assertThrows(InvalidStatusTransitionException.class, () -> service.deleteDraft(openId));
    }

    @Test
    @DisplayName("trocar o modelo apaga o anterior do disco; depois do envio, recusado antes do disco")
    void uploadTemplateReplacesPrevious() throws Exception {
        UUID draftId = UUID.randomUUID();
        DocumentRequest rascunho = rascunhoComCampo();
        rascunho.attachTemplate("velho.pdf", "solicitacoes-modelos/velho.pdf");
        when(requestRepository.findById(draftId)).thenReturn(Optional.of(rascunho));
        when(storage.save(any(), eq("solicitacoes-modelos"), eq("rg.pdf"))).thenReturn("solicitacoes-modelos/novo-rg.pdf");

        service.uploadTemplate(draftId, rgPdf());

        assertThat(rascunho.getTemplatePath()).isEqualTo("solicitacoes-modelos/novo-rg.pdf");
        verify(storage).delete("solicitacoes-modelos/velho.pdf");

        UUID openId = UUID.randomUUID();
        when(requestRepository.findById(openId)).thenReturn(Optional.of(abertaComCampo()));
        clearInvocations(storage);
        assertThrows(InvalidStatusTransitionException.class, () -> service.uploadTemplate(openId, rgPdf()));
        verifyNoInteractions(storage);
    }

    @Test
    @DisplayName("baixar o modelo de uma solicitação que a pessoa não recebeu: 404")
    void readTemplateOnlyForRecipients() {
        UUID requestId = UUID.randomUUID();
        // COM modelo: sem ele o 404 viria da falta do arquivo, e o teste não provaria a regra do "recebeu".
        DocumentRequest aberta = rascunhoComCampo();
        aberta.attachTemplate("contrato.pdf", "solicitacoes-modelos/contrato.pdf");
        aberta.send(AGORA);
        aberta.id = requestId;
        Employee ana = funcionario(UUID.randomUUID());
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(aberta));
        when(employeeRepository.findByUsername("ana")).thenReturn(Optional.of(ana));
        when(recipientRepository.findByEmployeeOrderByAddedAtDesc(ana)).thenReturn(List.of());

        assertThrows(com.proautokimium.api.Infrastructure.exceptions.humanResources.DocumentRequestFileNotFoundException.class,
                () -> service.readTemplate(requestId, "ana", false));
        verifyNoInteractions(storage);
    }

    // ── Sem acesso (V123): todos os ativos recebem; o RH registra no lugar ──

    private static Employee pessoa(String nome, String codParceiro) {
        Employee e = funcionario(UUID.randomUUID());
        e.setName(nome);
        e.setCodParceiro(codParceiro);
        return e;
    }

    private static User loginDe(Employee e, String login) {
        User u = usuario(login);
        u.setEmployee(e);
        return u;
    }

    /** Uma resposta de quem não tem login, numa solicitação aberta com o campo "rg" (arquivo obrigatório). */
    private DocumentRequestRecipient respostaDe(Employee e, UUID recipientId) {
        DocumentRequestRecipient recipient = DocumentRequestRecipient.create(abertaComCampo(), e, AGORA);
        when(recipientRepository.findById(recipientId)).thenReturn(Optional.of(recipient));
        return recipient;
    }

    @Test
    @DisplayName("quem não tem login também entra no público; o aviso vai só para quem tem")
    void audienceIncludesEmployeesWithoutLogin() {
        UUID requestId = UUID.randomUUID();
        Employee ana = pessoa("Ana", "0001"), bruno = pessoa("Bruno", "0002");
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(rascunhoComCampo()));
        when(employeeRepository.findByAtivoTrue()).thenReturn(List.of(ana, bruno));
        when(userRepository.findActiveByEmployeeIds(any())).thenReturn(List.of(loginDe(ana, "ana")));
        when(requestRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        service.send(requestId, true, Set.of(), Set.of(), Set.of());

        verify(recipientRepository, times(2)).save(any(DocumentRequestRecipient.class));
        verify(notificationService).notify(eq("ana"), any(), any(), any(), any());
        verify(notificationService, times(1)).notify(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("a prévia do envio separa quem recebe pelo portal de quem o RH vai registrar")
    void previewSplitsByAccess() {
        Employee ana = pessoa("Ana", "0001"), bruno = pessoa("Bruno", "0002");
        when(employeeRepository.findByAtivoTrue()).thenReturn(List.of(ana, bruno));
        when(userRepository.findActiveByEmployeeIds(any())).thenReturn(List.of(loginDe(ana, "ana")));

        var preview = service.previewAudience(true, Set.of(), Set.of(), Set.of());

        assertThat(preview.total()).isEqualTo(2);
        assertThat(preview.withAccess()).isEqualTo(1);
        assertThat(preview.withoutAccess()).extracting(p -> p.name()).containsExactly("Bruno");
    }

    @Test
    @DisplayName("o RH registra a resposta de quem não tem login: sem checar dono, e fica quem registrou")
    void registerOnBehalfSkipsOwnerCheck() {
        UUID recipientId = UUID.randomUUID();
        DocumentRequestRecipient recipient = respostaDe(pessoa("Bruno", "0002"), recipientId);
        when(fileRepository.findByDocumentRequestRecipientAndReplacedAtIsNull(recipient))
                .thenReturn(List.of(DocumentRequestFile.create(recipient, "rg", "rg.jpg", "0002/rg.jpg", AGORA)));
        when(recipientRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        service.registerOnBehalf(recipientId, "ana.rh", Map.of(), false);

        assertThat(recipient.getStatus()).isEqualTo(RecipientStatus.SUBMITTED);
        assertThat(recipient.getRegisteredBy()).isEqualTo("ana.rh");
        verify(employeeRepository, never()).findByUsername(any());
    }

    @Test
    @DisplayName("registrar e aprovar: aprova na mesma hora, com o RH como quem conferiu")
    void registerAndApprove() {
        UUID recipientId = UUID.randomUUID();
        DocumentRequestRecipient recipient = respostaDe(pessoa("Bruno", "0002"), recipientId);
        when(fileRepository.findByDocumentRequestRecipientAndReplacedAtIsNull(recipient))
                .thenReturn(List.of(DocumentRequestFile.create(recipient, "rg", "rg.jpg", "0002/rg.jpg", AGORA)));
        when(recipientRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        service.registerOnBehalf(recipientId, "ana.rh", Map.of(), true);

        assertThat(recipient.getStatus()).isEqualTo(RecipientStatus.APPROVED);
        assertThat(recipient.getReviewedBy()).isEqualTo("ana.rh");
        assertThat(recipient.getRegisteredBy()).isEqualTo("ana.rh");
    }

    @Test
    @DisplayName("registrar sem o arquivo obrigatório é recusado, como no portal")
    void registerOnBehalfRequiresFiles() {
        UUID recipientId = UUID.randomUUID();
        DocumentRequestRecipient recipient = respostaDe(pessoa("Bruno", "0002"), recipientId);
        when(fileRepository.findByDocumentRequestRecipientAndReplacedAtIsNull(recipient)).thenReturn(List.of());

        assertThrows(InvalidRequestDataException.class, () -> service.registerOnBehalf(recipientId, "ana.rh", Map.of(), false));
        assertThat(recipient.getStatus()).isEqualTo(RecipientStatus.PENDING);
    }

    @Test
    @DisplayName("o RH anexa o arquivo no lugar da pessoa: vai para a pasta DELA, sem checar dono")
    void uploadOnBehalfUsesEmployeeFolder() throws Exception {
        UUID recipientId = UUID.randomUUID();
        DocumentRequestRecipient recipient = respostaDe(pessoa("Bruno", "0077"), recipientId);
        when(fileRepository.findByDocumentRequestRecipientAndFieldKeyAndReplacedAtIsNull(recipient, "rg")).thenReturn(Optional.empty());
        when(storage.save(any(), eq("0077"), eq("rg.pdf"))).thenReturn("0077/abc-rg.pdf");
        when(fileRepository.save(any())).thenAnswer(chamada -> chamada.getArgument(0));

        DocumentRequestFile saved = service.uploadOnBehalf(recipientId, "rg", rgPdf());

        assertThat(saved.getStoragePath()).isEqualTo("0077/abc-rg.pdf");
        verify(employeeRepository, never()).findByUsername(any());
    }

    @Test
    @DisplayName("a linha diz se a pessoa tem acesso e quem registrou")
    void recipientRowShowsAccessAndRegistrar() {
        UUID requestId = UUID.randomUUID();
        DocumentRequest request = abertaComCampo();
        Employee ana = pessoa("Ana", "0001"), bruno = pessoa("Bruno", "0002");
        DocumentRequestRecipient daAna = DocumentRequestRecipient.create(request, ana, AGORA);
        DocumentRequestRecipient doBruno = DocumentRequestRecipient.create(request, bruno, AGORA);
        doBruno.registerOnBehalf(Map.of(), "ana.rh", AGORA);
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(recipientRepository.findByDocumentRequestOrderByAddedAtDesc(request)).thenReturn(List.of(daAna, doBruno));
        when(userRepository.findActiveByEmployeeIds(any())).thenReturn(List.of(loginDe(ana, "ana")));

        var rows = service.listRecipients(requestId);

        assertThat(rows).extracting(r -> r.employeeName(), r -> r.hasAccess(), r -> r.registeredBy())
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Ana", true, null),
                        org.assertj.core.groups.Tuple.tuple("Bruno", false, "ana.rh"));
    }
}
