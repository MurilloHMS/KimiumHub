package com.proautokimium.api.domain.entities.prostock;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.proautokimium.api.domain.entities.ProductEntity;
import com.proautokimium.api.domain.enums.MachineStatus;
import com.proautokimium.api.domain.enums.MachineType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.util.HashSet;
import java.util.Set;

@Entity
@DiscriminatorValue("INVENTORY")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductInventory extends ProductEntity {
	@Serial
    private static final long serialVersionUID = 1L;

	@Column(name = "minimum_stock")
    private int minimumStock;

    /**
     * Marca o produto que também é máquina.
     *
     * É flag e não `type` de propósito: o discriminador é exclusivo, e ser
     * máquina não pode custar deixar de ser produto de estoque — era isso que
     * deixava a máquina fora da tela de movimentações.
     *
     * O @JsonProperty não é decoração: o Lombok gera `isMachine()` e o Jackson
     * batizaria a propriedade de "machine", quebrando o filtro do front sem
     * erro em lugar nenhum.
     *
     * Sem `nullable = false` aqui: é coluna de subclasse numa tabela
     * compartilhada, e o DEFAULT FALSE da migration é quem garante o valor.
     */
    @JsonProperty("isMachine")
    @Column(name = "is_machine")
    private boolean isMachine = false;

    /*
     * Campos que só valem para o produto marcado como máquina. Nulos nos
     * demais — e por isso anuláveis: as colunas já existem em `products` desde
     * a V26, o que muda é que elas deixaram de pertencer a uma subclasse.
     */
    @Column(name = "brand", length = 100)
    private String brand;

    @Enumerated(EnumType.STRING)
    @Column(name = "machine_type", length = 10)
    private MachineType machineType;

    @Enumerated(EnumType.STRING)
    @Column(name = "machine_status", length = 30)
    private MachineStatus machineStatus;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private Set<MovementInventory> movements = new HashSet<>();
}
