package com.proautokimium.api.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Entity
@Table(name ="equipment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentGuide extends com.proautokimium.api.domain.abstractions.Entity {
    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "nome", length = 50)
    private String nome;
    @Column(name = "imagem", length = 200)
    private String imagem;

    @ManyToMany(mappedBy = "equipmentGuides", fetch = FetchType.LAZY)
    private List<ProductWebsite> products;
}
