package com.proautokimium.api.domain.exceptions.sales;

import com.proautokimium.api.domain.exceptions.DomainException;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * O checklist chegou com campo obrigatório vazio ou inválido.
 *
 * O celular já confere tudo antes de enviar; esta é a mesma regra do lado de cá,
 * para um aparelho com a versão antiga do site não gravar checklist incompleto.
 * A mensagem lista os problemas, porque é ela que aparece na tela.
 */
public class InvalidChecklistException extends DomainException {

    private final List<String> problems;

    public InvalidChecklistException(List<String> problems) {
        super(String.join(" ", problems), HttpStatus.BAD_REQUEST);
        this.problems = List.copyOf(problems);
    }

    public InvalidChecklistException(String problem) {
        this(List.of(problem));
    }

    public List<String> getProblems() {
        return problems;
    }
}
