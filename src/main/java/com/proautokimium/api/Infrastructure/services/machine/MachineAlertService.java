package com.proautokimium.api.Infrastructure.services.machine;

import com.proautokimium.api.Application.DTOs.machine.MachineAlertConfigDTO;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.prostock.MachineAlertConfigRepository;
import com.proautokimium.api.Infrastructure.repositories.prostock.MachineAlertSentRepository;
import com.proautokimium.api.Infrastructure.repositories.prostock.RegisterRepository;
import com.proautokimium.api.Infrastructure.services.email.EmailQueueService;
import com.proautokimium.api.domain.entities.prostock.ProductInventory;
import com.proautokimium.api.domain.entities.prostock.machine.MachineAlertConfig;
import com.proautokimium.api.domain.entities.prostock.machine.MachineAlertSent;
import com.proautokimium.api.domain.entities.prostock.machine.MachineRegister;
import com.proautokimium.api.domain.enums.MachineStatus;
import com.proautokimium.api.domain.enums.NotificationType;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class MachineAlertService {
    private static final String ALERT_TEMPLATE = "html/machine-alert-digest";
    private static final DateTimeFormatter DATE_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TemplateEngine templateEngine;
    @Value("${app.base-url}")
    String websiteBaseUrl;

    private static final String FROM = "noreply@envios.proautokimium.com.br";
    private static final int LATE_MARKER = -1;

    private final MachineAlertConfigRepository configRepository;
    private final MachineAlertSentRepository sentRepository;
    private final RegisterRepository registerRepository;
    private final EmployeeRepository employeeRepository;
    private final EmailQueueService emailQueueService;
    private final com.proautokimium.api.Infrastructure.repositories.UserRepository userRepository;
    private final com.proautokimium.api.Infrastructure.services.notification.NotificationService notificationService;
    private final Clock clock;

    public MachineAlertService(TemplateEngine templateEngine, MachineAlertConfigRepository configRepository, MachineAlertSentRepository sentRepository, RegisterRepository registerRepository, EmployeeRepository employeeRepository, EmailQueueService emailQueueService,
                               com.proautokimium.api.Infrastructure.repositories.UserRepository userRepository,
                               com.proautokimium.api.Infrastructure.services.notification.NotificationService notificationService,
                               Clock clock) {
        this.templateEngine = templateEngine;
        this.configRepository = configRepository;
        this.sentRepository = sentRepository;
        this.registerRepository = registerRepository;
        this.employeeRepository = employeeRepository;
        this.emailQueueService = emailQueueService;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    public MachineAlertConfigDTO get(){
        return configRepository.findAll().stream().findFirst()
                .map(this::toDto)
                .orElseGet(() -> new MachineAlertConfigDTO(
                        false, List.of(3), true, LocalTime.of(8,0), List.of()));
    }

    @Transactional
    public MachineAlertConfigDTO save(MachineAlertConfigDTO dto) {
        MachineAlertConfig config = configRepository.findAll().stream().findFirst()
                .orElseGet(MachineAlertConfig::new);

        config.setActive(dto.active());
        config.setAlertWhenLate(dto.alertWhenLate());
        config.setSendAt(dto.sendAt());
        config.setDaysBefore(new ArrayList<>(dto.daysBefore()));
        config.setRecipientEmployeeIds(new ArrayList<>(dto.recipientEmployeeIds()));

        return toDto(configRepository.save(config));
    }

    /**
     * Um aviso por dia com tudo: as atrasadas e as saídas próximas, num e-mail
     * só para cada destinatário e uma notificação no app para cada um que tem
     * login.
     *
     * <p>Antes era um e-mail por máquina — com dez atrasadas, dez e-mails por
     * pessoa, todo dia, e o resumo que importa ("o que está pegando hoje")
     * ninguém via inteiro.
     *
     * <p>Compara DATA, não instante: previsaoEntrega é LocalDateTime e comparar
     * timestamps erraria por causa das horas. Uma máquina entra no resumo uma
     * vez por dia ({@code MachineAlertSent}); rodar duas vezes no mesmo dia não
     * repete nada, e sem nada novo não sai e-mail nem notificação.
     *
     * @return quantas máquinas entraram no resumo
     */
    @Transactional
    public int runAlerts(boolean ignoreSchedule) {
        MachineAlertConfig config = configRepository.findAll().stream().findFirst().orElse(null);
        if (config == null || !config.isActive()) return 0;

        LocalDate today = LocalDate.now(clock);
        if (!ignoreSchedule && LocalTime.now(clock).getHour() != config.getSendAt().getHour()) return 0;

        List<String> emails = resolveRecipients(config);
        List<String> logins = resolveLogins(config);
        if (emails.isEmpty() && logins.isEmpty()) return 0;

        List<AlertItem> items = new ArrayList<>();
        for (MachineRegister register : registerRepository.findAll()) {
            if (register.getStatus() == MachineStatus.ENTREGUE) continue;
            if (register.getPrevisaoEntrega() == null) continue;

            long daysLeft = ChronoUnit.DAYS.between(today, register.getPrevisaoEntrega().toLocalDate());
            Integer marker = null;
            if (daysLeft >= 0 && config.getDaysBefore().contains((int) daysLeft)) {
                marker = (int) daysLeft;
            } else if (daysLeft < 0 && config.isAlertWhenLate()) {
                marker = LATE_MARKER;
            }
            if (marker == null) continue;
            if (sentRepository.alreadySent(register.getId(), today, marker)) continue;

            items.add(new AlertItem(register, daysLeft, marker));
        }
        if (items.isEmpty()) return 0;

        Digest digest = digest(items);
        String subject = subject(digest);
        String body = buildDigestBody(digest);
        emails.forEach(to -> emailQueueService.sendEmail(to, FROM, subject, body));

        items.forEach(item -> sentRepository.save(new MachineAlertSent(item.register().getId(), today, item.marker())));

        String link = "/stock/programacao" + (digest.late().isEmpty() ? "" : "?atrasadas=1");
        logins.forEach(login -> notificationService.notify(login, NotificationType.PROGRAMACAO,
                "Resumo da programação", summary(digest), link));

        return items.size();
    }

    /**
     * E-mail de exemplo para conferir a configuracao.
     *
     * Nao olha `active` nem procura registro de verdade, e nao grava no
     * historico de enviados: quem clica em "enviar teste" quer saber se o
     * e-mail chega, nao se a regra de datas esta certa. Sao duas perguntas
     * diferentes e cada uma merece o seu proprio caminho. Sai no formato do
     * resumo, com uma atrasada e uma proxima de exemplo.
     */
    public int sendSampleAlert() {
        MachineAlertConfig config = configRepository.findAll().stream().findFirst().orElse(null);
        if (config == null) return 0;

        List<String> recipients = resolveRecipients(config);
        if (recipients.isEmpty()) return 0;

        MachineRegister late = sampleRegister();
        late.setNomeCliente("Cliente atrasado de exemplo");
        late.setPrevisaoEntrega(LocalDate.now(clock).minusDays(2).atStartOfDay());
        Digest digest = digest(List.of(new AlertItem(late, -2, LATE_MARKER),
                new AlertItem(sampleRegister(), 3, 3)));
        String body = buildDigestBody(digest);
        String subject = "[TESTE] " + subject(digest);

        recipients.forEach(to -> emailQueueService.sendEmail(to, FROM, subject, body));
        return recipients.size();
    }

    /** Registro ficticio, apenas para o template ter o que mostrar. */
    private MachineRegister sampleRegister() {
        ProductInventory machine = new ProductInventory();
        machine.setName("CAPO NT 300");

        MachineRegister sample = new MachineRegister(machine);
        sample.setNomeCliente("Cliente de exemplo");
        sample.setRegiao("SP");
        sample.setSolicitante("Solicitante de exemplo");
        sample.setStatus(MachineStatus.RESERVADA);
        sample.setObservacao("Este e um e-mail de teste da configuracao de alertas.");
        sample.setPrevisaoEntrega(LocalDate.now(clock).plusDays(3).atStartOfDay());
        sample.setConsultor("Consultor de exemplo");
        sample.setTecnico("Tecnico de exemplo");

        return sample;
    }

    private MachineAlertConfigDTO toDto(MachineAlertConfig config) {
        return new MachineAlertConfigDTO(
                config.isActive(),
                config.getDaysBefore(),
                config.isAlertWhenLate(),
                config.getSendAt(),
                config.getRecipientEmployeeIds()
        );
    }

    private List<String> resolveRecipients(MachineAlertConfig config) {
        return employeeRepository.findAllById(config.getRecipientEmployeeIds()).stream()
                .filter(employee -> employee.getEmail() != null)
                .map(employee -> employee.getEmail().getAddress())
                .filter(Objects::nonNull)
                .toList();
    }

    /** Uma máquina no resumo do dia: o registro, os dias até a previsão e a marca do envio. */
    record AlertItem(MachineRegister register, long daysLeft, int marker) {}

    /** O resumo separado: atrasadas (a mais atrasada primeiro) e próximas (a mais perto primeiro). */
    record Digest(List<AlertItem> late, List<AlertItem> upcoming) {}

    static Digest digest(List<AlertItem> items) {
        java.util.Comparator<AlertItem> byDays = java.util.Comparator.comparingLong(AlertItem::daysLeft);
        return new Digest(
                items.stream().filter(i -> i.daysLeft() < 0).sorted(byDays).toList(),
                items.stream().filter(i -> i.daysLeft() >= 0).sorted(byDays).toList());
    }

    /** "Programação: 5 atrasadas e 3 saídas próximas" — o assunto já diz o tamanho do problema. */
    static String subject(Digest d) {
        return "Programação: " + summary(d).replace(" · ", " e ");
    }

    /** "5 atrasadas · 3 saídas próximas", ou só a parte que existe. */
    static String summary(Digest d) {
        List<String> parts = new ArrayList<>();
        int late = d.late().size();
        int upcoming = d.upcoming().size();
        if (late > 0) parts.add(late + (late == 1 ? " atrasada" : " atrasadas"));
        if (upcoming > 0) parts.add(upcoming + (upcoming == 1 ? " saída próxima" : " saídas próximas"));
        return String.join(" · ", parts);
    }

    private String buildDigestBody(Digest d) {
        Context ctx = new Context(new Locale("pt", "BR"));
        ctx.setVariable("assunto", subject(d));
        ctx.setVariable("resumo", summary(d));
        ctx.setVariable("totalAtrasadas", d.late().size());
        ctx.setVariable("totalProximas", d.upcoming().size());
        ctx.setVariable("atrasadas", d.late().stream().map(this::row).toList());
        ctx.setVariable("proximas", d.upcoming().stream().map(this::row).toList());
        ctx.setVariable("link", websiteBaseUrl + "/stock/programacao");
        return templateEngine.process(ALERT_TEMPLATE, ctx);
    }

    /** Mapa, e não record: o Thymeleaf lê a linha por nome de propriedade. */
    private java.util.Map<String, Object> row(AlertItem item) {
        MachineRegister r = item.register();
        java.util.Map<String, Object> row = new java.util.HashMap<>();
        row.put("maquina", r.getMachine() != null ? r.getMachine().getName() : "—");
        String cliente = blankToNull(r.getNomeCliente());
        row.put("cliente", cliente != null ? cliente : "Cliente não informado");
        row.put("regiao", blankToNull(r.getRegiao()));
        String pessoas = java.util.stream.Stream.of(blankToNull(r.getConsultor()), blankToNull(r.getTecnico()))
                .filter(Objects::nonNull).collect(java.util.stream.Collectors.joining(" · "));
        row.put("pessoas", pessoas.isEmpty() ? null : pessoas);
        row.put("previsao", r.getPrevisaoEntrega().toLocalDate().format(DATE_BR));
        row.put("chamada", chamada(item.daysLeft()));
        return row;
    }

    static String chamada(long daysLeft) {
        if (daysLeft == -1) return "1 dia de atraso";
        if (daysLeft < 0) return Math.abs(daysLeft) + " dias de atraso";
        if (daysLeft == 0) return "hoje";
        if (daysLeft == 1) return "amanhã";
        return "em " + daysLeft + " dias";
    }

    /** Quem tem login entre os destinatários: a notificação do app vai para eles. */
    private List<String> resolveLogins(MachineAlertConfig config) {
        if (config.getRecipientEmployeeIds() == null || config.getRecipientEmployeeIds().isEmpty()) return List.of();
        return userRepository.findActiveByEmployeeIds(config.getRecipientEmployeeIds()).stream()
                .map(com.proautokimium.api.domain.entities.auth.User::getLogin)
                .distinct()
                .toList();
    }

    /** `th:if` trata string vazia como verdadeira; null é o que esconde a linha. */
    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
