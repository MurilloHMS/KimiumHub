package com.proautokimium.api.domain.entities;

import com.proautokimium.api.domain.valueObjects.Email;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Entity
@Table(name = "smtp_emails")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailEntity extends com.proautokimium.api.domain.abstractions.Entity implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "name", nullable = false)
    private String name;

    @Embedded
    @AttributeOverride(name = "address", column = @Column(name = "email", length = 60, nullable = false))
    private Email email;
}
