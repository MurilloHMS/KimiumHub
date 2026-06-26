package com.proautokimium.api.Infrastructure.utils;

import java.util.LinkedHashSet;

/**
 * Converte um hex (#RRGGBB) no nome básico de cor em português mais próximo,
 * por distância euclidiana no espaço RGB. Usado no PDF do Guia de Utilização:
 * o cadastro guarda o hex (renderiza a bolinha exata), mas no relatório mostramos
 * o nome básico (Azul, Amarelo, Verde, ...).
 */
public final class ColorNameUtil {

    private ColorNameUtil() {}

    private record NamedColor(String nome, int r, int g, int b) {}

    private static final NamedColor[] PALETTE = {
            new NamedColor("Branco",    255, 255, 255),
            new NamedColor("Preto",       0,   0,   0),
            new NamedColor("Cinza",     128, 128, 128),
            new NamedColor("Vermelho",  220,  38,  38),
            new NamedColor("Laranja",   234, 140,  30),
            new NamedColor("Amarelo",   245, 210,  40),
            new NamedColor("Verde",      34, 160,  80),
            new NamedColor("Azul",       30,  90, 180),
            new NamedColor("Roxo",      120,  60, 160),
            new NamedColor("Rosa",      235, 110, 170),
            new NamedColor("Marrom",    120,  72,  40),
    };

    /** Para 1+ hex separados por vírgula, devolve os nomes únicos separados por " / ". */
    public static String toNames(String coresHex) {
        if (coresHex == null || coresHex.isBlank()) return null;
        LinkedHashSet<String> nomes = new LinkedHashSet<>();
        for (String hex : coresHex.split(",")) {
            String nome = toName(hex.trim());
            if (nome != null) nomes.add(nome);
        }
        return nomes.isEmpty() ? null : String.join(" / ", nomes);
    }

    /** Nome básico da cor mais próxima de um hex (#RRGGBB). */
    public static String toName(String hex) {
        if (hex == null) return null;
        String c = hex.replace("#", "").trim();
        if (c.length() < 6) return null;
        try {
            int r = Integer.parseInt(c.substring(0, 2), 16);
            int g = Integer.parseInt(c.substring(2, 4), 16);
            int b = Integer.parseInt(c.substring(4, 6), 16);
            NamedColor best = null;
            double bestDist = Double.MAX_VALUE;
            for (NamedColor nc : PALETTE) {
                double d = Math.pow(r - nc.r(), 2) + Math.pow(g - nc.g(), 2) + Math.pow(b - nc.b(), 2);
                if (d < bestDist) { bestDist = d; best = nc; }
            }
            return best != null ? best.nome() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
