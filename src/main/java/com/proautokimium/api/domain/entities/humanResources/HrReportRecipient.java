package com.proautokimium.api.domain.entities.humanResources;

import com.proautokimium.api.domain.exceptions.humanResources.InvalidRequestDataException;
import com.proautokimium.api.domain.valueObjects.Email;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Um endereço que recebe os relatórios do RH por e-mail.
 *
 * O e-mail entra normalizado (sem espaço, em minúsculas): a unicidade do banco
 * compara texto, e "RH@..." e "rh@..." são a mesma caixa.
 * {@code createdBy} guarda o LOGIN, não FK — é registro de quem fez, e
 * sobrevive ao usuário ser apagado (mesmo padrão do template de assinatura).
 */
@jakarta.persistence.Entity
@Table(name = "hr_report_recipients")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HrReportRecipient extends com.proautokimium.api.domain.abstractions.Entity {

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    private HrReportRecipient(String email, String createdBy, LocalDateTime createdAt) {
        this.email = email;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public static HrReportRecipient create(String email, String createdBy, LocalDateTime now) {
        String normalized = normalize(email);
        if (!Email.isValid(normalized)) {
            throw new InvalidRequestDataException("E-mail inválido");
        }
        return new HrReportRecipient(normalized, createdBy, now);
    }

    public static String normalize(String email) {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }
}
