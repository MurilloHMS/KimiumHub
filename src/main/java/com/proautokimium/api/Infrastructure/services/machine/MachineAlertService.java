package com.proautokimium.api.Infrastructure.services.machine;

import com.proautokimium.api.Application.DTOs.machine.MachineAlertConfigDTO;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.prostock.MachineAlertConfigRepository;
import com.proautokimium.api.Infrastructure.repositories.prostock.MachineAlertSentRepository;
import com.proautokimium.api.Infrastructure.repositories.prostock.RegisterRepository;
import com.proautokimium.api.Infrastructure.services.email.EmailQueueService;
import com.proautokimium.api.domain.entities.prostock.machine.Machine;
import com.proautokimium.api.domain.entities.prostock.machine.MachineAlertConfig;
import com.proautokimium.api.domain.entities.prostock.machine.MachineAlertSent;
import com.proautokimium.api.domain.entities.prostock.machine.MachineRegister;
import com.proautokimium.api.domain.enums.MachineStatus;
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
    private static final String ALERT_TEMPLATE = "html/machine-alert";
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
    private final Clock clock;

    public MachineAlertService(TemplateEngine templateEngine, MachineAlertConfigRepository configRepository, MachineAlertSentRepository sentRepository, RegisterRepository registerRepository, EmployeeRepository employeeRepository, EmailQueueService emailQueueService, Clock clock) {
        this.templateEngine = templateEngine;
        this.configRepository = configRepository;
        this.sentRepository = sentRepository;
        this.registerRepository = registerRepository;
        this.employeeRepository = employeeRepository;
        this.emailQueueService = emailQueueService;
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
     * Quem merece aviso hoje. Compara DATA, não instante: previsaoEntrega é
     * LocalDateTime e comparar timestamps erraria por causa das horas.
     */
    @Transactional
    public int runAlerts(boolean ignoreSchedule) {
        MachineAlertConfig config = configRepository.findAll().stream().findFirst().orElse(null);
        if (config == null || !config.isActive()) return 0;

        LocalDate today = LocalDate.now(clock);
        if (!ignoreSchedule && LocalTime.now(clock).getHour() != config.getSendAt().getHour()) return 0;

        List<String> recipients = resolveRecipients(config);
        if (recipients.isEmpty()) return 0;

        int sent = 0;

        for (MachineRegister register : registerRepository.findAll()) {
            if (register.getStatus() == MachineStatus.ENTREGUE) continue;
            if (register.getPrevisaoEntrega() == null) continue;

            LocalDate due = register.getPrevisaoEntrega().toLocalDate();
            long daysLeft = ChronoUnit.DAYS.between(today, due);

            Integer marker = null;
            if (daysLeft >= 0 && config.getDaysBefore().contains((int) daysLeft)) {
                marker = (int) daysLeft;
            } else if (daysLeft < 0 && config.isAlertWhenLate()) {
                marker = LATE_MARKER;
            }
            if (marker == null) continue;

            if (sentRepository.alreadySent(register.getId(), today, marker)) {
                continue;
            }

            String subject = daysLeft < 0
                    ? "Máquina atrasada: " + register.getNomeCliente()
                    : "Saída em " + daysLeft + " dia(s): " + register.getNomeCliente();

            String body = buildBody(register, daysLeft);
            recipients.forEach(to -> emailQueueService.sendEmail(to, FROM, subject, body));

            sentRepository.save(new MachineAlertSent(register.getId(), today, marker));
            sent++;
        }

        return sent;
    }

    /**
     * E-mail de exemplo para conferir a configuracao.
     *
     * Nao olha `active` nem procura registro de verdade, e nao grava no
     * historico de enviados: quem clica em "enviar teste" quer saber se o
     * e-mail chega, nao se a regra de datas esta certa. Sao duas perguntas
     * diferentes e cada uma merece o seu proprio caminho.
     */
    public int sendSampleAlert() {
        MachineAlertConfig config = configRepository.findAll().stream().findFirst().orElse(null);
        if (config == null) return 0;

        List<String> recipients = resolveRecipients(config);
        if (recipients.isEmpty()) return 0;

        String body = buildBody(sampleRegister(), 3);
        String subject = "[TESTE] Exemplo de alerta de saida de maquina";

        recipients.forEach(to -> emailQueueService.sendEmail(to, FROM, subject, body));
        return recipients.size();
    }

    /** Registro ficticio, apenas para o template ter o que mostrar. */
    private MachineRegister sampleRegister() {
        Machine machine = new Machine();
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

    /**
     * O texto muda conforme o prazo: aviso prévio conta os dias que faltam,
     * atraso conta os dias vencidos. O template decide a cor a partir de
     * `atrasado`, para não existirem dois arquivos quase iguais.
     */
    private String buildBody(MachineRegister register, long daysLeft) {
        boolean atrasado = daysLeft < 0;

        Context ctx = new Context(new Locale("pt", "BR"));
        ctx.setVariable("atrasado", atrasado);
        ctx.setVariable("chamada", chamada(daysLeft));
        ctx.setVariable("cliente", blankToNull(register.getNomeCliente()));
        ctx.setVariable("maquina", register.getMachine() != null ? register.getMachine().getName() : "—");
        ctx.setVariable("previsao", register.getPrevisaoEntrega().toLocalDate().format(DATE_BR));
        ctx.setVariable("regiao", blankToNull(register.getRegiao()));
        ctx.setVariable("consultor", blankToNull(register.getConsultor()));
        ctx.setVariable("tecnico", blankToNull(register.getTecnico()));
        ctx.setVariable("observacao", blankToNull(register.getObservacao()));
        ctx.setVariable("link", websiteBaseUrl + "/stock/programacao");

        return templateEngine.process(ALERT_TEMPLATE, ctx);
    }

    private String chamada(long daysLeft) {
        if (daysLeft < 0) return Math.abs(daysLeft) + " dia(s) em atraso";
        if (daysLeft == 0) return "hoje";
        if (daysLeft == 1) return "amanhã";
        return "em " + daysLeft + " dias";
    }

    /** `th:if` trata string vazia como verdadeira; null é o que esconde a linha. */
    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
