package com.proautokimium.api.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Um tipo de holerite: Salário, Adiantamento, PLR, Férias coletivas…
 *
 * Era um enum, e cada tipo novo pedia código nos dois repositórios e deploy.
 * O RH pediu o PLR numa manhã (2026-10-02); agora ele cria o tipo pela tela.
 *
 * O {@link #code} é o que o holerite guarda (`holerite_documento.tipo`, com FK)
 * e o que vai no nome do arquivo — por isso só A-Z, 0-9 e `_`. O {@link #label}
 * é o que as pessoas leem.
 */
@jakarta.persistence.Entity
@Table(name = "payslip_types")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PayslipType extends com.proautokimium.api.domain.abstractions.Entity {

    public static final int CODE_MAX = 40;
    public static final int LABEL_MAX = 60;

    @Column(name = "code", nullable = false, unique = true, length = CODE_MAX)
    private String code;

    @Column(name = "label", nullable = false, length = LABEL_MAX)
    private String label;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Login de quem criou. Nulo nos que a migration semeou. */
    @Column(name = "created_by", length = 120)
    private String createdBy;

    public PayslipType(String code, String label, int sortOrder, String createdBy, LocalDateTime createdAt) {
        this.code = code;
        this.label = label;
        this.sortOrder = sortOrder;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }
}
