package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.Reimbursement.PayReimbursementDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Reimbursement.ReimbursementResponseDTO;
import com.proautokimium.api.Application.DTOs.humanResources.Reimbursement.ReviewReimbursementDTO;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.ReimbursementRepository;
import com.proautokimium.api.Infrastructure.services.notification.NotificationService;
import com.proautokimium.api.Infrastructure.services.storage.ReimbursementStorageService;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.entities.humanResources.Reimbursement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReimbursementServiceTest {

    @Mock private ReimbursementRepository repository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private UserRepository userRepository;
    @Mock private ReimbursementStorageService storage;
    @Mock private NotificationService notificationService;

    private ReimbursementService service;

    private UUID employeeId;
    private Employee employee;

    @BeforeEach
    void setUp() throws Exception {
        Clock clock = Clock.fixed(LocalDateTime.of(2026, 7, 23, 10, 0).atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        service = new ReimbursementService(repository, employeeRepository, userRepository, storage, notificationService, clock);

        employeeId = UUID.randomUUID();
        employee = new Employee();
        employee.setCodParceiro("EMP001");
        setId(employee, employeeId);
    }

    private void setId(com.proautokimium.api.domain.abstractions.Entity entity, UUID id) throws Exception {
        Field field = com.proautokimium.api.domain.abstractions.Entity.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }

    @Test
    @DisplayName("Deve solicitar reembolso, salvar comprovante no storage e ficar PENDING")
    void deveSolicitarReembolso() throws Exception {
        MockMultipartFile receipt = new MockMultipartFile("receipt", "nota.jpg", "image/jpeg", "conteudo".getBytes());

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(storage.save(any(), eq("EMP001"), eq("nota.jpg"))).thenReturn("EMP001/uuid-nota.jpg");
        when(repository.save(any(Reimbursement.class))).thenAnswer(inv -> inv.getArgument(0));

        ReimbursementResponseDTO response = service.request(
                employeeId, LocalDate.of(2026, 7, 20), new BigDecimal("150.00"), "Restaurante", "Almoço com cliente", receipt
        );

        assertThat(response.status().name()).isEqualTo("PENDING");
        assertThat(response.amount()).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("Aprovar notifica o funcionário via NotificationService")
    void aprovarNotificaFuncionario() throws Exception {
        Reimbursement reimbursement = Reimbursement.request(
                employee, LocalDate.of(2026, 7, 20), new BigDecimal("150.00"),
                "Restaurante", "Almoço", "nota.jpg", "EMP001/nota.jpg", LocalDateTime.of(2026, 7, 20, 9, 0)
        );
        UUID requestId = UUID.randomUUID();
        Employee reviewer = new Employee();
        UUID reviewerId = UUID.randomUUID();

        when(repository.findById(requestId)).thenReturn(Optional.of(reimbursement));
        when(employeeRepository.findById(reviewerId)).thenReturn(Optional.of(reviewer));
        when(repository.save(any(Reimbursement.class))).thenAnswer(inv -> inv.getArgument(0));

        User linkedUser = mock(User.class);
        when(linkedUser.getLogin()).thenReturn("emp001.login");
        when(userRepository.findByEmployee_Id(employeeId)).thenReturn(Optional.of(linkedUser));

        service.approve(requestId, new ReviewReimbursementDTO(reviewerId, "Dentro da política"));

        verify(notificationService).notify(eq("emp001.login"), any(), any(), any(), eq("/reembolsos"));
    }

    @Test
    @DisplayName("Pagar um reembolso aprovado muda o status pra PAID com a data informada")
    void pagarMudaStatusParaPago() {
        Reimbursement reimbursement = Reimbursement.request(
                employee, LocalDate.of(2026, 7, 20), new BigDecimal("150.00"),
                "Restaurante", "Almoço", "nota.jpg", "EMP001/nota.jpg", LocalDateTime.of(2026, 7, 20, 9, 0)
        );
        reimbursement.approve(new Employee(), "Ok", LocalDateTime.of(2026, 7, 21, 9, 0));
        UUID requestId = UUID.randomUUID();

        when(repository.findById(requestId)).thenReturn(Optional.of(reimbursement));
        when(repository.save(any(Reimbursement.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findByEmployee_Id(employeeId)).thenReturn(Optional.empty());

        ReimbursementResponseDTO response = service.pay(requestId, new PayReimbursementDTO(LocalDate.of(2026, 8, 5)));

        assertThat(response.status().name()).isEqualTo("PAID");
        assertThat(response.paymentDate()).isEqualTo(LocalDate.of(2026, 8, 5));
    }

    @Test
    @DisplayName("RH sempre acessa; dono acessa; terceiro não acessa")
    void controleDeAcesso() {
        Reimbursement reimbursement = Reimbursement.request(
                employee, LocalDate.of(2026, 7, 20), new BigDecimal("150.00"),
                "Restaurante", "Almoço", "nota.jpg", "EMP001/nota.jpg", LocalDateTime.of(2026, 7, 20, 9, 0)
        );

        assertThat(service.podeAcessar(reimbursement, "qualquer-login", true)).isTrue();

        when(userRepository.findByLoginWithEmployee("dono.login")).thenReturn(Optional.empty());
        when(employeeRepository.findByUsername("dono.login")).thenReturn(Optional.of(employee));
        assertThat(service.podeAcessar(reimbursement, "dono.login", false)).isTrue();

        Employee outro = new Employee();
        when(userRepository.findByLoginWithEmployee("outro.login")).thenReturn(Optional.empty());
        when(employeeRepository.findByUsername("outro.login")).thenReturn(Optional.of(outro));
        assertThat(service.podeAcessar(reimbursement, "outro.login", false)).isFalse();
    }
}
