package com.proautokimium.api.domain.entities;

import com.proautokimium.api.domain.enums.StatusPostagem;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table
@Getter
@Setter
@AllArgsConstructor
public class Faq extends com.proautokimium.api.domain.abstractions.Entity{
    @Column(name = "title", length = 150, nullable = false)
    private String title;
    @Column(name = "body", length = 500, nullable = false)
    private String body;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusPostagem status;

    public Faq(){
        this.status = StatusPostagem.RASCUNHO;
    }

    // Methods

    public void publicar(){
        this.status = StatusPostagem.PUBLICADO;
    }

    public void arquivar(){
        this.status = StatusPostagem.ARQUIVADO;
    }

    public void rascunho(){
        this.status = StatusPostagem.RASCUNHO;
    }
}
