package com.proautokimium.api.domain.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.proautokimium.api.domain.enums.MachineStatus;
import com.proautokimium.api.domain.enums.MachineType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@DiscriminatorValue("MACHINE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductMachine extends ProductEntity{

    @Column(name = "minimum_stock")
    private int minimum_stock;

    @Column(name = "brand", length = 100)
    private String brand;

    @Enumerated(EnumType.STRING)
    @Column(name = "machine_status", length = 15)
    private MachineStatus machineStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "machine_type", length = 10)
    private MachineType machineType;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @OneToMany(mappedBy = "machine", fetch = FetchType.LAZY)
    private Set<MovementMachine> movements = new HashSet<>();
}
