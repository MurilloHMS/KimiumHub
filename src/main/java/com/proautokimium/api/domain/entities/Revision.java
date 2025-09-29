package com.proautokimium.api.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "revision")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Revision extends com.proautokimium.api.domain.abstractions.Entity implements Serializable {
    @Serial
    private static final long serialVersionUID =1L;

    @Column(name = "revision_date", nullable = false)
    private LocalDate revisionDate;

    @ManyToOne
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @Column(name = "kilometer")
    private int kilometer;

    @Column(name = "fiscal_note", length = 10)
    private String fiscalNote;

    @Column(name = "type")
    private String type;

    @Column(name = "driver_name")
    private String driver;

    @Column(name = "observation")
    private String observation;

    @ManyToOne
    @JoinColumn(name = "service_location_id")
    private ServiceLocation local;
}
