package com.proautokimium.api.domain.entities;

import com.proautokimium.api.domain.enums.ContactStatus;
import com.proautokimium.api.domain.enums.ContactType;
import com.proautokimium.api.domain.valueObjects.Email;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "contact")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Contact extends com.proautokimium.api.domain.abstractions.Entity{
    @Column(name = "name")
    private String name;
    @Embedded
    @AttributeOverride(name = "address", column = @Column(name = "email", length = 200, nullable = false))
    private Email email;
    @Column(name = "contactType")
    private ContactType contactType;
    @Column(name = "otherContactType")
    private String other;
    @Column(name = "message")
    private String message;
    @Column(name = "businessName")
    private String businessName;
    @Column(name = "contactStatus")
    private ContactStatus contactStatus;
    @Column(name = "contactDate")
    private LocalDateTime contactDate;
}
