package com.proautokimium.api.domain.entities.permission;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Um modelo de permissão — o carimbo.
 *
 * Não é um pai: aplicar um modelo **escreve** as permissões no usuário, e
 * aplicar outro em cima soma. Depois disso ele não tem mais poder nenhum sobre
 * quem já foi carimbado, e é por isso que a resolução da permissão efetiva não
 * passa por aqui.
 *
 * Foi a decisão que resolveu o vendedor que mexe em telas do estoque sem
 * precisar de um grupo "Vendas + Estoque".
 */
@Entity
@Table(name = "permission_templates")
@Getter
@Setter
@NoArgsConstructor
public class PermissionTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", length = 60, nullable = false, unique = true)
    private String name;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}