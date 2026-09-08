package com.proautokimium.api.Infrastructure.utils;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A hora da ordem de serviço, que no ERP é texto livre.
 *
 * Vem em duas convenções — `13:00` e `14h20` — e em anotações que não são hora
 * nenhuma: `5:00 horas`, `1603`, `9:40 8/5`. A regra é a decisão dele: **o que
 * tem uma leitura só, lê; o que exige interpretação, vai para a correção
 * manual.** Vazio não é erro — é trabalho para a tela de revisão.
 */
public final class TimeParserUtil {

    /**
     * Hora e minuto, de uma ou duas casas cada, e nada além disso.
     *
     * **Os dois-pontos são obrigatórios.** Um número solto como `8` tanto pode
     * ser oito horas quanto oito horas de trabalho — a mesma ambiguidade de
     * `5:00 horas`, e adivinhar produziria um valor errado que ninguém confere.
     * Pendência é visível e corrigível.
     */
    private static final Pattern ACCEPT = Pattern.compile("(\\d{1,2}):(\\d{1,2})");

    private TimeParserUtil() {
    }

    public static Optional<LocalTime> interpret(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        String clean = text.trim()
                .toLowerCase()
                .replace("h", ":")
                .trim();

        Matcher achado = ACCEPT.matcher(clean);
        if (!achado.matches()) {
            return Optional.empty();
        }

        // `LocalTime.parse` é ISO-8601 e exige duas casas: `08:00` passa,
        // `8:00` estoura. Quem digita no ERP escreve das duas formas, e a
        // segunda é a mais natural — a OS 20043 da FRIMESA veio `8:00 até
        // 12:00` e caiu como pendência sendo perfeitamente legível.
        String hora = comDuasCasas(achado.group(1));
        String minuto = comDuasCasas(achado.group(2));

        try {
            return Optional.of(LocalTime.parse(hora + ":" + minuto));
        } catch (DateTimeParseException e) {
            // Sobra o que casa o formato e não existe no relógio: `25:00`,
            // `10:99`. Continua pendência, que é onde tem que aparecer.
            return Optional.empty();
        }
    }

    private static String comDuasCasas(String valor) {
        return valor.length() == 1 ? "0" + valor : valor;
    }
}
