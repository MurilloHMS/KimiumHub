package com.proautokimium.api.domain.entities;

import com.proautokimium.api.domain.entities.auth.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@jakarta.persistence.Entity
@Table(name = "rental_receipt_batches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RentalReceiptBatch extends com.proautokimium.api.domain.abstractions.Entity {

    @Column(name = "reference_month", nullable = false, length = 20)
    private String referenceMonth;

    @Column(name = "reference_year", nullable = false)
    private int referenceYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by")
    private User generatedBy;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "source_filename", length = 255)
    private String sourceFilename;

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RentalReceipt> receipts = new ArrayList<>();
}
