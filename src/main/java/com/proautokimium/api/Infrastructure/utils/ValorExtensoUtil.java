package com.proautokimium.api.Infrastructure.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utilitário que converte um valor em Reais para extenso.
 * Ex: 1579.80 → "Um mil e quinhentos e setenta e nove reais e oitenta centavos"
 */
public final class ValorExtensoUtil {

    private ValorExtensoUtil() {}

    private static final String[] UNIDADES = {
            "", "Um", "Dois", "Três", "Quatro", "Cinco",
            "Seis", "Sete", "Oito", "Nove", "Dez",
            "Onze", "Doze", "Treze", "Quatorze", "Quinze",
            "Dezesseis", "Dezessete", "Dezoito", "Dezenove"
    };

    public static final String[] UNIDADES_ESPECIAL = {
            "", "Uma", "Duas", "Três", "Quatro", "Cinco",
            "Seis", "Sete", "Oito", "Nove", "Dez",
            "Onze", "Doze", "Treze", "Quatorze", "Quinze",
            "Dezesseis", "Dezessete", "Dezoito", "Dezenove"
    };

    private static final String[] DEZENAS = {
            "", "", "Vinte", "Trinta", "Quarenta", "Cinquenta",
            "Sessenta", "Setenta", "Oitenta", "Noventa"
    };

    private static final String[] CENTENAS = {
            "", "Cento", "Duzentos", "Trezentos", "Quatrocentos",
            "Quinhentos", "Seiscentos", "Setecentos", "Oitocentos", "Novecentos"
    };

    private static final String[] CENTENAS_ESPECIAL = {
            "", "Cem", "Duzentos", "Trezentos", "Quatrocentos",
            "Quinhentos", "Seiscentos", "Setecentos", "Oitocentos", "Novecentos"
    };

    public static String converter(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) == 0) {
            return "Zero reais";
        }

        valor = valor.setScale(2, RoundingMode.HALF_UP);

        long inteiro = valor.longValue();
        int centavos = valor.subtract(BigDecimal.valueOf(inteiro))
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();

        StringBuilder sb = new StringBuilder();

        if (inteiro > 0) {
            sb.append(converterInteiro(inteiro));
            sb.append(inteiro == 1 ? " real" : " reais");
        }

        if (centavos > 0) {
            if (sb.length() > 0) sb.append(" e ");
            sb.append(converterInteiro(centavos));
            sb.append(centavos == 1 ? " centavo" : " centavos");
        }

        if (sb.length() > 0) {
            sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
        }

        return sb.toString();
    }

    private static String converterInteiro(long numero) {
        if (numero == 0) return "";

        if (numero == 1000000L) return "Um milhão";
        if (numero > 1000000L) {
            long milhoes = numero / 1000000L;
            long resto   = numero % 1000000L;
            String milhoesStr = converterInteiro(milhoes) + (milhoes == 1 ? " milhão" : " milhões");
            if (resto == 0) return milhoesStr;
            return milhoesStr + " e " + converterInteiro(resto);
        }

        if (numero == 1000L) return "Um mil";
        if (numero > 1000L) {
            long milhares = numero / 1000L;
            long resto    = numero % 1000L;
            String milStr = (milhares == 1 ? "Mil" : converterInteiro(milhares) + " mil");
            if (resto == 0) return milStr;
            String conector = (resto < 100) ? " e " : " e ";
            return milStr + conector + converterInteiro(resto);
        }

        if (numero == 100) return "Cem";

        if (numero > 100) {
            long centena = numero / 100;
            long resto   = numero % 100;
            if (resto == 0) return CENTENAS_ESPECIAL[(int) centena];
            return CENTENAS[(int) centena] + " e " + converterInteiro(resto);
        }

        if (numero < 20) return UNIDADES[(int) numero];

        long dezena  = numero / 10;
        long unidade = numero % 10;
        if (unidade == 0) return DEZENAS[(int) dezena];
        return DEZENAS[(int) dezena] + " e " + UNIDADES[(int) unidade];
    }
}