package com.proautokimium.api.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("CLIENTE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Customer extends Partner {
    @Column(name = "recebe_email", nullable = false)
    private boolean recebeEmail;
    @Column(name = "codigo_matriz", length = 9)
    private String codigoMatriz;
}
