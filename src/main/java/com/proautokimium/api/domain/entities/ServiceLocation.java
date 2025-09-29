package com.proautokimium.api.domain.entities;

import com.proautokimium.api.domain.valueObjects.Email;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("SERVICE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceLocation extends Partner {
    @Column(name = "address")
    private String address;

    public ServiceLocation(String systemCode, String documento, String nome, Email email, boolean ativo, String address){
        super(systemCode,documento,nome,email,ativo);
        this.address = address;
    }
}
