package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.EmployeeDocumentAlertSentRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.EmployeeDocumentRepository;
import com.proautokimium.api.Infrastructure.services.email.EmailQueueService;
import com.proautokimium.api.Infrastructure.services.notification.NotificationService;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.entities.humanResources.EmployeeDocument;
import com.proautokimium.api.domain.entities.humanResources.EmployeeDocumentAlertSent;
import com.proautokimium.api.domain.entities.humanResources.EmployeeDocumentType;
import com.proautokimium.api.domain.enums.NotificationType;
import com.proautokimium.api.domain.valueObjects.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Os avisos de vencimento.
 *
 * O que se protege: o aviso sai no dia CERTO, sai UMA vez, chega a quem é
 * responsável pelo tipo — pelo sino e por e-mail — e o sino leva à tela já
 * filtrada. O e-mail é renderizado com o template de verdade.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeDocumentAlertServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 29);

    @Mock private EmployeeDocumentRepository documentRepository;
    @Mock private EmployeeDocumentAlertSentRepository sentRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private EmailQueueService emailQueueService;

    private EmployeeDocumentAlertService service;

    private Employee ana;
    private Employee responsible;
    private EmployeeDocumentType aso;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        Clock clock = Clock.fixed(TODAY.atTime(8, 0).atZone(ZONE).toInstant(), ZONE);
        service = new EmployeeDocumentAlertService(documentRepository, sentRepository, employeeRepository,
                userRepository, notificationService, emailQueueService, engine, clock);
        service.websiteBaseUrl = "https://www.proautokimium.com.br";

        ana = employee("Ana Souza", null);
        responsible = employee("Diego Martins", "diego@proautokimium.com.br");

        aso = EmployeeDocumentType.create("ASO", LocalDateTime.of(2026, 1, 1, 0, 0));
        aso.id = UUID.randomUUID();
        aso.configureAlerts(List.of(30, 7), true, List.of(responsible.getId()));
    }

    private static Employee employee(String name, String email) {
        Employee employee = new Employee();
        employee.id = UUID.randomUUID();
        employee.setName(name);
        if (email != null) employee.setEmail(new Email(email));
        return employee;
    }

    private EmployeeDocument dueIn(long days) {
        EmployeeDocument document = new EmployeeDocument();
        document.id = UUID.randomUUID();
        document.setEmployee(ana);
        document.setType(aso);
        document.setTitle("ASO periódico");
        document.setDueDate(TODAY.plusDays(days));
        return document;
    }

    // ─── O dia do aviso ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("em que dia o aviso sai")
    class Marker {

        /** Só o dia EXATO: "30, 7" avisa a 30 e a 7, não em todos entre eles. */
        @ParameterizedTest(name = "faltando {0} dias → {1}")
        @CsvSource({
                "30, 30",
                "7, 7",
                "29, ",
                "8, ",
                "31, ",
                "0, -1",
                "-1, ",
        })
        void soNoDiaExato(long daysLeft, Integer expected) {
            assertThat(EmployeeDocumentAlertService.markerFor(dueIn(daysLeft), TODAY)).isEqualTo(expected);
        }

        @Test
        @DisplayName("no dia do vencimento, só se o tipo pedir")
        void noDiaSoSeOTipoPedir() {
            aso.configureAlerts(List.of(30), false, List.of(responsible.getId()));
            assertThat(EmployeeDocumentAlertService.markerFor(dueIn(0), TODAY)).isNull();
        }
    }

    // ─── O envio ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("avisa o responsável pelo sino e por e-mail, e grava o marco")
    void avisaEGrava() {
        EmployeeDocument document = dueIn(30);
        when(documentRepository.findAlertCandidates()).thenReturn(List.of(document));
        when(employeeRepository.findAllById(aso.getRecipientEmployeeIds())).thenReturn(List.of(responsible));
        User user = mock(User.class);
        when(user.getLogin()).thenReturn("diego.login");
        when(userRepository.findByEmployee_Id(responsible.getId())).thenReturn(Optional.of(user));

        int alerted = service.runAlerts();

        assertThat(alerted).isEqualTo(1);

        // O sino abre a tela já no recorte: "vence em breve" da Ana.
        verify(notificationService).notify(eq("diego.login"), eq(NotificationType.DOCUMENTO),
                eq("Vence em 30 dias: ASO periódico"), contains("Ana Souza"),
                eq("/rh/employee-documents?status=EXPIRING&employeeId=" + ana.getId()));

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailQueueService).sendEmail(eq("diego@proautokimium.com.br"), anyString(),
                eq("Vence em 30 dias: ASO periódico — Ana Souza"), body.capture());
        assertThat(body.getValue())
                .contains("Ana Souza").contains("ASO periódico").contains("29/10/2026")
                .contains("https://www.proautokimium.com.br/rh/employee-documents?status=EXPIRING");

        ArgumentCaptor<EmployeeDocumentAlertSent> saved = ArgumentCaptor.forClass(EmployeeDocumentAlertSent.class);
        verify(sentRepository).save(saved.capture());
        assertThat(saved.getValue().getDaysBefore()).isEqualTo(30);
        assertThat(saved.getValue().getDueDate()).isEqualTo(TODAY.plusDays(30));
    }

    /** **A trava.** Rodar de novo no mesmo dia — o botão "rodar agora" — não repete. */
    @Test
    @DisplayName("marco já enviado não avisa de novo")
    void naoRepete() {
        EmployeeDocument document = dueIn(7);
        when(documentRepository.findAlertCandidates()).thenReturn(List.of(document));
        when(sentRepository.alreadySent(document.getId(), document.getDueDate(), 7))
                .thenReturn(true);
        // O responsável existe: sem a trava, o aviso SAIRIA — é isso que o teste
        // precisa enxergar, e não só um mock vazio que não manda nada.
        lenient().when(employeeRepository.findAllById(any())).thenReturn(List.of(responsible));

        assertThat(service.runAlerts()).isZero();

        verifyNoInteractions(notificationService, emailQueueService);
        verify(sentRepository, never()).save(any());
    }

    /**
     * Sem responsável, o marco NÃO é gravado: se o RH puser alguém ainda hoje, o
     * próximo "rodar agora" avisa. Gravar aqui perderia o aviso para sempre.
     */
    @Test
    @DisplayName("sem responsável, não avisa e não grava o marco")
    void semResponsavel() {
        aso.configureAlerts(List.of(30), true, List.of());
        when(documentRepository.findAlertCandidates()).thenReturn(List.of(dueIn(30)));
        when(employeeRepository.findAllById(any())).thenReturn(List.of());

        assertThat(service.runAlerts()).isZero();

        verify(sentRepository, never()).save(any());
        verifyNoInteractions(emailQueueService);
    }

    @Test
    @DisplayName("fora do dia de aviso, nada acontece")
    void foraDoDia() {
        when(documentRepository.findAlertCandidates()).thenReturn(List.of(dueIn(12)));

        assertThat(service.runAlerts()).isZero();

        verifyNoInteractions(notificationService, emailQueueService);
        verify(sentRepository, never()).alreadySent(any(), any(), anyInt());
    }

    /** Responsável sem e-mail cadastrado ainda recebe o sino; o e-mail é pulado. */
    @Test
    @DisplayName("responsável sem e-mail recebe só o sino")
    void semEmail() {
        Employee noEmail = employee("Renata Lima", null);
        aso.configureAlerts(List.of(30), true, List.of(noEmail.getId()));
        when(documentRepository.findAlertCandidates()).thenReturn(List.of(dueIn(30)));
        when(employeeRepository.findAllById(any())).thenReturn(List.of(noEmail));
        User user = mock(User.class);
        when(user.getLogin()).thenReturn("renata.login");
        when(userRepository.findByEmployee_Id(noEmail.getId())).thenReturn(Optional.of(user));

        assertThat(service.runAlerts()).isEqualTo(1);

        verify(notificationService).notify(eq("renata.login"), any(), any(), any(), any());
        verifyNoInteractions(emailQueueService);
    }

    @Test
    @DisplayName("no dia do vencimento, o título diz \"vence hoje\"")
    void venceHoje() {
        when(documentRepository.findAlertCandidates()).thenReturn(List.of(dueIn(0)));
        when(employeeRepository.findAllById(any())).thenReturn(List.of(responsible));

        service.runAlerts();

        verify(emailQueueService).sendEmail(anyString(), anyString(), startsWith("Vence hoje"), anyString());
        ArgumentCaptor<EmployeeDocumentAlertSent> saved = ArgumentCaptor.forClass(EmployeeDocumentAlertSent.class);
        verify(sentRepository).save(saved.capture());
        assertThat(saved.getValue().getDaysBefore()).isEqualTo(EmployeeDocumentAlertSent.ON_DUE_DATE);
    }

    private static String contains(String part) {
        return org.mockito.ArgumentMatchers.contains(part);
    }

    private static String startsWith(String prefix) {
        return org.mockito.ArgumentMatchers.startsWith(prefix);
    }
}
