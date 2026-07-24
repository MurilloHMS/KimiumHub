package com.proautokimium.api.domain.entities.humanResources;

import com.proautokimium.api.domain.entities.Employee;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@jakarta.persistence.Entity
@Table(name = "equipment_assignments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EquipmentAssignment extends com.proautokimium.api.domain.abstractions.Entity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "equipment_type", length = 100, nullable = false)
    private String equipmentType;

    @Column(name = "description", length = 300)
    private String description;

    @Column(name = "delivered_at", nullable = false)
    private LocalDate deliveredAt;

    @Column(name = "returned_at")
    private LocalDate returnedAt;

    @Column(name = "notes", length = 500)
    private String notes;

    private EquipmentAssignment(Employee employee, String equipmentType, String description,
                                 LocalDate deliveredAt, String notes) {
        this.employee = employee;
        this.equipmentType = equipmentType;
        this.description = description;
        this.deliveredAt = deliveredAt;
        this.notes = notes;
    }

    public static EquipmentAssignment deliver(Employee employee, String equipmentType, String description,
                                               LocalDate deliveredAt, String notes) {
        return new EquipmentAssignment(employee, equipmentType, description, deliveredAt, notes);
    }

    public void markAsReturned(LocalDate returnedAt) {
        if (this.returnedAt != null) {
            throw new IllegalStateException("Equipamento já foi devolvido");
        }
        if (returnedAt.isBefore(this.deliveredAt)) {
            throw new IllegalArgumentException("Data de devolução não pode ser antes da entrega");
        }
        this.returnedAt = returnedAt;
    }

    public boolean isWithEmployee() {
        return returnedAt == null;
    }
}
