package com.proautokimium.api.domain.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "revision")
public class Revision extends com.proautokimium.api.domain.abstractions.Entity implements Serializable {
    private static final long serialVersionUID =1L;

    @Column(name = "revision_date", nullable = false)
    private LocalDate revisionDate;

    @ManyToOne
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;
}
