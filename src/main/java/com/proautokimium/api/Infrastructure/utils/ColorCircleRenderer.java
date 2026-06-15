package com.proautokimium.api.Infrastructure.utils;

import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Utilitário para renderizar círculos coloridos como SVG.
 *
 * <p>
 * Utilizado diretamente nas expressões do JRXML para converter
 * uma lista de hexadecimais (ex: {@code "#1E90FF,#FF8C00"})
 * em um {@link InputStream} de SVG contendo círculos coloridos,
 * que o JasperReports renderiza como imagem.
 * </p>
 *
 * <p>
 * Suporta até 4 cores por produto. Círculos são organizados
 * em grade automática (1, 2 ou até 4 por linha).
 * </p>
 */
public class ColorCircleRenderer {

    private static final int SVG_WIDTH  = 25;
    private static final int SVG_HEIGHT = 25;
    private static final int CIRCLE_R   = 9;
    private static final int PADDING    = 2;

    private ColorCircleRenderer() {}

    /**
     * Converte uma string de hexadecimais separados por vírgula
     * em um SVG com círculos coloridos, retornado como {@link InputStream}.
     *
     * @param coresHex ex: {@code "#1E90FF"} ou {@code "#1E90FF,#FF8C00,#FFFFFF"}
     * @return {@link InputStream} do SVG, ou {@code null} se entrada inválida
     */
    public static InputStream render(String coresHex) {
        if (coresHex == null || coresHex.isBlank()) return null;

        List<String> cores = Arrays.stream(coresHex.split(","))
                .map(String::trim)
                .filter(s -> s.startsWith("#") && s.length() >= 4)
                .limit(4)
                .toList();

        if (cores.isEmpty()) return null;

        StringBuilder svg = new StringBuilder();
        svg.append(String.format(
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\">",
                SVG_WIDTH, SVG_HEIGHT
        ));

        // Fundo transparente
        svg.append("<rect width=\"100%\" height=\"100%\" fill=\"none\"/>");

        // Layout: até 2 colunas
        int cols = cores.size() == 1 ? 1 : 2;
        int rows = (int) Math.ceil((double) cores.size() / cols);

        int diameter = CIRCLE_R * 2;
        int totalW   = cols * diameter + (cols - 1) * PADDING;
        int totalH   = rows * diameter + (rows - 1) * PADDING;
        int startX   = (SVG_WIDTH  - totalW) / 2;
        int startY   = (SVG_HEIGHT - totalH) / 2;

        for (int i = 0; i < cores.size(); i++) {
            int col = i % cols;
            int row = i / cols;

            int cx = startX + col * (diameter + PADDING) + CIRCLE_R;
            int cy = startY + row * (diameter + PADDING) + CIRCLE_R;

            String hex = cores.get(i);

            // Borda branca para cores muito claras (transparente/branco)
            boolean isLight = isLightColor(hex);
            String stroke      = isLight ? "#AAAAAA" : "none";
            float  strokeWidth = isLight ? 0.8f : 0f;

            svg.append(String.format(
                    "<circle cx=\"%d\" cy=\"%d\" r=\"%d\" fill=\"%s\" stroke=\"%s\" stroke-width=\"%.1f\"/>",
                    cx, cy, CIRCLE_R, hex, stroke, strokeWidth
            ));
        }

        svg.append("</svg>");

        byte[] bytes = svg.toString().getBytes(StandardCharsets.UTF_8);
        return new ByteArrayInputStream(bytes);
    }

    /**
     * Heurística simples para detectar cores claras (próximas ao branco/transparente).
     */
    private static boolean isLightColor(String hex) {
        try {
            String clean = hex.replace("#", "");
            if (clean.length() < 6) return false;
            int r = Integer.parseInt(clean.substring(0, 2), 16);
            int g = Integer.parseInt(clean.substring(2, 4), 16);
            int b = Integer.parseInt(clean.substring(4, 6), 16);
            // Luminância relativa simplificada
            double luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0;
            return luminance > 0.85;
        } catch (Exception e) {
            return false;
        }
    }
}