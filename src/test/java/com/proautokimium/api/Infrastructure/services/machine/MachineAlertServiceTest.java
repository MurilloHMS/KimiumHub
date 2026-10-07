package com.proautokimium.api.Infrastructure.services.machine;

import com.proautokimium.api.domain.enums.email.EmailOrigin;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.repositories.prostock.MachineAlertConfigRepository;
import com.proautokimium.api.Infrastructure.repositories.prostock.MachineAlertSentRepository;
import com.proautokimium.api.Infrastructure.repositories.prostock.RegisterRepository;
import com.proautokimium.api.Infrastructure.services.email.EmailQueueService;
import com.proautokimium.api.Infrastructure.services.notification.NotificationService;
import com.proautokimium.api.domain.abstractions.Entity;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.entities.prostock.ProductInventory;
import com.proautokimium.api.domain.entities.prostock.machine.MachineAlertConfig;
import com.proautokimium.api.domain.entities.prostock.machine.MachineAlertSent;
import com.proautokimium.api.domain.entities.prostock.machine.MachineRegister;
import com.proautokimium.api.domain.enums.MachineStatus;
import com.proautokimium.api.domain.enums.NotificationType;
import com.proautokimium.api.domain.enums.UserRole;
import com.proautokimium.api.domain.valueObjects.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * O resumo diário da programação: um e-mail por pessoa com tudo, e uma
 * notificação no app para quem tem login.
 *
 * <p>O template é renderizado de verdade (Thymeleaf do classpath): uma
 * expressão errada no HTML quebraria o envio em produção, e um dublê do
 * motor de template não veria.
 */
class MachineAlertServiceTest {

    /** Sexta, 02/10/2026, 08:00 em São Paulo — a hora do envio. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-02T11:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    private static final LocalDate HOJE = LocalDate.of(2026, 10, 2);

    private final MachineAlertConfigRepository configs = mock(MachineAlertConfigRepository.class);
    private final MachineAlertSentRepository sent = mock(MachineAlertSentRepository.class);
    private final RegisterRepository registers = mock(RegisterRepository.class);
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final EmailQueueService emails = mock(EmailQueueService.class);
    private final UserRepository users = mock(UserRepository.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final List<MachineRegister> board = new ArrayList<>();
    private MachineAlertConfig config;
    private MachineAlertService service;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        service = new MachineAlertService(new com.proautokimium.api.Infrastructure.services.email.EmailRenderer(engine), configs, sent, registers, employees, emails, users, notifications, CLOCK);
        ReflectionTestUtils.setField(service, "websiteBaseUrl", "https://portal.proautokimium.com.br");

        config = new MachineAlertConfig();
        config.setActive(true);
        config.setAlertWhenLate(true);
        config.setSendAt(LocalTime.of(8, 0));
        config.setDaysBefore(new ArrayList<>(List.of(1, 3)));
        Employee carmen = employee("carmen@proauto.com.br");
        Employee fabio = employee("fabio@proauto.com.br");
        config.setRecipientEmployeeIds(new ArrayList<>(List.of(carmen.getId(), fabio.getId())));
        when(configs.findAll()).thenReturn(List.of(config));
        when(employees.findAllById(any())).thenReturn(List.of(carmen, fabio));
        when(users.findActiveByEmployeeIds(any())).thenReturn(List.of(user("carmen.lima"), user("fabio.reis")));
        when(registers.findAll()).thenReturn(board);
    }

    @Test
    @DisplayName("três atrasadas e duas próximas viram UM e-mail por pessoa, com tudo, a mais atrasada no topo")
    void oneDigestPerRecipient() {
        machine("Padaria Sol", -2);
        machine("Lava Rápido Estrela", -9);
        machine("Hotel Bourbon", -1);
        machine("Restaurante Serra", 3);
        machine("Mercado Bom Preço", 1);

        assertThat(service.runAlerts(false)).isEqualTo(5);

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emails, times(2)).enqueue(eq(EmailOrigin.MACHINE_ALERT), anyString(), eq("Programação: 3 atrasadas e 2 saídas próximas"), body.capture());
        verify(emails).enqueue(eq(EmailOrigin.MACHINE_ALERT), eq("carmen@proauto.com.br"), anyString(), anyString());
        verify(emails).enqueue(eq(EmailOrigin.MACHINE_ALERT), eq("fabio@proauto.com.br"), anyString(), anyString());

        String html = body.getValue();
        assertThat(html).contains("Atrasadas (3)", "Saídas próximas (2)", "9 dias de atraso", "1 dia de atraso", "amanhã", "em 3 dias");
        assertThat(html.indexOf("Lava Rápido Estrela")).as("a mais atrasada primeiro")
                .isLessThan(html.indexOf("Padaria Sol"));
        assertThat(html.indexOf("Padaria Sol")).isLessThan(html.indexOf("Hotel Bourbon"));
        assertThat(html.indexOf("Mercado Bom Preço")).as("a mais perto primeiro")
                .isLessThan(html.indexOf("Restaurante Serra"));
    }

    @Test
    @DisplayName("os configurados com login recebem uma notificação no app, que abre as atrasadas")
    void inAppNotification() {
        machine("Padaria Sol", -2);
        machine("Mercado Bom Preço", 1);

        service.runAlerts(false);

        for (String login : List.of("carmen.lima", "fabio.reis")) {
            verify(notifications).notify(login, NotificationType.PROGRAMACAO, "Resumo da programação",
                    "1 atrasada · 1 saída próxima", "/stock/programacao?atrasadas=1");
        }
    }

    @Test
    @DisplayName("sem atrasada, a notificação abre a programação sem filtro")
    void linkWithoutLate() {
        machine("Mercado Bom Preço", 1);

        service.runAlerts(false);

        verify(notifications).notify("carmen.lima", NotificationType.PROGRAMACAO, "Resumo da programação",
                "1 saída próxima", "/stock/programacao");
        verify(emails, times(2)).enqueue(eq(EmailOrigin.MACHINE_ALERT), anyString(), eq("Programação: 1 saída próxima"), anyString());
    }

    @Test
    @DisplayName("entregue, sem previsão e fora dos dias configurados não entram")
    void onlyWhatMatters() {
        machine("Entregue Ltda", -5).setStatus(MachineStatus.ENTREGUE);
        machine("Sem Data", 0).setPrevisaoEntrega(null);
        machine("Daqui Dois Dias", 2);
        machine("Padaria Sol", -2);

        assertThat(service.runAlerts(false)).isEqualTo(1);

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emails, times(2)).enqueue(eq(EmailOrigin.MACHINE_ALERT), anyString(), anyString(), body.capture());
        assertThat(body.getValue()).contains("Padaria Sol").doesNotContain("Entregue Ltda", "Sem Data", "Daqui Dois Dias");
    }

    @Test
    @DisplayName("o que já saiu hoje não repete; sem nada novo, nem e-mail nem notificação")
    void nothingNewNothingSent() {
        MachineRegister padaria = machine("Padaria Sol", -2);
        when(sent.alreadySent(eq(padaria.getId()), eq(HOJE), anyInt())).thenReturn(true);

        assertThat(service.runAlerts(false)).isZero();

        verify(emails, never()).enqueue(eq(EmailOrigin.MACHINE_ALERT), anyString(), anyString(), anyString());
        verify(notifications, never()).notify(anyString(), any(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("cada máquina do resumo fica marcada como enviada hoje, com a marca de atraso ou os dias")
    void marksEachMachine() {
        MachineRegister padaria = machine("Padaria Sol", -2);
        MachineRegister mercado = machine("Mercado Bom Preço", 1);

        service.runAlerts(false);

        ArgumentCaptor<MachineAlertSent> marked = ArgumentCaptor.forClass(MachineAlertSent.class);
        verify(sent, times(2)).save(marked.capture());
        assertThat(marked.getAllValues()).extracting(m -> ReflectionTestUtils.getField(m, "registerId"))
                .containsExactlyInAnyOrder(padaria.getId(), mercado.getId());
    }

    @Test
    @DisplayName("fora da hora configurada não envia; desligado não envia")
    void scheduleAndSwitch() {
        machine("Padaria Sol", -2);
        config.setSendAt(LocalTime.of(9, 0));
        assertThat(service.runAlerts(false)).isZero();

        config.setSendAt(LocalTime.of(8, 0));
        config.setActive(false);
        assertThat(service.runAlerts(false)).isZero();
        verify(emails, never()).enqueue(eq(EmailOrigin.MACHINE_ALERT), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("quem não tem e-mail cadastrado ainda recebe no app")
    void appOnly() {
        when(employees.findAllById(any())).thenReturn(List.of(employee(null)));
        machine("Padaria Sol", -2);

        assertThat(service.runAlerts(false)).isEqualTo(1);

        verify(emails, never()).enqueue(eq(EmailOrigin.MACHINE_ALERT), anyString(), anyString(), anyString());
        verify(notifications).notify(eq("carmen.lima"), eq(NotificationType.PROGRAMACAO), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("o e-mail de teste sai no formato do resumo, marcado como teste")
    void sample() {
        assertThat(service.sendSampleAlert()).isEqualTo(2);

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emails, times(2)).enqueue(eq(EmailOrigin.MACHINE_ALERT), anyString(), eq("[TESTE] Programação: 1 atrasada e 1 saída próxima"), body.capture());
        assertThat(body.getValue()).contains("Atrasadas (1)", "Saídas próximas (1)");
    }

    // ─── Montagem ────────────────────────────────────────────────────────────

    private MachineRegister machine(String cliente, int daysFromToday) {
        ProductInventory product = new ProductInventory();
        product.setName("CAPO NT 300");
        MachineRegister r = withId(new MachineRegister(product));
        r.setNomeCliente(cliente);
        r.setStatus(MachineStatus.RESERVADA);
        r.setRegiao("PR");
        r.setConsultor("Carmen");
        r.setPrevisaoEntrega(HOJE.plusDays(daysFromToday).atTime(14, 0));
        board.add(r);
        return r;
    }

    private static Employee employee(String email) {
        Employee e = withId(new Employee());
        if (email != null) e.setEmail(new Email(email));
        return e;
    }

    private static User user(String login) {
        return new User(login, login + "@t.com", "hash", List.of(UserRole.USER));
    }

    private static <T extends Entity> T withId(T entity) {
        try {
            Field id = Entity.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(entity, UUID.randomUUID());
            return entity;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
