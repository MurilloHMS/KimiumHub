package com.proautokimium.api.Infrastructure.services.vcard;

import com.proautokimium.api.Infrastructure.interfaces.vcard.IVCardService;
import com.proautokimium.api.domain.entities.Profile;
import com.proautokimium.api.domain.models.RedeSocial;
import com.proautokimium.api.domain.models.TelefoneContato;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Service
public class VCardService implements IVCardService {

    @Value("${app.base-url}")
    private String frontendUrl;

    @Override
    public byte[] generate(Profile profile) {

        StringBuilder vcf = new StringBuilder();

        vcf.append("BEGIN:VCARD\r\n");
        vcf.append("VERSION:3.0\r\n");
        vcf.append("PRODID:-//Proauto Kimium//vCard 1.0//BR\r\n");
        vcf.append("UID:").append(profile.getSlug()).append("\r\n");

        vcf.append("FN:")
                .append(sanitize(profile.getNome()))
                .append("\r\n");

        vcf.append("N:")
                .append(sanitize(profile.getNome()))
                .append(";;;;\r\n");

        vcf.append("ORG:")
                .append(hasText(profile.getEmpresa()) ? sanitize(profile.getEmpresa()) : "Proauto Kimium")
                .append("\r\n");

        if (hasText(profile.getCargo())) {
            vcf.append("TITLE:")
                    .append(sanitize(profile.getCargo()))
                    .append("\r\n");
        }

        if (profile.getEmail() != null && hasText(profile.getEmail().getAddress())) {
            vcf.append("EMAIL;TYPE=WORK:")
                    .append(profile.getEmail().getAddress())
                    .append("\r\n");
        }

        if (profile.getTelefones() != null) {

            for (TelefoneContato t : profile.getTelefones()) {

                if (t == null || !hasText(t.getNumero())) continue;

                String raw = t.getNumero();
                String phone = normalizePhone(raw);

                vcf.append("TEL;TYPE=")
                        .append(mapPhoneType(t.getTipo()))
                        .append(":")
                        .append(phone)
                        .append("\r\n");

                if (isWhatsApp(t.getTipo())) {

                    vcf.append("URL;TYPE=WHATSAPP:")
                            .append(buildWhatsAppLink(raw))
                            .append("\r\n");
                }
            }
        }

        if (hasText(profile.getImagem())) {
            vcf.append("PHOTO;VALUE=URI:")
                    .append(profile.getImagem())
                    .append("\r\n");
        }

        vcf.append("URL:")
                .append(frontendUrl)
                .append("/profile/")
                .append(profile.getSlug())
                .append("\r\n");

        StringBuilder note = new StringBuilder();

        if (hasText(profile.getDescricao())) {
            note.append(profile.getDescricao().trim());
        }

        if (profile.getRegioesAtendimento() != null && !profile.getRegioesAtendimento().isEmpty()) {
            if (!note.isEmpty()) note.append("\n\n");
            note.append("Regiões de atendimento: ")
                    .append(String.join(", ", profile.getRegioesAtendimento()));
        }

        if (profile.getSegmentosAtendimento() != null && !profile.getSegmentosAtendimento().isEmpty()) {
            if (!note.isEmpty()) note.append("\n\n");
            note.append("Segmentos de atendimento: ")
                    .append(String.join(", ", profile.getSegmentosAtendimento()));
        }

        if (!note.isEmpty()) {
            vcf.append("NOTE:")
                    .append(sanitize(note.toString()))
                    .append("\r\n");
        }

        if (profile.getRedesSociais() != null) {

            for (RedeSocial rede : profile.getRedesSociais()) {

                if (rede == null || !hasText(rede.getUrl())) continue;

                vcf.append("URL:")
                        .append(rede.getUrl())
                        .append("\r\n");
            }
        }

        vcf.append("REV:")
                .append(Instant.now().toString())
                .append("\r\n");

        vcf.append("END:VCARD\r\n");

        return vcf.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String mapPhoneType(String tipo) {
        if (tipo == null) return "CELL";

        return switch (tipo.toUpperCase()) {
            case "CELULAR" -> "CELL";
            case "TELEFONE" -> "WORK";
            case "FIXO" -> "HOME";
            case "WHATSAPP" -> "CELL";
            default -> "CELL";
        };
    }

    private boolean isWhatsApp(String tipo) {
        return "WHATSAPP".equalsIgnoreCase(tipo);
    }

    private String normalizePhone(String phone) {
        if (phone == null) return "";

        String digits = phone.replaceAll("\\D", "");
        digits = digits.replaceFirst("^0+", "");

        if (!digits.startsWith("55")) {
            digits = "55" + digits;
        }

        return "+" + digits;
    }

    private String buildWhatsAppLink(String phone) {
        String digits = phone.replaceAll("\\D", "");
        digits = digits.replaceFirst("^0+", "");

        if (!digits.startsWith("55")) {
            digits = "55" + digits;
        }

        return "https://wa.me/" + digits;
    }

    private String sanitize(String value) {
        if (value == null) return "";

        return value
                .replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r", "")
                .replace("\n", "\\n");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}