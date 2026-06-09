package com.proautokimium.api.domain.models;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class RedeSocial {
    private String tipo;
    private String url;
}
