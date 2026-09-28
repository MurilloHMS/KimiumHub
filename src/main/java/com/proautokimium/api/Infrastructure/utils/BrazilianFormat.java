package com.proautokimium.api.Infrastructure.utils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Datas e dinheiro como o leitor brasileiro espera, para documentos gerados
 * pela API (PDF, e-mail).
 *
 * O dinheiro NÃO usa {@code NumberFormat.getCurrencyInstance}: ele põe um
 * espaço não separável (U+00A0) entre "R$" e o número, que some ou vira
 * lixo em fonte embutida e quebra toda busca por "R$ 1.694,90" no PDF.
 *
 * Também é chamado de dentro do jrxml, para o subtotal que o Jasper soma.
 */
public final class BrazilianFormat {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormatSymbols SYMBOLS = DecimalFormatSymbols.getInstance(Locale.of("pt", "BR"));

    private BrazilianFormat() {}

    public static String money(BigDecimal value) {
        if (value == null) return "—";
        // DecimalFormat não é thread-safe: um por chamada, que é barato.
        return "R$ " + new DecimalFormat("#,##0.00", SYMBOLS).format(value);
    }

    public static String date(LocalDate value) {
        return value == null ? "—" : value.format(DATE);
    }

    public static String date(LocalDateTime value) {
        return value == null ? "—" : value.format(DATE);
    }

    public static String dateTime(LocalDateTime value) {
        return value == null ? "—" : value.format(DATE_TIME);
    }
}
