package com.proautokimium.api.domain.entities;

import com.proautokimium.api.domain.valueObjects.Email;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "holder")
@Table(name = "certificate_holder")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CertificateHolder extends com.proautokimium.api.domain.abstractions.Entity {

    @Column(name = "name", length = 100, nullable = false, unique = true)
    private String name;

    @Column(name = "cellphone", length = 11, nullable = false)
    private String cellphone;

    @Embedded
    @AttributeOverride(name = "address", column = @Column(name = "email", length = 60, nullable = false))
    private Email email;
}
