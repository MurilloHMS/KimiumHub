package com.proautokimium.api.domain.entities.prostock.machine;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "machine_alert_config")
@Getter
@Setter
public class MachineAlertConfig extends com.proautokimium.api.domain.abstractions.Entity {

    @Column(name = "active", nullable = false)
    private boolean active = false;

    @Column(name = "alert_when_late", nullable = false)
    private boolean alertWhenLate = true;

    @Column(name = "send_at", nullable = false)
    private LocalTime sendAt = LocalTime.of(8, 0);

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "machine_alert_days", joinColumns = @JoinColumn(name = "config_id"))
    @Column(name = "days_before", nullable = false)
    private List<Integer> daysBefore = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "machine_alert_recipients", joinColumns = @JoinColumn(name = "config_id"))
    @Column(name = "employee_id", nullable = false)
    private List<UUID> recipientEmployeeIds = new ArrayList<>();
}