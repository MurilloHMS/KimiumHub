package com.proautokimium.api.Infrastructure.settings;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;
import java.util.TimeZone;

@Configuration
public class ClockConfig {

    /**
     * O fuso da operação. Cravado no código, e não herdado do ambiente.
     *
     * O container roda em UTC, e `LocalDateTime` não carrega fuso: o Jackson
     * serializa "2026-08-20T19:40:00" sem offset, o navegador lê como hora
     * local, e o valor errado atravessa inteiro. Quem salvava às 16:40 via
     * 19:40 na tela.
     *
     * A alternativa correta em sistema multi-fuso seria trocar LocalDateTime
     * por Instant e mandar offset no JSON — reescreveria DTO e entidade do
     * projeto inteiro. A empresa opera num fuso só.
     */
    public static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");

    /**
     * Muda o padrão da JVM, não só o bean.
     *
     * Só o Clock não bastaria: metade do código chama `LocalDateTime.now()`
     * direto, sem clock injetado, e continuaria em UTC. Isto também alinha o
     * Hibernate e o driver JDBC.
     */
    @PostConstruct
    public void fixarFusoDaAplicacao() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZONA));
    }

    @Bean
    public Clock clock() {
        return Clock.system(ZONA);
    }
}