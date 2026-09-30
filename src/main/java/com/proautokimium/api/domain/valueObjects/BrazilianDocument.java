package com.proautokimium.api.domain.valueObjects;

/**
 * CPF e CNPJ pelo dígito verificador — a mesma regra de
 * {@code documento-br.ts} no site.
 *
 * Sequência repetida ({@code 111.111.111-11}) passa na conta dos dígitos, mas
 * não é documento de ninguém, e é recusada.
 */
public final class BrazilianDocument {

    private BrazilianDocument() {
    }

    public static String digits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    public static boolean isValidCpf(String value) {
        String d = digits(value);
        if (d.length() != 11 || repeated(d)) {
            return false;
        }
        return checkDigit(d, 9, 10) == d.charAt(9) - '0'
                && checkDigit(d, 10, 11) == d.charAt(10) - '0';
    }

    public static boolean isValidCnpj(String value) {
        String d = digits(value);
        if (d.length() != 14 || repeated(d)) {
            return false;
        }
        int[] first = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] second = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        return cnpjDigit(d, first) == d.charAt(12) - '0'
                && cnpjDigit(d, second) == d.charAt(13) - '0';
    }

    /** CPF com 11 dígitos, CNPJ com 14; qualquer outro tamanho é inválido. */
    public static boolean isValidCpfOrCnpj(String value) {
        String d = digits(value);
        return d.length() == 11 ? isValidCpf(d) : isValidCnpj(d);
    }

    private static int checkDigit(String d, int length, int startWeight) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (d.charAt(i) - '0') * (startWeight - i);
        }
        int rest = (sum * 10) % 11;
        return rest == 10 ? 0 : rest;
    }

    private static int cnpjDigit(String d, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += (d.charAt(i) - '0') * weights[i];
        }
        int rest = sum % 11;
        return rest < 2 ? 0 : 11 - rest;
    }

    private static boolean repeated(String d) {
        return d.chars().allMatch(c -> c == d.charAt(0));
    }
}
