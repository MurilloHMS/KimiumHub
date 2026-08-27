package com.proautokimium.api.domain.entities.permission;

import com.proautokimium.api.domain.enums.Permission;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Uma célula do grid de um modelo.
 *
 * **Todas as combinações existem, inclusive as negadas.** Guardar só o que é
 * permitido daria uma tabela menor, mas a tela de configuração não teria como
 * distinguir "negado de propósito" de "tela que ninguém configurou ainda" — as
 * duas ficariam iguais no banco.
 */
@Entity
@Table(name = "template_permissions")
@IdClass(TemplatePermission.Key.class)
@Getter @Setter
@NoArgsConstructor
public class TemplatePermission {

    @Id
    @Column(name = "template_id")
    private UUID templateId;

    @Id
    @Column(name = "screen_code", length = 120)
    private String screenCode;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "permission", length = 20)
    private Permission permission;

    @Column(name = "allowed", nullable = false)
    private boolean allowed;

    /**
     * A chave composta.
     *
     * `equals` e `hashCode` não são ritual: sem eles o Hibernate não consegue
     * dizer se duas linhas são a mesma, e passa a recarregar, duplicar ou
     * perder alteração — sem erro nenhum.
     */
    @Getter @Setter
    @NoArgsConstructor
    public static class Key implements Serializable {
        private UUID templateId;
        private String screenCode;
        private Permission permission;

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Key key)) return false;
            return Objects.equals(templateId, key.templateId)
                    && Objects.equals(screenCode, key.screenCode)
                    && permission == key.permission;
        }

        @Override
        public int hashCode() {
            return Objects.hash(templateId, screenCode, permission);
        }
    }
}
