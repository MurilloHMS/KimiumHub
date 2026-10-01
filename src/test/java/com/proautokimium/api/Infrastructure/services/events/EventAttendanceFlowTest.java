package com.proautokimium.api.Infrastructure.services.events;

import com.proautokimium.api.Application.DTOs.events.EventAttendanceDTOs.AttendanceDTO;
import com.proautokimium.api.Application.DTOs.events.EventAttendanceDTOs.AttendeeDTO;
import com.proautokimium.api.Application.DTOs.events.EventAttendanceDTOs.InvitationAnswerDTO;
import com.proautokimium.api.Infrastructure.exceptions.events.EventExceptions.EventNotFoundException;
import com.proautokimium.api.Infrastructure.exceptions.events.EventExceptions.InvalidEventDataException;
import com.proautokimium.api.Infrastructure.exceptions.events.EventExceptions.InvitationClosedException;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.repositories.events.CompanyEventRepository;
import com.proautokimium.api.Infrastructure.repositories.events.EventReminderSentRepository;
import com.proautokimium.api.Infrastructure.repositories.events.EventResponseRepository;
import com.proautokimium.api.Infrastructure.repositories.events.EventViewRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.CompanyRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.DepartmentRepository;
import com.proautokimium.api.Infrastructure.services.notification.NotificationService;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.entities.events.CompanyEvent;
import com.proautokimium.api.domain.entities.events.EventResponse;
import com.proautokimium.api.domain.entities.events.EventTalk;
import com.proautokimium.api.domain.entities.humanResources.Company;
import com.proautokimium.api.domain.enums.NotificationType;
import com.proautokimium.api.domain.enums.UserRole;
import com.proautokimium.api.domain.enums.events.EventAnswer;
import com.proautokimium.api.domain.valueObjects.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * A confirmação de presença de ponta a ponta no banco (H2): responder, abrir,
 * o Acompanhamento e o lembrete. Só a notificação é dublê.
 *
 * <p>O evento de exemplo é a Poseidon Week, de 06 a 08/10/2026, com a primeira
 * palestra às 08:00 do dia 06 — é aí que as respostas fecham e o lembrete para.
 */
@DataJpaTest
@ActiveProfiles("test")
class EventAttendanceFlowTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");

    @Autowired TestEntityManager em;
    @Autowired CompanyEventRepository events;
    @Autowired EmployeeRepository employees;
    @Autowired UserRepository users;
    @Autowired EventResponseRepository responses;
    @Autowired EventViewRepository views;
    @Autowired EventReminderSentRepository remindersSent;
    @Autowired CompanyRepository companies;
    @Autowired DepartmentRepository departments;
    @Autowired PlatformTransactionManager transactionManager;

    final MutableClock clock = new MutableClock();
    final NotificationService notifications = mock(NotificationService.class);
    EventAttendanceService attendance;
    EventReminderService reminders;

    Company matriz, filial;
    Employee diego, carlos, ana;
    final Map<Employee, String> login = new HashMap<>();
    CompanyEvent poseidon;
    int seq;

    @BeforeEach
    void setUp() {
        attendance = new EventAttendanceService(events, employees, users, responses, views, remindersSent,
                companies, departments, clock);
        reminders = new EventReminderService(events, employees, users, responses, remindersSent, notifications,
                transactionManager, clock);

        matriz = empresa("Matriz", "11.222.333/0001-81");
        filial = empresa("Filial", "11.222.333/0002-62");
        diego = funcionario("Diego", matriz);
        carlos = funcionario("Carlos", matriz);
        ana = funcionario("Ana", filial);

        // Só a Matriz é convidada: Ana fica de fora.
        poseidon = evento(Set.of(matriz));
        clock.set(LocalDateTime.of(2026, 10, 1, 9, 0));
    }

    // ─── Responder ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("responder")
    class Respond {

        @Test
        @DisplayName("mudar a resposta guarda a primeira vez e atualiza a última")
        void changeKeepsTheFirstTime() {
            attendance.respond(poseidon.getId(), login.get(diego), EventAnswer.GOING, "  Vou de carro  ");
            fresh();
            clock.set(LocalDateTime.of(2026, 10, 2, 15, 0));

            InvitationAnswerDTO mudou = attendance.respond(poseidon.getId(), login.get(diego), EventAnswer.NOT_GOING, " ");
            fresh();

            EventResponse r = responses.findByEventIdAndEmployeeId(poseidon.getId(), diego.getId()).orElseThrow();
            assertThat(r.getAnswer()).isEqualTo(EventAnswer.NOT_GOING);
            assertThat(r.getNote()).as("observação em branco vira nula").isNull();
            assertThat(r.getFirstAnsweredAt()).isEqualTo(LocalDateTime.of(2026, 10, 1, 9, 0));
            assertThat(r.getAnsweredAt()).isEqualTo(LocalDateTime.of(2026, 10, 2, 15, 0));
            assertThat(mudou.answer()).isEqualTo(EventAnswer.NOT_GOING);
            assertThat(responses.findByEventId(poseidon.getId())).as("uma linha, não duas").hasSize(1);
        }

        @Test
        @DisplayName("quem não foi convidado leva o mesmo 404 de evento inexistente")
        void notInvitedIsNotFound() {
            assertThatThrownBy(() -> attendance.respond(poseidon.getId(), login.get(ana), EventAnswer.GOING, null))
                    .isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("rascunho é 404 até para quem está no público")
        void draftIsNotFound() {
            poseidon.setPublishedAt(null);
            fresh();

            assertThatThrownBy(() -> attendance.respond(poseidon.getId(), login.get(diego), EventAnswer.GOING, null))
                    .isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("conta sem funcionário (um admin) não tem convite")
        void accountWithoutEmployee() {
            em.persist(new User("admin", "admin@t.com", "hash", List.of(UserRole.ADMIN)));

            assertThatThrownBy(() -> attendance.respond(poseidon.getId(), "admin", EventAnswer.GOING, null))
                    .isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("um minuto antes da primeira palestra ainda dá; na hora exata, fechou")
        void closesWhenTheEventStarts() {
            clock.set(LocalDateTime.of(2026, 10, 6, 7, 59));
            attendance.respond(poseidon.getId(), login.get(diego), EventAnswer.GOING, null);

            clock.set(LocalDateTime.of(2026, 10, 6, 8, 0));
            assertThatThrownBy(() -> attendance.respond(poseidon.getId(), login.get(carlos), EventAnswer.GOING, null))
                    .isInstanceOf(InvitationClosedException.class);
        }

        @Test
        @DisplayName("observação: 500 caracteres cabem, 501 não")
        void noteLimit() {
            attendance.respond(poseidon.getId(), login.get(diego), EventAnswer.GOING, "  " + "a".repeat(500) + "  ");

            assertThatThrownBy(() -> attendance.respond(poseidon.getId(), login.get(carlos), EventAnswer.GOING, "a".repeat(501)))
                    .isInstanceOf(InvalidEventDataException.class);
        }
    }

    // ─── Abrir ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("abrir duas vezes conta duas, e guarda a primeira")
    void viewsCount() {
        attendance.registerView(poseidon.getId(), login.get(diego));
        fresh();
        clock.set(LocalDateTime.of(2026, 10, 2, 10, 0));
        attendance.registerView(poseidon.getId(), login.get(diego));
        fresh();

        var v = views.findByEventIdAndEmployeeId(poseidon.getId(), diego.getId()).orElseThrow();
        assertThat(v.getViewCount()).isEqualTo(2);
        assertThat(v.getFirstViewedAt()).isEqualTo(LocalDateTime.of(2026, 10, 1, 9, 0));
        assertThat(v.getLastViewedAt()).isEqualTo(LocalDateTime.of(2026, 10, 2, 10, 0));
    }

    @Test
    @DisplayName("quem não é convidado não conta visualização")
    void outsiderViewDoesNotCount() {
        assertThatThrownBy(() -> attendance.registerView(poseidon.getId(), login.get(ana)))
                .isInstanceOf(EventNotFoundException.class);
        assertThat(views.findByEventId(poseidon.getId())).isEmpty();
    }

    // ─── Meus convites ───────────────────────────────────────────────────────

    @Test
    @DisplayName("pendente na home: some quando a pessoa responde, até com não vou")
    void pendingDisappearsAfterAnswering() {
        assertThat(attendance.pendingInvitations(login.get(diego))).hasSize(1);
        assertThat(attendance.pendingInvitations(login.get(ana))).as("não convidada").isEmpty();

        attendance.respond(poseidon.getId(), login.get(diego), EventAnswer.NOT_GOING, null);
        fresh();

        assertThat(attendance.pendingInvitations(login.get(diego))).isEmpty();
        assertThat(attendance.myInvitations(login.get(diego))).singleElement()
                .satisfies(i -> assertThat(i.answer().answer()).isEqualTo(EventAnswer.NOT_GOING));
    }

    @Test
    @DisplayName("pendente na home: some quando o evento começa, mesmo sem resposta")
    void pendingDisappearsWhenItStarts() {
        clock.set(LocalDateTime.of(2026, 10, 6, 8, 0));

        assertThat(attendance.pendingInvitations(login.get(diego))).isEmpty();
        assertThat(attendance.myInvitations(login.get(diego))).singleElement()
                .satisfies(i -> assertThat(i.open()).isFalse());
    }

    // ─── Acompanhamento ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Acompanhamento: totais de quem está no público, e quem saiu continua na lista marcado")
    void attendanceTotals() {
        attendance.registerView(poseidon.getId(), login.get(diego));
        attendance.respond(poseidon.getId(), login.get(diego), EventAnswer.GOING, "Vou");

        // Ana respondeu quando a Filial estava no público; depois a Filial saiu.
        poseidon.getAudienceCompanies().add(filial);
        fresh();
        attendance.respond(poseidon.getId(), login.get(ana), EventAnswer.NOT_GOING, "Férias");
        fresh();
        CompanyEvent e = events.findById(poseidon.getId()).orElseThrow();
        e.getAudienceCompanies().removeIf(c -> c.getId().equals(filial.getId()));
        fresh();

        AttendanceDTO a = attendance.attendance(poseidon.getId());

        assertThat(a.invited()).isEqualTo(2);
        assertThat(a.going()).isEqualTo(1);
        assertThat(a.notGoing()).as("Ana não está mais no público").isZero();
        assertThat(a.noAnswer()).isEqualTo(1);
        assertThat(a.neverViewed()).as("Carlos").isEqualTo(1);
        assertThat(a.attendees()).extracting(AttendeeDTO::name, AttendeeDTO::invited)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Carlos", true),
                        org.assertj.core.groups.Tuple.tuple("Diego", true),
                        org.assertj.core.groups.Tuple.tuple("Ana", false));
        AttendeeDTO diegoRow = a.attendees().get(1);
        assertThat(diegoRow.viewCount()).isEqualTo(1);
        assertThat(diegoRow.note()).isEqualTo("Vou");
        assertThat(diegoRow.companyName()).isEqualTo("Matriz");
    }

    // ─── Lembrete ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("lembrete")
    class Reminder {

        @Test
        @DisplayName("na hora marcada, só para quem não respondeu — vai e não vai ficam de fora")
        void onlyTheUnanswered() {
            Employee bruno = funcionario("Bruno", matriz);
            attendance.respond(poseidon.getId(), login.get(diego), EventAnswer.GOING, null);
            attendance.respond(poseidon.getId(), login.get(bruno), EventAnswer.NOT_GOING, null);
            fresh();

            int lembrados = reminders.runReminders();

            assertThat(lembrados).isEqualTo(1);
            verify(notifications).notify(eq(login.get(carlos)), eq(NotificationType.EVENTO),
                    eq("Você ainda não respondeu"), eq("Poseidon Week, de 06 a 08/10. Vai participar?"),
                    eq("/convites?evento=" + poseidon.getId()));
            verify(notifications, times(1)).notify(anyString(), any(), anyString(), anyString(), anyString());
            assertThat(remindersSent.findByEventIdOrderBySentOnAsc(poseidon.getId()))
                    .singleElement().satisfies(s -> assertThat(s.getRecipients()).isEqualTo(1));
        }

        @Test
        @DisplayName("fora da hora marcada, nada")
        void wrongHour() {
            clock.set(LocalDateTime.of(2026, 10, 1, 10, 0));

            assertThat(reminders.runReminders()).isZero();
            verify(notifications, never()).notify(anyString(), any(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("duas rodadas no mesmo dia: um envio só")
        void oncePerDay() {
            reminders.runReminders();
            fresh();
            reminders.runReminders();

            verify(notifications, times(2)).notify(anyString(), any(), anyString(), anyString(), anyString());
            assertThat(remindersSent.findByEventIdOrderBySentOnAsc(poseidon.getId())).hasSize(1);
        }

        @Test
        @DisplayName("no dia seguinte, sai de novo")
        void nextDay() {
            reminders.runReminders();
            fresh();
            clock.set(LocalDateTime.of(2026, 10, 2, 9, 0));
            reminders.runReminders();

            assertThat(remindersSent.findByEventIdOrderBySentOnAsc(poseidon.getId())).hasSize(2);
        }

        @Test
        @DisplayName("no primeiro dia, depois do início, não sai mais")
        void notAfterTheStart() {
            clock.set(LocalDateTime.of(2026, 10, 6, 9, 0));

            assertThat(reminders.runReminders()).isZero();
            assertThat(remindersSent.findAll()).isEmpty();
        }

        @Test
        @DisplayName("o dia fica registrado ANTES de notificar — é o que barra a segunda rodada")
        void recordedBeforeNotifying() {
            doAnswer(inv -> {
                assertThat(remindersSent.existsByEventIdAndSentOn(poseidon.getId(), LocalDate.of(2026, 10, 1)))
                        .as("a linha já existia quando a notificação saiu").isTrue();
                return null;
            }).when(notifications).notify(anyString(), any(), anyString(), anyString(), anyString());

            assertThat(reminders.runReminders()).isEqualTo(2);
        }

        @Test
        @DisplayName("antes da janela de antecedência, nada; no primeiro dia dela, sai")
        void onlyInsideTheWindow() {
            poseidon.setReminderDaysBefore(3);
            fresh();

            // 06/10 menos 3 dias = 03/10. Hoje (01/10) ainda está fora.
            assertThat(reminders.runReminders()).isZero();

            clock.set(LocalDateTime.of(2026, 10, 3, 9, 0));
            assertThat(reminders.runReminders()).isEqualTo(2);
        }

        @Test
        @DisplayName("lembrete desligado, nada")
        void disabled() {
            poseidon.setReminderEnabled(false);
            fresh();

            assertThat(reminders.runReminders()).isZero();
        }
    }

    // ─── Apoio ───────────────────────────────────────────────────────────────

    void fresh() {
        em.flush();
        em.clear();
        poseidon = events.findById(poseidon.getId()).orElseThrow();
    }

    Company empresa(String nome, String cnpj) {
        Company c = new Company();
        c.setName(nome);
        c.setLegalName(nome + " Ltda");
        c.setCnpj(cnpj);
        return em.persist(c);
    }

    Employee funcionario(String nome, Company empresa) {
        seq++;
        Employee e = new Employee();
        e.setName(nome);
        e.setCodParceiro(String.valueOf(5000 + seq));
        e.setAtivo(true);
        e.setEmail(new Email("f" + seq + "@t.com"));
        e.setCompany(empresa);
        em.persist(e);
        User u = new User("login" + seq, "u" + seq + "@t.com", "hash", List.of(UserRole.USER));
        u.setEmployee(e);
        em.persist(u);
        login.put(e, u.getLogin());
        return e;
    }

    CompanyEvent evento(Set<Company> empresas) {
        CompanyEvent e = new CompanyEvent();
        e.setName("Poseidon Week");
        e.setStartDate(LocalDate.of(2026, 10, 6));
        e.setEndDate(LocalDate.of(2026, 10, 8));
        e.setCreatedAt(LocalDateTime.of(2026, 9, 20, 9, 0));
        e.setPublishedAt(LocalDateTime.of(2026, 9, 20, 9, 0));
        e.setAudienceAll(false);
        e.getAudienceCompanies().addAll(empresas);
        e.setReminderEnabled(true);
        e.setReminderTime(LocalTime.of(9, 0));
        e.setReminderDaysBefore(7);

        EventTalk abertura = new EventTalk();
        abertura.setEvent(e);
        abertura.setTitle("Abertura");
        abertura.setDate(LocalDate.of(2026, 10, 6));
        abertura.setStartTime(LocalTime.of(8, 0));
        abertura.setEndTime(LocalTime.of(9, 0));
        e.getTalks().add(abertura);

        em.persist(e);
        em.flush();
        return e;
    }

    /** Um relógio que o teste adianta. */
    static final class MutableClock extends Clock {
        private Instant instant = Instant.EPOCH;

        void set(LocalDateTime now) {
            instant = now.atZone(SP).toInstant();
        }

        @Override public ZoneId getZone() { return SP; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
