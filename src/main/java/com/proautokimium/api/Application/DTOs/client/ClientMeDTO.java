package com.proautokimium.api.Application.DTOs.client;

import java.util.List;

public record ClientMeDTO(String nome,
                          String codParceiro,
                          String documento,
                          boolean matriz,
                          List<ClientUnitDTO> unidades) {}