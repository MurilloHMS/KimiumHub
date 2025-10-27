package com.proautokimium.api.domain.entities;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("VENDEDOR")
@Getter
@Setter
public class Seller extends Employee {
}
