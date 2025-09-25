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
    @Column(name = "name", length = 150)
    private String name;
    @Embedded
    @AttributeOverride(name = "address", column = @Column(name = "email", length = 200, nullable = false))
    private Email email;
    @Enumerated(EnumType.STRING)
    @Column(name = "contact_type", length = 60)
    private ContactType contactType;
    @Column(name = "other_contact_type", length = 100)
    private String other;
    @Column(name = "message")
    private String message;
    @Column(name = "business_name", length = 200)
    private String businessName;
    @Enumerated(EnumType.STRING)
    @Column(name = "contact_status", length = 100)
    private ContactStatus contactStatus;
    @Column(name = "contact_date")
    private LocalDateTime contactDate;
}
