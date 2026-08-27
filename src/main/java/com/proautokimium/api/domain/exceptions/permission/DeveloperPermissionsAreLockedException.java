package com.proautokimium.api.domain.exceptions.permission;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

/**
 * A conta de desenvolvedor não perde permissão pela interface.
 *
 * Recusar aqui, e não deixar gravar em silêncio, é o ponto: o desenvolvedor tem
 * todas as authorities por resolução, então mexer na grade dele **não mudaria
 * nada** — e uma tela que aceita o clique, confirma "gravado" e não muda nada é
 * pior que uma que diz não.
 *
 * O que isto protege é o mesmo impasse da V87: sem uma conta que não se tranca,
 * um "bloquear tudo" na pessoa errada fecha a tela de permissões para todo
 * mundo, e a saída volta a ser `UPDATE` no banco.
 */
public class DeveloperPermissionsAreLockedException extends DomainException {
    public DeveloperPermissionsAreLockedException() {
        super("Esta é uma conta de desenvolvedor: ela tem acesso a tudo por definição, "
                + "e não pode perder permissão pela tela. É o que garante que sempre "
                + "exista alguém capaz de reabrir o sistema.", HttpStatus.CONFLICT);
    }
}
