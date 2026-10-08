package com.proautokimium.api.Application.DTOs.user;

import com.proautokimium.api.domain.enums.UserRole;

import java.util.Collection;
import java.util.List;

/**
 * Uma conta, do jeito que a tela de administração a lista.
 *
 * `codParceiro` e `employeeName` são do funcionário vinculado, ou null se não
 * houver vínculo. `templates` são os modelos de permissão já aplicados — os
 * chips da lista. `developer` e `client` decidem o que a tela oferece: a grade
 * não abre para nenhum dos dois.
 */
public record UserResponseDTO(String id,
                              String login,
                              String email,
                              Collection<UserRole> roles,
                              String codParceiro,
                              String employeeName,
                              boolean active,
                              boolean developer,
                              boolean client,
                              List<String> templates) {
}
