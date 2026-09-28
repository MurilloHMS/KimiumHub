package com.proautokimium.api.Infrastructure.services.humanResources;

import java.util.List;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidRequestDataException;
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
        String login = "emp001.login";

        when(userRepository.findByLoginWithEmployee(login)).thenReturn(Optional.empty());
        when(employeeRepository.findByUsername(login)).thenReturn(Optional.of(employee));
        when(storage.save(any(), eq("EMP001"), eq("nota.jpg"))).thenReturn("EMP001/uuid-nota.jpg");
        when(repository.save(any(Reimbursement.class))).thenAnswer(inv -> inv.getArgument(0));

        ReimbursementResponseDTO response = service.request(
                login, LocalDate.of(2026, 7, 20), new BigDecimal("150.00"), "Restaurante", "Almoço com cliente", receipt
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
        String reviewerLogin = "reviewer.login";

        when(repository.findById(requestId)).thenReturn(Optional.of(reimbursement));
        when(userRepository.findByLoginWithEmployee(reviewerLogin)).thenReturn(Optional.empty());
        when(employeeRepository.findByUsername(reviewerLogin)).thenReturn(Optional.of(reviewer));
        when(repository.save(any(Reimbursement.class))).thenAnswer(inv -> inv.getArgument(0));

        User linkedUser = mock(User.class);
        when(linkedUser.getLogin()).thenReturn("emp001.login");
        when(userRepository.findByEmployee_Id(employeeId)).thenReturn(Optional.of(linkedUser));

        service.approve(requestId, new ReviewReimbursementDTO("Dentro da política"), reviewerLogin);

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

    // ─── O arquivo não fica órfão quando o pedido é recusado ─────────────────
    //
    // O comprovante é salvo ANTES de a entidade validar (ela precisa do
    // caminho). Recusado o pedido, o arquivo ficava no disco sem nenhuma linha
    // no banco apontando para ele — e ninguém o apagaria pela tela.

    @Test
    @DisplayName("pedido recusado apaga o comprovante que acabou de ser salvo")
    void pedidoRecusadoApagaOComprovante() throws Exception {
        MockMultipartFile receipt = new MockMultipartFile("receipt", "nota.jpg", "image/jpeg", "conteudo".getBytes());
        String login = "emp001.login";
        when(userRepository.findByLoginWithEmployee(login)).thenReturn(Optional.empty());
        when(employeeRepository.findByUsername(login)).thenReturn(Optional.of(employee));
        when(storage.save(any(), eq("EMP001"), eq("nota.jpg"))).thenReturn("EMP001/uuid-nota.jpg");

        assertThrows(InvalidRequestDataException.class, () -> service.request(
                login, LocalDate.of(2026, 7, 20), BigDecimal.ZERO, "Restaurante", "Almoço", receipt));

        verify(storage).delete("EMP001/uuid-nota.jpg");
        verify(repository, never()).save(any());
    }

    /**
     * Se apagar também falhar, a pessoa continua vendo o motivo REAL da
     * recusa — o erro do disco vai junto, como suprimido, para o log.
     */
    @Test
    @DisplayName("falha ao apagar não esconde o motivo da recusa")
    void falhaAoApagarNaoEscondeARecusa() throws Exception {
        MockMultipartFile receipt = new MockMultipartFile("receipt", "nota.jpg", "image/jpeg", "conteudo".getBytes());
        String login = "emp001.login";
        when(userRepository.findByLoginWithEmployee(login)).thenReturn(Optional.empty());
        when(employeeRepository.findByUsername(login)).thenReturn(Optional.of(employee));
        when(storage.save(any(), eq("EMP001"), eq("nota.jpg"))).thenReturn("EMP001/uuid-nota.jpg");
        doThrow(new java.io.IOException("disco")).when(storage).delete("EMP001/uuid-nota.jpg");

        InvalidRequestDataException recusa = assertThrows(InvalidRequestDataException.class, () -> service.request(
                login, LocalDate.of(2026, 7, 20), BigDecimal.ZERO, "Restaurante", "Almoço", receipt));

        assertThat(recusa.getSuppressed()).hasSize(1);
        assertThat(recusa.getSuppressed()[0]).hasMessage("disco");
    }

    @Test
    @DisplayName("pedido aceito não apaga nada")
    void pedidoAceitoNaoApaga() throws Exception {
        MockMultipartFile receipt = new MockMultipartFile("receipt", "nota.jpg", "image/jpeg", "conteudo".getBytes());
        String login = "emp001.login";
        when(userRepository.findByLoginWithEmployee(login)).thenReturn(Optional.empty());
        when(employeeRepository.findByUsername(login)).thenReturn(Optional.of(employee));
        when(storage.save(any(), eq("EMP001"), eq("nota.jpg"))).thenReturn("EMP001/uuid-nota.jpg");
        when(repository.save(any(Reimbursement.class))).thenAnswer(inv -> inv.getArgument(0));

        service.request(login, LocalDate.of(2026, 7, 20), new BigDecimal("150.00"), "Restaurante", "Almoço", receipt);

        verify(storage, never()).delete(any());
    }

    // ─── Contestação ─────────────────────────────────────────────────────────

    private Reimbursement recusadoDoEmployee() {
        Reimbursement r = Reimbursement.request(employee, LocalDate.of(2026, 7, 20), new BigDecimal("320.00"),
                "Hospedagem", "Pernoite", "foto.jpg", "EMP001/foto.jpg", LocalDateTime.of(2026, 7, 20, 9, 0));
        r.reject(new Employee(), "Comprovante ilegível", LocalDateTime.of(2026, 7, 21, 9, 0));
        return r;
    }

    private void loginDoDono(String login) {
        when(userRepository.findByLoginWithEmployee(login)).thenReturn(Optional.empty());
        when(employeeRepository.findByUsername(login)).thenReturn(Optional.of(employee));
    }

    @Test
    @DisplayName("o dono contesta: comprovante novo salvo, pedido volta a em análise")
    void donoContesta() throws Exception {
        Reimbursement r = recusadoDoEmployee();
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(r));
        loginDoDono("emp001.login");
        when(storage.save(any(), eq("EMP001"), eq("nota.pdf"))).thenReturn("EMP001/uuid-nota.pdf");
        when(repository.save(any(Reimbursement.class))).thenAnswer(inv -> inv.getArgument(0));

        ReimbursementResponseDTO dto = service.contest(id, "emp001.login", "Segue a nota escaneada",
                new MockMultipartFile("receipt", "nota.pdf", "application/pdf", "%PDF".getBytes()));

        assertThat(dto.status().name()).isEqualTo("PENDING");
        assertThat(dto.contestComment()).isEqualTo("Segue a nota escaneada");
        assertThat(dto.originalReceiptFilename()).isEqualTo("foto.jpg");
        assertThat(dto.firstReviewNotes()).isEqualTo("Comprovante ilegível");
        verify(storage, never()).delete(any());
    }

    /**
     * Quem não é o dono recebe o mesmo 404 de "não existe": 403 confirmaria que
     * aquele id é o reembolso de outra pessoa. E nada é salvo no disco.
     */
    @Test
    @DisplayName("quem não é o dono não contesta, e recebe 404 — não 403")
    void outroNaoContesta() throws Exception {
        Reimbursement r = recusadoDoEmployee();
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(r));
        Employee outro = new Employee();
        java.lang.reflect.Field f = com.proautokimium.api.domain.abstractions.Entity.class.getDeclaredField("id");
        f.setAccessible(true);
        f.set(outro, UUID.randomUUID());
        when(userRepository.findByLoginWithEmployee("outro")).thenReturn(Optional.empty());
        when(employeeRepository.findByUsername("outro")).thenReturn(Optional.of(outro));

        assertThrows(com.proautokimium.api.Infrastructure.exceptions.humanResources.ReimbursementNotFoundException.class,
                () -> service.contest(id, "outro", "x",
                        new MockMultipartFile("receipt", "n.pdf", "application/pdf", "%PDF".getBytes())));
        verify(storage, never()).save(any(), any(), any());
        assertThat(r.getStatus().name()).isEqualTo("REJECTED");
    }

    @Test
    @DisplayName("contestação recusada (sem comentário) apaga o arquivo que acabou de salvar")
    void contestacaoRecusadaApagaArquivo() throws Exception {
        Reimbursement r = recusadoDoEmployee();
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(r));
        loginDoDono("emp001.login");
        when(storage.save(any(), eq("EMP001"), eq("nota.pdf"))).thenReturn("EMP001/uuid-nota.pdf");

        assertThrows(InvalidRequestDataException.class, () -> service.contest(id, "emp001.login", "  ",
                new MockMultipartFile("receipt", "nota.pdf", "application/pdf", "%PDF".getBytes())));

        verify(storage).delete("EMP001/uuid-nota.pdf");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("sem arquivo é recusado antes de salvar qualquer coisa")
    void semArquivo() {
        Reimbursement r = recusadoDoEmployee();
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(r));
        loginDoDono("emp001.login");

        assertThrows(InvalidRequestDataException.class, () -> service.contest(id, "emp001.login", "x", null));
    }

    // ─── Totais do mês ───────────────────────────────────────────────────────

    @Test
    @DisplayName("totais: enviado conta tudo; pendente, a pagar e pago separados; contestados à parte")
    void totaisDoMes() {
        Reimbursement pago = Reimbursement.request(employee, LocalDate.of(2026, 9, 3), new BigDecimal("180.00"),
                "Combustível", "x", "a.jpg", "p/a.jpg", LocalDateTime.of(2026, 9, 3, 9, 0));
        pago.approve(new Employee(), "ok", LocalDateTime.of(2026, 9, 4, 9, 0));
        pago.pay(LocalDate.of(2026, 9, 10), LocalDateTime.of(2026, 9, 10, 9, 0));
        Reimbursement aPagar = Reimbursement.request(employee, LocalDate.of(2026, 9, 5), new BigDecimal("92.00"),
                "Pedágio", "x", "b.jpg", "p/b.jpg", LocalDateTime.of(2026, 9, 5, 9, 0));
        aPagar.approve(new Employee(), "ok", LocalDateTime.of(2026, 9, 6, 9, 0));
        Reimbursement pendente = Reimbursement.request(employee, LocalDate.of(2026, 9, 17), new BigDecimal("96.50"),
                "Alimentação", "x", "c.jpg", "p/c.jpg", LocalDateTime.of(2026, 9, 17, 9, 0));
        Reimbursement contestado = Reimbursement.request(employee, LocalDate.of(2026, 9, 3), new BigDecimal("320.00"),
                "Hospedagem", "x", "d.jpg", "p/d.jpg", LocalDateTime.of(2026, 9, 3, 9, 0));
        contestado.reject(new Employee(), "ilegível", LocalDateTime.of(2026, 9, 5, 9, 0));
        contestado.contest("e.pdf", "p/e.pdf", "nova", LocalDateTime.of(2026, 9, 8, 9, 0));
        Reimbursement recusado = Reimbursement.request(employee, LocalDate.of(2026, 9, 20), new BigDecimal("50.00"),
                "Outros", "x", "f.jpg", "p/f.jpg", LocalDateTime.of(2026, 9, 20, 9, 0));
        recusado.reject(new Employee(), "sem nota", LocalDateTime.of(2026, 9, 21, 9, 0));

        when(repository.findByExpenseDateBetween(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(List.of(pago, aPagar, pendente, contestado, recusado));

        var t = service.summary(java.time.YearMonth.of(2026, 9));

        assertThat(t.month()).isEqualTo("2026-09");
        assertThat(t.sent().count()).as("enviado inclui o recusado").isEqualTo(5);
        assertThat(t.sent().amount()).isEqualByComparingTo("738.50");
        assertThat(t.pending().count()).isEqualTo(2);
        assertThat(t.pending().amount()).isEqualByComparingTo("416.50");
        assertThat(t.contestedPending()).isEqualTo(1);
        assertThat(t.approved().amount()).as("aprovado é o que falta pagar").isEqualByComparingTo("92.00");
        assertThat(t.paid().amount()).isEqualByComparingTo("180.00");
    }
}
