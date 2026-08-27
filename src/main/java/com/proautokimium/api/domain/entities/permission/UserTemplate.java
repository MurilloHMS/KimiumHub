package com.proautokimium.api.domain.entities.permission;

import com.proautokimium.api.domain.enums.ApplyMode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * O registro de que um carimbo passou por alguém.
 *
 * **Não é vínculo vivo.** Apagar uma linha daqui não muda permissão nenhuma —
 * quem manda é `user_permissions`. Existe por um motivo só, e é o risco que o
 * plano registrou: como o carimbo não propaga, um modelo corrigido hoje não
 * conserta ninguém. Sem esta tabela, a tela do modelo não teria como dizer
 * "3 pessoas usaram este carimbo" nem oferecer reaplicar, e a correção ficaria
 * na memória de quem lembrasse.
 *
 * `appliedAt` é preenchido no Java e não pelo `DEFAULT clock_timestamp()` da
 * coluna: os testes rodam em H2, onde a tabela é criada a partir desta classe e
 * essa função não existe.
 */
@Entity
@Table(name = "user_templates")
@IdClass(UserTemplate.Key.class)
@Getter
@Setter
@NoArgsConstructor
public class UserTemplate {

    @Id
    @Column(name = "user_id")
    private String userId;

    @Id
    @Column(name = "template_id")
    private UUID templateId;

    @Column(name = "applied_at", nullable = false)
    private OffsetDateTime appliedAt = OffsetDateTime.now();

    /** Nulo quando quem aplicou foi o sistema — uma migration, ou o primeiro acesso. */
    @Column(name = "applied_by", length = 120)
    private String appliedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", length = 12, nullable = false)
    private ApplyMode mode = ApplyMode.SOMAR;

    public UserTemplate(String userId, UUID templateId, String appliedBy, ApplyMode mode) {
        this.userId = userId;
        this.templateId = templateId;
        this.appliedBy = appliedBy;
        this.mode = mode;
        this.appliedAt = OffsetDateTime.now();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Key implements Serializable {
        private String userId;
        private UUID templateId;

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Key key)) return false;
            return Objects.equals(userId, key.userId)
                    && Objects.equals(templateId, key.templateId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, templateId);
        }
    }
}
