package com.proautokimium.api.domain.entities.auth;

import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.Partner;
import jakarta.persistence.*;
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
public class FirstAccessToken extends com.proautokimium.api.domain.abstractions.Entity{
    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne
    @JoinColumn(name = "partner_id")
    private Partner partner;

    private String email;

    @Column(name = "expires_at")
    private LocalDateTime expiration;

    private boolean used = false;

    // Methods
    public boolean isValid(LocalDateTime now){
        return this.expiration.isAfter(now) && !this.used;
    }

    public void markUsed(){
        this.used = true;
    }
}
