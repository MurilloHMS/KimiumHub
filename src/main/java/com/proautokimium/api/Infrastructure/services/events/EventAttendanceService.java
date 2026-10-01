package com.proautokimium.api.Infrastructure.services.events;

import com.proautokimium.api.Application.DTOs.events.EventAttendanceDTOs.AttendanceDTO;
import com.proautokimium.api.Application.DTOs.events.EventAttendanceDTOs.AttendeeDTO;
import com.proautokimium.api.Application.DTOs.events.EventAttendanceDTOs.AudienceOptionsDTO;
import com.proautokimium.api.Application.DTOs.events.EventAttendanceDTOs.InvitationAnswerDTO;
import com.proautokimium.api.Application.DTOs.events.EventAttendanceDTOs.InvitationDTO;
import com.proautokimium.api.Application.DTOs.events.EventAttendanceDTOs.InvitationDetailDTO;
import com.proautokimium.api.Application.DTOs.events.EventAttendanceDTOs.ReminderDayDTO;
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
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.entities.events.CompanyEvent;
import com.proautokimium.api.domain.entities.events.EventResponse;
import com.proautokimium.api.domain.entities.events.EventView;
import com.proautokimium.api.domain.enums.events.EventAnswer;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A confirmação de presença: responder ao convite, contar quem abriu o evento
 * e montar o Acompanhamento.
 *
 * <p><b>Quem não é convidado leva o mesmo 404 de evento inexistente.</b>
 * Responder "você não foi convidado" confirmaria que o evento existe — a mesma
 * regra anti-oráculo do banco de talentos. Rascunho também é 404.
 *
 * <p>Quem é convidado é calculado na hora ({@code EmployeeRepository.findEventInvitees}),
 * nunca copiado: quem for contratado na semana que vem já entra.
 */
@Service
public class EventAttendanceService {

    private final CompanyEventRepository events;
    private final EmployeeRepository employees;
    private final UserRepository users;
    private final EventResponseRepository responses;
    private final EventViewRepository views;
    private final EventReminderSentRepository remindersSent;
    private final CompanyRepository companies;
    private final DepartmentRepository departments;
    private final Clock clock;

    public EventAttendanceService(CompanyEventRepository events, EmployeeRepository employees,
                                  UserRepository users, EventResponseRepository responses,
                                  EventViewRepository views, EventReminderSentRepository remindersSent,
                                  CompanyRepository companies, DepartmentRepository departments,
                                  Clock clock) {
        this.events = events;
        this.employees = employees;
        this.users = users;
        this.responses = responses;
        this.views = views;
        this.remindersSent = remindersSent;
        this.companies = companies;
        this.departments = departments;
        this.clock = clock;
    }

    // ─── Quem foi convidado ──────────────────────────────────────────────────

    /** "Meus convites": todos os eventos publicados em que entro, o mais recente primeiro. */
    @Transactional(readOnly = true)
    public List<InvitationDTO> myInvitations(String login) {
        Optional<Employee> employee = optionalEmployeeOf(login);
        if (employee.isEmpty()) {
            return List.of();
        }
        Map<UUID, EventResponse> answers = answersOf(employee.get());
        LocalDateTime now = LocalDateTime.now(clock);

        return events.findInvitationsFor(employee.get().getId()).stream()
                .map(e -> new InvitationDTO(EventMapper.summary(e), e.startsAt(), now.isBefore(e.startsAt()),
                        InvitationAnswerDTO.from(answers.get(e.getId()))))
                .toList();
    }

    /**
     * Os convites que ainda esperam resposta, para a home: abertos e sem
     * resposta. Quem não é funcionário não tem nenhum.
     */
    @Transactional(readOnly = true)
    public List<InvitationDTO> pendingInvitations(String login) {
        return myInvitations(login).stream()
                .filter(i -> i.open() && i.answer() == null)
                .toList();
    }

    /** O convite aberto: o evento com a programação, e a minha resposta. */
    @Transactional(readOnly = true)
    public InvitationDetailDTO invitation(UUID eventId, String login) {
        Employee employee = employeeOf(login);
        CompanyEvent event = invitedEvent(eventId, employee);
        EventResponse answer = responses.findByEventIdAndEmployeeId(eventId, employee.getId()).orElse(null);
        LocalDateTime now = LocalDateTime.now(clock);
        return new InvitationDetailDTO(EventMapper.detail(event), event.startsAt(),
                now.isBefore(event.startsAt()), InvitationAnswerDTO.from(answer));
    }

    /**
     * Vou / Não vou, com observação. Pode mudar até o evento começar.
     *
     * <p>Mudar a resposta não chama {@code save}: a linha veio do banco dentro
     * desta transação, e o Hibernate grava o que mudou no commit (dirty checking).
     */
    @Transactional
    public InvitationAnswerDTO respond(UUID eventId, String login, EventAnswer answer, String note) {
        if (answer == null) {
            throw new InvalidEventDataException("Escolha se vai ou não vai.");
        }
        Employee employee = employeeOf(login);
        CompanyEvent event = invitedEvent(eventId, employee);
        LocalDateTime now = LocalDateTime.now(clock);

        if (!now.isBefore(event.startsAt())) {
            throw new InvitationClosedException();
        }
        if (note != null && note.strip().length() > EventResponse.NOTE_MAX) {
            throw new InvalidEventDataException("A observação pode ter no máximo 500 caracteres.");
        }

        EventResponse response = responses.findByEventIdAndEmployeeId(eventId, employee.getId())
                .map(existing -> {
                    existing.change(answer, note, now);
                    return existing;
                })
                .orElseGet(() -> responses.save(EventResponse.first(event, employee, answer, note, now)));
        return InvitationAnswerDTO.from(response);
    }

    /**
     * Conta que a pessoa abriu o evento. Só convidado conta: o organizador
     * conferindo o "Ver como fica" não é uma visualização de quem foi chamado.
     */
    @Transactional
    public void registerView(UUID eventId, String login) {
        Employee employee = employeeOf(login);
        CompanyEvent event = invitedEvent(eventId, employee);
        LocalDateTime now = LocalDateTime.now(clock);

        views.findByEventIdAndEmployeeId(eventId, employee.getId())
                .ifPresentOrElse(v -> v.seenAgain(now),
                        () -> views.save(EventView.first(event, employee, now)));
    }

    // ─── Quem organiza ───────────────────────────────────────────────────────

    /**
     * O Acompanhamento: cada convidado com o que viu e o que respondeu.
     *
     * <p>Quem respondeu ou abriu e depois saiu do público continua na lista,
     * marcado: a auditoria não apaga o que a pessoa fez. Mas não entra nos totais.
     */
    @Transactional(readOnly = true)
    public AttendanceDTO attendance(UUID eventId) {
        CompanyEvent event = events.findById(eventId).orElseThrow(EventNotFoundException::new);

        Map<UUID, EventResponse> answerByEmployee = responses.findByEventId(eventId).stream()
                .collect(Collectors.toMap(r -> r.getEmployee().getId(), Function.identity()));
        Map<UUID, EventView> viewByEmployee = views.findByEventId(eventId).stream()
                .collect(Collectors.toMap(v -> v.getEmployee().getId(), Function.identity()));

        Map<UUID, Employee> invitees = new LinkedHashMap<>();
        employees.findEventInvitees(eventId).forEach(e -> invitees.put(e.getId(), e));

        List<AttendeeDTO> rows = new ArrayList<>();
        invitees.values().forEach(e -> rows.add(row(e, true, viewByEmployee.get(e.getId()), answerByEmployee.get(e.getId()))));

        // Quem deixou rastro e não está mais no público.
        Map<UUID, Employee> formerGuests = new LinkedHashMap<>();
        answerByEmployee.values().forEach(r -> formerGuests.putIfAbsent(r.getEmployee().getId(), r.getEmployee()));
        viewByEmployee.values().forEach(v -> formerGuests.putIfAbsent(v.getEmployee().getId(), v.getEmployee()));
        formerGuests.keySet().removeAll(invitees.keySet());
        formerGuests.values().stream()
                .sorted(Comparator.comparing(Employee::getName, String.CASE_INSENSITIVE_ORDER))
                .forEach(e -> rows.add(row(e, false, viewByEmployee.get(e.getId()), answerByEmployee.get(e.getId()))));

        int going = 0, notGoing = 0, noAnswer = 0, neverViewed = 0;
        for (AttendeeDTO r : rows) {
            if (!r.invited()) continue;
            if (r.answer() == EventAnswer.GOING) going++;
            else if (r.answer() == EventAnswer.NOT_GOING) notGoing++;
            else noAnswer++;
            if (r.viewCount() == 0) neverViewed++;
        }

        List<ReminderDayDTO> reminderDays = remindersSent.findByEventIdOrderBySentOnAsc(eventId).stream()
                .map(s -> new ReminderDayDTO(s.getSentOn(), s.getSentAt(), s.getRecipients()))
                .toList();

        return new AttendanceDTO(event.getId(), event.getName(), event.getStartDate(), event.getEndDate(),
                event.startsAt(), event.isReminderEnabled(), event.getReminderTime(), event.getReminderDaysBefore(), reminderDays,
                invitees.size(), going, notGoing, noAnswer, neverViewed, rows);
    }

    /**
     * O que os seletores do público oferecem. Vem por aqui, e não por
     * {@code /api/employee}: quem organiza evento não precisa ter acesso ao RH.
     */
    @Transactional(readOnly = true)
    public AudienceOptionsDTO audienceOptions() {
        return new AudienceOptionsDTO(
                companies.findAll(Sort.by("name")).stream().map(EventMapper::option).toList(),
                departments.findAll(Sort.by("name")).stream().map(EventMapper::option).toList(),
                employees.findInvitable().stream().map(EventMapper::option).toList());
    }

    // ─── Apoio ───────────────────────────────────────────────────────────────

    private static AttendeeDTO row(Employee e, boolean invited, EventView view, EventResponse answer) {
        return new AttendeeDTO(e.getId(), e.getName(),
                e.getCompany() == null ? null : e.getCompany().getName(),
                EventMapper.departmentName(e), invited,
                view == null ? null : view.getFirstViewedAt(),
                view == null ? null : view.getLastViewedAt(),
                view == null ? 0 : view.getViewCount(),
                answer == null ? null : answer.getAnswer(),
                answer == null ? null : answer.getNote(),
                answer == null ? null : answer.getFirstAnsweredAt(),
                answer == null ? null : answer.getAnsweredAt());
    }

    private Map<UUID, EventResponse> answersOf(Employee employee) {
        return responses.findByEmployeeId(employee.getId()).stream()
                .collect(Collectors.toMap(r -> r.getEvent().getId(), Function.identity()));
    }

    /** O evento, se estiver publicado e a pessoa for convidada; senão, o mesmo 404 de inexistente. */
    private CompanyEvent invitedEvent(UUID eventId, Employee employee) {
        CompanyEvent event = events.findById(eventId).orElseThrow(EventNotFoundException::new);
        if (!event.isPublished() || !employees.isInvitedToEvent(eventId, employee.getId())) {
            throw new EventNotFoundException();
        }
        return event;
    }

    /** Conta sem funcionário (um admin, por exemplo) não tem convite. */
    private Employee employeeOf(String login) {
        return optionalEmployeeOf(login).orElseThrow(EventNotFoundException::new);
    }

    private Optional<Employee> optionalEmployeeOf(String login) {
        return users.findByLoginWithEmployee(login).map(User::getEmployee);
    }
}
