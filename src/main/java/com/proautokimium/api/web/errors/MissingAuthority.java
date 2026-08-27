package com.proautokimium.api.web.errors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.authorization.ExpressionAuthorizationDecision;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Descobre **qual permissão faltou** num 403.
 *
 * O Spring responde "Access Denied" e mais nada. Numa API com 228 endpoints e
 * permissão por tela, isso é o defeito operacional mais caro que existe: o
 * mapeamento errado tira o trabalho de alguém no meio do expediente, e o
 * sintoma não diz nem em que tela procurar. Quem recebe abre um chamado
 * dizendo "não consigo salvar" — e a investigação começa do zero.
 *
 * O `@PreAuthorize` guarda a própria expressão na decisão, então dá para lê-la
 * de volta e dizer a authority pelo nome. É o passo 5 inteiro dependendo de uma
 * frase.
 */
final class MissingAuthority {

    private MissingAuthority() { }

    /** As strings entre aspas simples de `hasAuthority('x')` e `hasAnyAuthority('x','y')`. */
    private static final Pattern LITERAIS = Pattern.compile("'([^']+)'");

    /**
     * As authorities exigidas pelo endpoint, se der para saber.
     *
     * Devolve vazio quando a recusa não veio de uma expressão — a regra global
     * do `SecurityConfiguration`, por exemplo. Nesse caso não há nome a dizer, e
     * inventar um seria pior que a mensagem genérica.
     */
    static List<String> of(AccessDeniedException ex) {
        if (!(ex instanceof AuthorizationDeniedException negada)) return List.of();

        AuthorizationResult resultado = negada.getAuthorizationResult();
        if (!(resultado instanceof ExpressionAuthorizationDecision decisao)) return List.of();

        String expressao = decisao.getExpression().getExpressionString();

        List<String> authorities = new ArrayList<>();
        Matcher m = LITERAIS.matcher(expressao);
        while (m.find()) {
            String valor = m.group(1);
            // `hasRole('ADMIN')` também casa aqui, e é informação útil do mesmo
            // jeito: dizer "falta ADMIN" resolve o chamado igual.
            authorities.add(valor);
        }
        return authorities;
    }

    /**
     * A frase que vai no corpo do 403.
     *
     * Diz a permissão **e o que ela significa**: `stock/movements:EXCLUIR` vira
     * "Excluir em stock/movements". Sem essa tradução, quem lê o erro precisa
     * saber de cor o formato da authority — e quem lê o erro costuma ser
     * justamente quem não sabe.
     */
    static String message(AccessDeniedException ex) {
        List<String> faltando = of(ex);
        if (faltando.isEmpty()) {
            return "Você não tem permissão para esta ação.";
        }

        String lista = faltando.stream().map(MissingAuthority::humanize).toList()
                .toString().replace("[", "").replace("]", "");

        return faltando.size() == 1
                ? "Você não tem permissão para esta ação. Falta: " + lista + "."
                : "Você não tem permissão para esta ação. Falta uma destas: " + lista + ".";
    }

    /** `stock/movements:EXCLUIR` → `EXCLUIR em stock/movements (stock/movements:EXCLUIR)`. */
    private static String humanize(String authority) {
        int corte = authority.lastIndexOf(':');
        if (corte < 0) return authority;

        String tela = authority.substring(0, corte);
        String acao = authority.substring(corte + 1);
        return acao + " em " + tela + " (" + authority + ")";
    }
}
