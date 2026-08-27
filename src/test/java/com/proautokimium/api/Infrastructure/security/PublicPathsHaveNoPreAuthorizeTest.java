package com.proautokimium.api.Infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * **Rota pública não pode ter `@PreAuthorize`.**
 *
 * O `permitAll` do `SecurityPaths` só solta a requisição no filtro. A segurança
 * de método é outra camada, e roda depois: com `@PreAuthorize` no método, a
 * requisição anônima passa pelo filtro e morre no controller.
 *
 * Aconteceu em 2026-08-27, no passo 5. Anotei `POST /api/certificate` e
 * `/api/certificate/no-validation`, que estão em `PUBLIC_POST`, e **os testes
 * continuaram verdes** — porque eu tinha dado `@WithMockUser` a eles no mesmo
 * lote. O formulário público do site teria quebrado no deploy, sem sinal antes.
 *
 * Varre o código-fonte, e não o classpath: assim dá para ler a rota literal de
 * cada método sem subir contexto nenhum.
 */
class PublicPathsHaveNoPreAuthorizeTest {

    private static final Path CONTROLLERS = Path.of(
            "src/main/java/com/proautokimium/api/controllers");

    /**
     * A assinatura de um endpoint.
     *
     * Casa `public ResponseEntity<List<X>> nome(` e companhia — o retorno pode
     * ter genéricos, vírgulas e pontos.
     */
    private static final Pattern ASSINATURA = Pattern.compile(
            "^\\s*(?:public|protected)\\s+[\\w<>,\\[\\]?\\s.]+\\s+(\\w+)\\s*\\(");

    /** Uma rota anotada, do jeito que o código a escreve. */
    private record Anotado(String arquivo, String metodo, String rota, String verbo) { }

    @Test
    @DisplayName("nenhuma rota do SecurityPaths tem @PreAuthorize")
    void rotaPublicaNaoTemPreAuthorize() throws IOException {
        List<Anotado> anotados = varrer();

        assertThat(anotados)
                .as("a varredura precisa achar anotações, senão este teste não prova nada")
                .isNotEmpty();

        List<String> conflitos = new ArrayList<>();
        for (Anotado a : anotados) {
            for (String publica : publicasDoVerbo(a.verbo())) {
                if (casa(a.rota(), publica)) {
                    conflitos.add(a.arquivo() + "." + a.metodo()
                            + " (" + a.verbo() + " " + a.rota() + ") casa com " + publica);
                }
            }
        }

        assertThat(conflitos)
                .as("rota pública com @PreAuthorize passa pelo filtro e morre no controller "
                        + "— o sintoma é 403 em quem nunca fez login")
                .isEmpty();
    }

    private static List<String> publicasDoVerbo(String verbo) {
        List<String> todas = new ArrayList<>();
        if ("GET".equals(verbo)) todas.addAll(Arrays.asList(SecurityPaths.PUBLIC_GET));
        if ("POST".equals(verbo)) todas.addAll(Arrays.asList(SecurityPaths.PUBLIC_POST));
        return todas;
    }

    /** O bastante do casamento do Spring para o que está no `SecurityPaths` hoje. */
    private static boolean casa(String rota, String padrao) {
        if (padrao.endsWith("/**")) {
            return rota.startsWith(padrao.substring(0, padrao.length() - 3));
        }
        return rota.equals(padrao);
    }

    // ─── A varredura ─────────────────────────────────────────────────────────
    //
    // Por BLOCO DE MÉTODO, de trás para frente: a partir da assinatura, sobe
    // recolhendo anotações até achar linha em branco, chave ou comentário.
    //
    // A primeira versão dividia o arquivo nos `@...Mapping` e olhava o que vinha
    // depois. Passou verde com o defeito de volta — neste projeto o
    // `@PreAuthorize` aparece dos dois lados do mapping, e quando vem ANTES cai
    // no bloco do método anterior.

    private static List<Anotado> varrer() throws IOException {
        List<Anotado> achados = new ArrayList<>();

        try (var arquivos = Files.walk(CONTROLLERS)) {
            for (Path arquivo : arquivos.filter(x -> x.toString().endsWith(".java")).toList()) {
                List<String> linhas = Files.readAllLines(arquivo);

                String base = entreAspas(String.join("\n", linhas), "@RequestMapping(");
                if (base == null) {
                    base = "";
                }
                if (!base.isEmpty() && !base.startsWith("/")) {
                    base = "/" + base;
                }

                for (int i = 0; i < linhas.size(); i++) {
                    Matcher m = ASSINATURA.matcher(linhas.get(i));
                    if (!m.find()) {
                        continue;
                    }

                    String bloco = anotacoesAcima(linhas, i);
                    if (!bloco.contains("@PreAuthorize")) {
                        continue;
                    }

                    String verbo = verboDe(bloco);
                    if (verbo == null) {
                        continue;
                    }

                    String sufixo = entreAspas(bloco, verbo + "Mapping(");
                    if (sufixo == null) {
                        sufixo = "";
                    }
                    if (!sufixo.isEmpty() && !sufixo.startsWith("/")) {
                        sufixo = "/" + sufixo;
                    }

                    achados.add(new Anotado(arquivo.getFileName().toString(),
                            m.group(1), base + sufixo, verbo.toUpperCase()));
                }
            }
        }
        return achados;
    }

    private static String anotacoesAcima(List<String> linhas, int assinatura) {
        List<String> bloco = new ArrayList<>();
        for (int j = assinatura - 1; j >= 0; j--) {
            String anterior = linhas.get(j).strip();
            boolean fimDoBloco = anterior.isEmpty()
                    || anterior.equals("{") || anterior.equals("}")
                    || anterior.startsWith("*") || anterior.startsWith("/*")
                    || anterior.startsWith("//");
            if (fimDoBloco) {
                break;
            }
            bloco.add(linhas.get(j));
        }
        return String.join("\n", bloco);
    }

    private static String verboDe(String bloco) {
        for (String verbo : List.of("Get", "Post", "Put", "Delete", "Patch")) {
            if (bloco.contains("@" + verbo + "Mapping")) {
                return verbo;
            }
        }
        return null;
    }

    /** O primeiro literal entre aspas depois de `depoisDe`, se estiver na mesma chamada. */
    private static String entreAspas(String texto, String depoisDe) {
        int i = texto.indexOf(depoisDe);
        if (i < 0) {
            return null;
        }
        int fimDaChamada = texto.indexOf(')', i);
        int abre = texto.indexOf('"', i);
        if (abre < 0 || (fimDaChamada >= 0 && abre > fimDaChamada)) {
            return null;
        }
        return texto.substring(abre + 1, texto.indexOf('"', abre + 1));
    }
}
