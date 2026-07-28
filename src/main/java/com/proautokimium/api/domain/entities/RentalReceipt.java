package com.proautokimium.api.domain.entities;

import com.proautokimium.api.domain.enums.ReceiptType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@jakarta.persistence.Entity
@Table(name = "rental_receipts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RentalReceipt extends com.proautokimium.api.domain.abstractions.Entity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private RentalReceiptBatch batch;

    @Enumerated(EnumType.STRING)
    @Column(name = "receipt_type", nullable = false, length = 10)
    private ReceiptType receiptType;

    @Column(name = "cod_matriz", nullable = false, length = 50)
    private String codMatriz;

    @Column(name = "nome_matriz", nullable = false, length = 255)
    private String nomeMatriz;

    @Column(name = "num_nota", length = 50)
    private String numNota;

    @Column(name = "nome_parceiro", length = 255)
    private String nomeParceiro;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "total_unidades")
    private Integer totalUnidades;

    @Column(name = "total_maquinas")
    private Integer totalMaquinas;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
