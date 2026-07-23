package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.EmployeeDocument.EmployeeDocumentResponseDTO;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.EmployeeDocumentRepository;
import com.proautokimium.api.Infrastructure.services.notification.NotificationService;
import com.proautokimium.api.Infrastructure.services.storage.EmployeeDocumentStorageService;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.entities.humanResources.EmployeeDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeDocumentServiceTest {

    @Mock private EmployeeDocumentRepository repository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmployeeDocumentStorageService storage;
    @Mock private NotificationService notificationService;

    private EmployeeDocumentService service;

    private UUID employeeId;
    private Employee employee;
    private EmployeeDocument document;

    @BeforeEach
    void setUp() throws Exception {
        Clock clock = Clock.fixed(LocalDateTime.of(2026, 7, 23, 10, 0).atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        service = new EmployeeDocumentService(repository, employeeRepository, userRepository, storage, notificationService, clock);

        employeeId = UUID.randomUUID();
        employee = new Employee();
        employee.setCodParceiro("EMP001");
        setId(employee, employeeId);

        document = new EmployeeDocument();
        document.setEmployee(employee);
        document.setTitle("Contrato assinado");
    }

    private void setId(com.proautokimium.api.domain.abstractions.Entity entity, UUID id) throws Exception {
        Field field = com.proautokimium.api.domain.abstractions.Entity.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }

    @Test
    @DisplayName("Deve vincular documento, salvar no storage e notificar o funcionário")
    void deveVincularDocumentoESalvarNoStorage() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "contrato.pdf", "application/pdf", "conteudo".getBytes());

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(storage.save(any(), eq("EMP001"), eq("contrato.pdf"))).thenReturn("EMP001/uuid-contrato.pdf");
        when(repository.save(any(EmployeeDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        User linkedUser = mock(User.class);
        when(linkedUser.getLogin()).thenReturn("emp001.login");
        when(userRepository.findByEmployee_Id(employeeId)).thenReturn(Optional.of(linkedUser));

        EmployeeDocumentResponseDTO response = service.vincular(employeeId, "Contrato assinado", file);

        assertThat(response.title()).isEqualTo("Contrato assinado");
        assertThat(response.originalFilename()).isEqualTo("contrato.pdf");
        verify(notificationService).notify(eq("emp001.login"), any(), any(), any(), eq("/documentos"));
    }

    @Test
    @DisplayName("RH sempre pode acessar, mesmo não sendo o dono")
    void rhSempreConsegueAcessar() {
        assertThat(service.podeAcessar(document, "qualquer-login", true)).isTrue();
    }

    @Test
    @DisplayName("O dono do documento consegue acessar")
    void donoConsegueAcessar() {
        when(userRepository.findByLoginWithEmployee("dono.login")).thenReturn(Optional.empty());
        when(employeeRepository.findByUsername("dono.login")).thenReturn(Optional.of(employee));

        assertThat(service.podeAcessar(document, "dono.login", false)).isTrue();
    }

    @Test
    @DisplayName("Funcionário que não é o dono não consegue acessar")
    void terceiroNaoConsegueAcessar() {
        Employee outroFuncionario = new Employee();

        when(userRepository.findByLoginWithEmployee("outro.login")).thenReturn(Optional.empty());
        when(employeeRepository.findByUsername("outro.login")).thenReturn(Optional.of(outroFuncionario));

        assertThat(service.podeAcessar(document, "outro.login", false)).isFalse();
    }
}
