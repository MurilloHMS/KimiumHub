package com.proautokimium.api.Infrastructure.services.reports.guide;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class GuideReportServiceTest {

    @Test
    @DisplayName("REPORT_LOCATION deve apontar para um .jasper pré-compilado, não para um .jrxml compilado em runtime")
    void deveApontarParaJasperPreCompilado() throws Exception {
        Field field = GuideReportService.class.getDeclaredField("REPORT_LOCATION");
        field.setAccessible(true);
        String location = (String) field.get(null);

        assertThat(location)
                .as("relatórios .jrxml são compilados em runtime e falham dentro do jar empacotado " +
                        "(JRException: package net.sf.jasperreports.engine does not exist) — use o .jasper pré-compilado")
                .endsWith(".jasper");

        assertThat(getClass().getResourceAsStream("/templates/reports/" + location))
                .as("o .jasper referenciado precisa existir de fato no classpath")
                .isNotNull();
    }
}
