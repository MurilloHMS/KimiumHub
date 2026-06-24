package com.proautokimium.api.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "holerite_documento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HoleriteDocumento extends com.proautokimium.api.domain.abstractions.Entity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private LocalDate competencia;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "storage_path", length = 500, nullable = false)
    private String storagePath;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public HoleriteDocumento(Employee employee, LocalDate competencia, String originalFilename, String storagePath) {
        this.employee = employee;
        this.competencia = competencia;
        this.originalFilename = originalFilename;
        this.storagePath = storagePath;
        this.createdAt = LocalDateTime.now();
    }
}
