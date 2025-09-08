package com.proautokimium.api.domain.entities;

import com.proautokimium.api.Application.DTOs.cliente.CustomerRequestDTO;
import com.proautokimium.api.domain.valueObjects.Email;
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

    public Customer(String systemCode, String documento, String nome, Email email, boolean ativo, boolean recebeEmail, String codigoMatriz){
        super(systemCode, documento, nome, email, ativo);
        this.recebeEmail = recebeEmail;
        this.codigoMatriz = codigoMatriz;
    }
    public static Customer fromDTO(CustomerRequestDTO dto){
        return new Customer(
                dto.codParceiro(),
                dto.documento(),
                dto.nome(),
                new Email(dto.email()),
                dto.ativo(),
                dto.recebeEmail(),
                dto.codMatriz());
    }
}
