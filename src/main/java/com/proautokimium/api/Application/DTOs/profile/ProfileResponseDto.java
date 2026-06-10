package com.proautokimium.api.Application.DTOs.profile;

import java.util.List;
import java.util.UUID;

public record ProfileResponseDto(
        UUID id,
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
