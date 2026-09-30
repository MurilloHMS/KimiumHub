package com.proautokimium.api.Infrastructure.services.authentication.webauthn;

/**
 * "Android · Chrome", "iPhone · Safari", "Windows · Edge": o nome que o Perfil e
 * o RH mostram para um aparelho, tirado do User-Agent do cadastro.
 *
 * <p>É só um rótulo para a pessoa reconhecer o aparelho — não decide nada. A
 * ordem das perguntas importa: o Edge e o Samsung Internet também dizem
 * "Chrome", e o Chrome também diz "Safari".
 */
public final class DeviceLabel {

    static final int MAX = 120;

    private DeviceLabel() {
    }

    public static String from(String userAgent) {
        String ua = userAgent == null ? "" : userAgent;
        String system = system(ua);
        String browser = browser(ua);
        String label = browser == null ? system : system + " · " + browser;
        return label.length() > MAX ? label.substring(0, MAX) : label;
    }

    private static String system(String ua) {
        if (ua.contains("Android")) return "Android";
        if (ua.contains("iPhone")) return "iPhone";
        if (ua.contains("iPad")) return "iPad";
        if (ua.contains("Windows")) return "Windows";
        if (ua.contains("Mac OS X") || ua.contains("Macintosh")) return "Mac";
        if (ua.contains("Linux")) return "Linux";
        return "Aparelho";
    }

    private static String browser(String ua) {
        if (ua.contains("Edg/") || ua.contains("EdgA/") || ua.contains("EdgiOS/")) return "Edge";
        if (ua.contains("SamsungBrowser/")) return "Samsung Internet";
        if (ua.contains("Firefox/") || ua.contains("FxiOS/")) return "Firefox";
        if (ua.contains("Chrome/") || ua.contains("CriOS/")) return "Chrome";
        if (ua.contains("Safari/")) return "Safari";
        return null;
    }
}
