package com.proautokimium.api.domain.exceptions.partners;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

/**
 * A conta existe, e não está ligada a nenhum funcionário.
 *
 * Separada de `EmployeeNotFoundException` porque **não é a mesma coisa**: lá o
 * funcionário procurado não existe; aqui ele existe em algum lugar e ninguém
 * amarrou os dois. O 404 é o mesmo, a saída não — e quem lê "Funcionário não
 * encontrado" numa tela de perfil próprio conclui que o cadastro dele sumiu.
 *
 * A mensagem diz o que houve **e para quem pedir**. Uma tela que só informa o
 * problema deixa a pessoa parada; a diferença entre as duas é uma frase.
 */
public class AccountNotLinkedToEmployeeException extends DomainException {
    public AccountNotLinkedToEmployeeException() {
        super("Sua conta de acesso ainda não está vinculada ao seu cadastro de funcionário, "
                + "e o perfil precisa desse vínculo para existir. "
                + "Peça ao RH para vincular o seu login ao seu cadastro — depois disso a tela "
                + "abre normalmente.", HttpStatus.NOT_FOUND);
    }
}
