package com.proautokimium.api.domain.entities;

import com.proautokimium.api.Application.DTOs.partners.CustomerRequestDTO;
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
    @Column(name = "recebe_email")
    private boolean recebeEmail;
    @Column(name = "codigo_matriz", length = 9)
    private String codigoMatriz;
    /** Matriz do grupo: enxerga as próprias unidades no portal do cliente. */
    @Column(name = "is_matriz")
    private boolean isMatriz;

    public Customer(String systemCode, String documento, String nome, String username, Email email, boolean ativo, boolean recebeEmail, String codigoMatriz, boolean isMatriz) {
        super(systemCode, documento, nome, email, username ,ativo);
        this.recebeEmail = recebeEmail;
        this.codigoMatriz = codigoMatriz;
        this.isMatriz = isMatriz;
    }
    public static Customer fromDTO(CustomerRequestDTO dto){
        return new Customer(
                dto.codParceiro(),
                dto.documento(),
                dto.nome(),
                dto.username(),
                new Email(dto.email()),
                dto.ativo(),
                dto.recebeEmail(),
                dto.codMatriz(),
                dto.isMatriz());
    }
}
