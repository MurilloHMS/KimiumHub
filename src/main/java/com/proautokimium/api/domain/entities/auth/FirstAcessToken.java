package com.proautokimium.api.domain.entities.auth;

import com.proautokimium.api.domain.entities.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "first_access_token")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FirstAcessToken extends com.proautokimium.api.domain.abstractions.Entity{
    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne
    private Employee employee;

    @Column(name = "expires_at")
    private LocalDateTime expiration;

    private boolean used = false;
}
