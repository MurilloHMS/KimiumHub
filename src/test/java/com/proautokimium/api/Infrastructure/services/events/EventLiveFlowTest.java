package com.proautokimium.api.Infrastructure.services.events;

import com.proautokimium.api.Application.DTOs.events.EventAttendanceDTOs.AttendanceDTO;
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
import com.proautokimium.api.domain.entities.events.EventTalk;
import com.proautokimium.api.domain.entities.humanResources.Company;
import com.proautokimium.api.domain.enums.NotificationType;
import com.proautokimium.api.domain.enums.UserRole;
import com.proautokimium.api.domain.enums.events.EventAnswer;
import com.proautokimium.api.domain.enums.events.EventLocationType;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * As lives de ponta a ponta no banco (H2): responder "Estou ciente", o aviso de
 * publicação e o "Começou agora". Só a notificação é dublê.
 *
 * <p>A live de exemplo é o Alinhamento semanal, quinta 08/10/2026, das 09:00 às
 * 09:40, para a Matriz (Diego e Carlos). Ana, da Filial, fica de fora.
 */
@DataJpaTest
@ActiveProfiles("test")
class EventLiveFlowTest {

    private static final String LIVE_URL = "https://www.youtube.com/live/abc123";

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

    final EventAttendanceFlowTest.MutableClock clock = new EventAttendanceFlowTest.MutableClock();
    final NotificationService notifications = mock(NotificationService.class);
    EventAttendanceService attendance;
    EventReminderService reminders;
    EventAnnouncementService announcements;

    Employee diego, carlos, ana;
    final Map<Employee, String> login = new HashMap<>();
    CompanyEvent live;
    int seq;

    @BeforeEach
    void setUp() {
        attendance = new EventAttendanceService(events, employees, users, responses, views, remindersSent,
                companies, departments, clock);
        reminders = new EventReminderService(events, employees, users, responses, remindersSent, notifications,
                transactionManager, clock);
        announcements = new EventAnnouncementService(events, employees, users, notifications, transactionManager, clock);

        Company matriz = empresa("Matriz", "11.222.333/0001-81");
        Company filial = empresa("Filial", "11.222.333/0002-62");
        diego = funcionario("Diego", matriz);
        carlos = funcionario("Carlos", matriz);
        ana = funcionario("Ana", filial);
        live = live(Set.of(matriz));
        clock.set(LocalDateTime.of(2026, 10, 6, 10, 0));
    }

    // ─── Estou ciente ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Estou ciente")
    class Acknowledge {

        @Test
        @DisplayName("grava a confirmação e conta no Acompanhamento como ciente")
        void acknowledges() {
            attendance.respond(live.getId(), login.get(diego), EventAnswer.ACKNOWLEDGED, null);
            fresh();

            AttendanceDTO a = attendance.attendance(live.getId());
            assertThat(a.online()).isTrue();
            assertThat(a.acknowledged()).isEqualTo(1);
            assertThat(a.noAnswer()).isEqualTo(1);
            assertThat(a.going()).isZero();
        }

        @Test
        @DisplayName("live não aceita Vou / Não vou")
        void liveRefusesPresence() {
            assertThatThrownBy(() -> attendance.respond(live.getId(), login.get(diego), EventAnswer.GOING, null))
                    .isInstanceOf(InvalidEventDataException.class)
                    .hasMessageContaining("Estou ciente");
        }

        @Test
        @DisplayName("evento presencial não aceita Estou ciente")
        void presentialRefusesAcknowledge() {
            live.setLocationType(null);
            live.setOnlineUrl(null);
            live.setStartTime(null);
            live.setEndTime(null);
            fresh();

            assertThatThrownBy(() -> attendance.respond(live.getId(), login.get(diego), EventAnswer.ACKNOWLEDGED, null))
                    .isInstanceOf(InvalidEventDataException.class)
                    .hasMessageContaining("Vou ou Não vou");
        }

        @Test
        @DisplayName("durante a live ainda dá para confirmar; quando ela acaba, fecha")
        void closesWhenTheLiveEnds() {
            clock.set(LocalDateTime.of(2026, 10, 8, 9, 39));
            attendance.respond(live.getId(), login.get(diego), EventAnswer.ACKNOWLEDGED, null);

            clock.set(LocalDateTime.of(2026, 10, 8, 9, 40));
            assertThatThrownBy(() -> attendance.respond(live.getId(), login.get(carlos), EventAnswer.ACKNOWLEDGED, null))
                    .isInstanceOf(InvitationClosedException.class)
                    .hasMessageContaining("transmissão já terminou");
        }

        @Test
        @DisplayName("o convite continua aberto (e pendente na Home) durante a live")
        void stillPendingDuringTheLive() {
            clock.set(LocalDateTime.of(2026, 10, 8, 9, 10));
            assertThat(attendance.pendingInvitations(login.get(diego))).hasSize(1);

            clock.set(LocalDateTime.of(2026, 10, 8, 9, 40));
            assertThat(attendance.pendingInvitations(login.get(diego))).isEmpty();
        }

        @Test
        @DisplayName("quem confirmou sai do lembrete, e o texto pede a confirmação")
        void reminderSkipsWhoAcknowledged() {
            attendance.respond(live.getId(), login.get(diego), EventAnswer.ACKNOWLEDGED, null);
            fresh();
            clock.set(LocalDateTime.of(2026, 10, 7, 8, 0));

            assertThat(reminders.runReminders()).isEqualTo(1);
            verify(notifications).notify(eq(login.get(carlos)), eq(NotificationType.EVENTO),
                    eq("Você ainda não confirmou"), startsWith("Alinhamento semanal, em 08/10. Confirme"), anyString());
            verify(notifications, never()).notify(eq(login.get(diego)), eq(NotificationType.EVENTO),
                    anyString(), anyString(), anyString());
        }
    }

    // ─── Aviso de publicação ─────────────────────────────────────────────────

    @Nested
    @DisplayName("aviso de publicação")
    class Announce {

        @Test
        @DisplayName("avisa todos os convidados uma vez, com o horário e 'online'; a segunda vez não avisa")
        void onlyOnce() {
            assertThat(announcements.announcePublished(live)).isEqualTo(2);
            verify(notifications).notify(eq(login.get(diego)), eq(NotificationType.EVENTO),
                    eq("Novo evento: Alinhamento semanal"), eq("em 08/10, das 09:00 às 09:40, online. Toque para ver."),
                    eq("/convites?evento=" + live.getId()));
            verify(notifications, never()).notify(eq(login.get(ana)), eq(NotificationType.EVENTO),
                    anyString(), anyString(), anyString());
            assertThat(live.getAnnouncedAt()).isEqualTo(LocalDateTime.of(2026, 10, 6, 10, 0));

            clearInvocations(notifications);
            assertThat(announcements.announcePublished(live)).isZero();
            verify(notifications, never()).notify(anyString(), eq(NotificationType.EVENTO), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("com o aviso desligado, publicar não avisa ninguém")
        void switchedOff() {
            live.setAnnounceOnPublish(false);
            assertThat(announcements.announcePublished(live)).isZero();
            assertThat(live.getAnnouncedAt()).isNull();
        }

        @Test
        @DisplayName("rascunho não avisa")
        void draft() {
            live.setPublishedAt(null);
            assertThat(announcements.announcePublished(live)).isZero();
        }
    }

    // ─── Começou agora ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("começou agora")
    class LiveStart {

        @Test
        @DisplayName("um minuto antes, nada; na hora, todos os convidados; rodar de novo, nada")
        void exactlyOnce() {
            clock.set(LocalDateTime.of(2026, 10, 8, 8, 59));
            assertThat(announcements.runLiveStarts()).isZero();

            clock.set(LocalDateTime.of(2026, 10, 8, 9, 0));
            assertThat(announcements.runLiveStarts()).isEqualTo(2);
            verify(notifications, times(2)).notify(anyString(), eq(NotificationType.EVENTO),
                    eq("Começou agora: Alinhamento semanal"), anyString(), eq("/convites?evento=" + live.getId()));

            clock.set(LocalDateTime.of(2026, 10, 8, 9, 1));
            clearInvocations(notifications);
            fresh();
            assertThat(announcements.runLiveStarts()).isZero();
            verify(notifications, never()).notify(anyString(), eq(NotificationType.EVENTO), anyString(), anyString(), anyString());
        }

        /**
         * A API fora do ar às 09:00 e de volta às 09:12: a live ainda está
         * acontecendo, e o aviso ainda serve.
         */
        @Test
        @DisplayName("se a API estava fora na hora, avisa quando volta — desde que a live não tenha acabado")
        void lateButStillLive() {
            clock.set(LocalDateTime.of(2026, 10, 8, 9, 12));
            assertThat(announcements.runLiveStarts()).isEqualTo(2);
        }

        @Test
        @DisplayName("depois do fim, 'começou agora' seria mentira: não avisa")
        void afterTheEnd() {
            clock.set(LocalDateTime.of(2026, 10, 8, 9, 40));
            assertThat(announcements.runLiveStarts()).isZero();
        }

        @Test
        @DisplayName("com o aviso desligado, não avisa")
        void switchedOff() {
            live.setNotifyLiveStart(false);
            fresh();
            clock.set(LocalDateTime.of(2026, 10, 8, 9, 0));
            assertThat(announcements.runLiveStarts()).isZero();
        }
    }

    // ─── Apoio ───────────────────────────────────────────────────────────────

    void fresh() {
        em.flush();
        em.clear();
        live = events.findById(live.getId()).orElseThrow();
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
        e.setCodParceiro(String.valueOf(7000 + seq));
        e.setAtivo(true);
        e.setEmail(new Email("l" + seq + "@t.com"));
        e.setCompany(empresa);
        em.persist(e);
        User u = new User("live" + seq, "lu" + seq + "@t.com", "hash", List.of(UserRole.USER));
        u.setEmployee(e);
        em.persist(u);
        login.put(e, u.getLogin());
        return e;
    }

    CompanyEvent live(Set<Company> empresas) {
        CompanyEvent e = new CompanyEvent();
        e.setName("Alinhamento semanal");
        e.setStartDate(LocalDate.of(2026, 10, 8));
        e.setEndDate(LocalDate.of(2026, 10, 8));
        e.setLocationType(EventLocationType.ONLINE);
        e.setOnlineUrl(LIVE_URL);
        e.setStartTime(LocalTime.of(9, 0));
        e.setEndTime(LocalTime.of(9, 40));
        e.setCreatedAt(LocalDateTime.of(2026, 10, 5, 9, 0));
        e.setPublishedAt(LocalDateTime.of(2026, 10, 5, 9, 0));
        e.setAudienceAll(false);
        e.getAudienceCompanies().addAll(empresas);
        e.setReminderEnabled(true);
        e.setReminderTime(LocalTime.of(8, 0));
        e.setReminderDaysBefore(2);
        e.setNotifyLiveStart(true);

        // Uma palestra mais cedo no mesmo dia: o horário da live tem que vencer.
        EventTalk talk = new EventTalk();
        talk.setEvent(e);
        talk.setTitle("Bastidores");
        talk.setDate(LocalDate.of(2026, 10, 8));
        talk.setStartTime(LocalTime.of(7, 0));
        talk.setEndTime(LocalTime.of(7, 30));
        e.getTalks().add(talk);

        em.persist(e);
        em.flush();
        return e;
    }
}
