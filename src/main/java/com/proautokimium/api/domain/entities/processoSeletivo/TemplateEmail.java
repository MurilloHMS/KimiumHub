package com.proautokimium.api.domain.entities.processoSeletivo;

import com.proautokimium.api.domain.enums.processoSeletivo.Etapa;
import com.proautokimium.api.domain.enums.processoSeletivo.TipoNotificacao;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "template_email")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TemplateEmail extends com.proautokimium.api.domain.abstractions.Entity{
    @Enumerated(EnumType.STRING)
    private TipoNotificacao tipo;
    @Enumerated(EnumType.STRING)
    private Etapa etapa;
    private String assunto;
    private String corpo;
    private Boolean ativo;
}
