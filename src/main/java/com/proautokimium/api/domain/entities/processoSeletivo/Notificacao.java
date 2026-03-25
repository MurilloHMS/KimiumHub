package com.proautokimium.api.domain.entities.processoSeletivo;

import com.proautokimium.api.domain.enums.processoSeletivo.TipoNotificacao;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notificacao_processo_seletivo")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Notificacao extends com.proautokimium.api.domain.abstractions.Entity{
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidatura_id", nullable = false)
    private Candidatura candidatura;
    @Enumerated(EnumType.STRING)
    private TipoNotificacao tipo;
    private String Assunto;
    private String corpo;
    private Boolean enviado;
    private LocalDateTime enviadoEm;
}
