package com.proautokimium.api.domain.entities.permission;

import com.proautokimium.api.domain.enums.Permission;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

/**
 * O que uma pessoa pode, e **é aqui que mora a verdade**.
 *
 * Nada é herdado em tempo de leitura: o modelo já carimbou, e o que ficou
 * escrito aqui é o que vale. Por isso "o que o João pode?" é uma consulta a
 * esta tabela e mais nada.
 *
 * `userId` é `String`, e não `UUID`: a entidade `User` declara `String id` com
 * `@GeneratedValue(UUID)`, e o Hibernate gravou como texto. Declarar `UUID`
 * aqui faria a leitura estourar em tempo de execução, não de compilação.
 */
@Entity
@Table(name = "user_permissions")
@IdClass(UserPermission.Key.class)
@Getter
@Setter
@NoArgsConstructor
public class UserPermission {

    @Id
    @Column(name = "user_id")
    private String userId;

    @Id
    @Column(name = "screen_code", length = 120)
    private String screenCode;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "permission", length = 20)
    private Permission permission;

    @Column(name = "allowed", nullable = false)
    private boolean allowed;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Key implements Serializable {
        private String userId;
        private String screenCode;
        private Permission permission;

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Key key)) return false;
            return Objects.equals(userId, key.userId)
                    && Objects.equals(screenCode, key.screenCode)
                    && permission == key.permission;
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, screenCode, permission);
        }
    }
}