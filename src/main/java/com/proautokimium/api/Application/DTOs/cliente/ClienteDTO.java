package com.proautokimium.api.Application.DTOs.cliente;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClienteDTO {
    private String codParceiro;
    private String documento;
    private String nome;
    private String email;
    private boolean ativo;
    private boolean recebeEmail;
}
