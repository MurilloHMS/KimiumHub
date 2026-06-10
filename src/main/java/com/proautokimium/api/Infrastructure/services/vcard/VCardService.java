package com.proautokimium.api.Infrastructure.services.vcard;

import com.proautokimium.api.Infrastructure.interfaces.vcard.IVCardService;
import com.proautokimium.api.domain.entities.Profile;
import com.proautokimium.api.domain.models.RedeSocial;
import com.proautokimium.api.domain.models.TelefoneContato;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class VCardService implements IVCardService {

    @Value("${app.base-url}")
    private String frontendUrl;

    @Override
    public byte[] generate(Profile profile) {

        StringBuilder vcf = new StringBuilder();

        vcf.append("BEGIN:VCARD\n");
        vcf.append("VERSION:3.0\n");

        // Nome completo
        vcf.append("FN:")
                .append(sanitize(profile.getNome()))
                .append("\n");

        // Nome estruturado
        vcf.append("N:;")
                .append(sanitize(profile.getNome()))
                .append(";;;\n");

        // Empresa
        vcf.append("ORG:Proauto Kimium\n");

        // Cargo
        if (hasText(profile.getCargo())) {
            vcf.append("TITLE:")
                    .append(sanitize(profile.getCargo()))
                    .append("\n");
        }

        // E-mail
        if (profile.getEmail() != null &&
                hasText(profile.getEmail().getAddress())) {

            vcf.append("EMAIL;TYPE=WORK:")
                    .append(profile.getEmail().getAddress())
                    .append("\n");
        }

        // Telefones
        if (profile.getTelefones() != null) {

            for (TelefoneContato telefone : profile.getTelefones()) {

                if (telefone == null || !hasText(telefone.getNumero())) {
                    continue;
                }

                String tipo = hasText(telefone.getTipo())
                        ? telefone.getTipo().toUpperCase()
                        : "CELL";

                vcf.append("TEL;TYPE=")
                        .append(tipo)
                        .append(":")
                        .append(telefone.getNumero())
                        .append("\n");
            }
        }

        // Foto
        if (hasText(profile.getImagem())) {

            vcf.append("PHOTO;VALUE=URI:")
                    .append(profile.getImagem())
                    .append("\n");
        }

        // URL do cartão digital
        vcf.append("URL:")
                .append(frontendUrl)
                .append("/profile/")
                .append(profile.getSlug())
                .append("\n");

        // Descrição
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

        // Redes sociais
        if (profile.getRedesSociais() != null) {
            for (RedeSocial rede : profile.getRedesSociais()) {

                if (rede == null ||
                        !hasText(rede.getTipo()) ||
                        !hasText(rede.getUrl())) {
                    continue;
                }

                vcf.append("X-SOCIALPROFILE;TYPE=")
                        .append(rede.getTipo().toLowerCase())
                        .append(":")
                        .append(rede.getUrl())
                        .append("\r\n");
            }
        }

        vcf.append("END:VCARD");

        return vcf.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String sanitize(String value) {

        if (value == null) {
            return "";
        }

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