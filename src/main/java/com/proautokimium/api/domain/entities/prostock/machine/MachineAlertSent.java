package com.proautokimium.api.domain.entities.prostock.machine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "machine_alert_sent")
@Getter
@Setter
public class MachineAlertSent extends com.proautokimium.api.domain.abstractions.Entity {

    @Column(name = "register_id", nullable = false)
    private UUID registerId;

    @Column(name = "alert_date", nullable = false)
    private LocalDate alertDate;

    @Column(name = "days_before", nullable = false)
    private int daysBefore;

    protected MachineAlertSent() { }

    public MachineAlertSent(UUID registerId, LocalDate alertDate, int daysBefore) {
        this.registerId = registerId;
        this.alertDate = alertDate;
        this.daysBefore = daysBefore;
    }
}