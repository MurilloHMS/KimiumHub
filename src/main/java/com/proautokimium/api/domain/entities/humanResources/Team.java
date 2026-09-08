package com.proautokimium.api.domain.entities.humanResources;

import com.proautokimium.api.domain.abstractions.Entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@jakarta.persistence.Entity
@Table(name = "teams")
@Getter
@NoArgsConstructor
public class Team extends Entity {

    @Column(name =  "name", length = 100, nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    public Team(String name, Department department){
        if(department == null){
            throw new IllegalArgumentException("Team must belong to a Department");
        }
        this.name = name;
        this.department = department;
    }

    /**
     * Renomeia o setor e/ou o move de departamento.
     *
     * Metodo de dominio em vez de `@Setter`: o setor sempre teve um construtor
     * que exige departamento, e um setter solto abriria a porta para setor sem
     * departamento — que a coluna nem aceita (`nullable = false`).
     *
     * **Mover de departamento move junto todo mundo que esta no setor**, ja que
     * o departamento do funcionario e lido pelo setor desde a migracao das FKs.
     */
    public void alterar(String name, Department department) {
        this.name = name;
        this.department = department;
    }
}
