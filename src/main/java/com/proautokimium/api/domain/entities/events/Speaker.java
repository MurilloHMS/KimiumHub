package com.proautokimium.api.domain.entities.events;

import com.proautokimium.api.domain.abstractions.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Quem palestra. Cadastrado uma vez e escolhido nas palestras de qualquer
 * evento — quem volta na edição seguinte não é digitado de novo.
 *
 * <p>Instagram e LinkedIn guardam só o usuário ({@code marina.quimica},
 * {@code marinaalves}), e o site monta o link. Guardar a URL inteira daria três
 * formatos para o mesmo perfil, e o link quebraria no primeiro com {@code www}.
 */
@jakarta.persistence.Entity
@Table(name = "speakers")
@Getter
@Setter
@NoArgsConstructor
public class Speaker extends Entity {

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "role", length = 120)
    private String role;

    @Column(name = "company_name", length = 120)
    private String companyName;

    @Column(name = "photo_url", length = 255)
    private String photoUrl;

    @Column(name = "instagram", length = 100)
    private String instagram;

    @Column(name = "linkedin", length = 100)
    private String linkedin;

    @Column(name = "website", length = 255)
    private String website;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;
}
