package com.proautokimium.api.Application.DTOs.profile;

import java.util.List;

public record ProfileCreateDto(
        String nome,
        String slug,
        String cargo,
        String empresa,
        String email,
        String imagem,
        String descricao,
        List<TelefoneContatoDto> telefones,
        List<RedeSocialDto> redesSociais,
        List<String> regioesAtendimento,
        List<String> segmentosAtendimento,
        Boolean ativo
) {
}