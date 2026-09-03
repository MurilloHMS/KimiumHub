package com.proautokimium.api.Infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * **O health tem que ser público, e só ele.**
 *
 * O container consulta `/actuator/health` a cada 15 segundos para dizer se está
 * de pé. Sem `permitAll`, a requisição leva 401 — e o `wget -q` do healthcheck
 * engole o erro e sai com código 1 **sem imprimir nada**.
 *
 * Aconteceu em 2026-09-03, no primeiro deploy pelo Jenkins. A API subiu
 * perfeitamente (`Started ApiApplication in 18.6s`, Tomcat na 8080), o
 * healthcheck acumulou nove falhas de saída vazia, o pipeline concluiu que o
 * deploy tinha dado errado e **reverteu uma versão que estava funcionando**.
 *
 * Falso negativo que desfaz deploy bom é pior que não verificar nada, e o
 * sintoma não apontava para segurança em lugar nenhum.
 *
 * O segundo teste é o par que protege o primeiro: liberar `/actuator/**`
 * resolveria o healthcheck e abriria `/env`, `/beans` e `/heapdump` no dia em
 * que alguém ligasse outro endpoint — sem ninguém revisitar esta decisão.
 */
class ActuatorHealthIsPublicTest {

    private static final Path PATHS = Path.of(
            "src/main/java/com/proautokimium/api/Infrastructure/security/SecurityPaths.java");

    @Test
    @DisplayName("/actuator/health está liberado para GET anônimo")
    void healthEhPublico() {
        assertThat(SecurityPaths.PUBLIC_GET)
                .withFailMessage("""
                        `/actuator/health` saiu de PUBLIC_GET.

                        O healthcheck do container vai levar 401, falhar com saída
                        vazia, e o pipeline vai reverter deploys que deram certo.""")
                .contains("/actuator/health");
    }

    @Test
    @DisplayName("nenhum caminho público abre o actuator inteiro")
    void naoLiberaOActuatorInteiro() {
        var todos = Arrays.asList(
                String.join(",", SecurityPaths.PUBLIC_GET),
                String.join(",", SecurityPaths.PUBLIC_POST),
                String.join(",", SecurityPaths.SWAGGER));

        assertThat(todos)
                .withFailMessage("""
                        Algum caminho público casa com o actuator inteiro.

                        `/actuator/**` expõe `/env`, `/beans` e `/heapdump` — hoje
                        eles estão desligados, mas o curinga os abriria no dia em que
                        alguém ligasse um deles sem lembrar desta decisão.

                        Liberar endpoint por endpoint.""")
                .noneMatch(lista -> lista.contains("/actuator/*")
                        || lista.contains("/actuator/**"));
    }

    /**
     * O `permitAll` do filtro é só a primeira camada. Se o health for exposto
     * com detalhes, ele passa a contar estado de banco, disco e broker para
     * quem alcançar a porta — e aí ser público deixa de ser inofensivo.
     */
    @Test
    @DisplayName("o health público não detalha banco, disco nem broker")
    void healthNaoDetalha() throws IOException {
        for (var perfil : new String[] { "prod", "dev" }) {
            var conteudo = Files.readString(
                    Path.of("src/main/resources/application-" + perfil + ".properties"));

            assertThat(conteudo)
                    .withFailMessage("""
                            O perfil %s não limita os detalhes do health.

                            Sem `show-details=never`, um endpoint público conta o
                            estado do banco, do disco e do broker. O container só
                            precisa do "UP".""", perfil)
                    .contains("management.endpoint.health.show-details=never");
        }
    }

    /** A constante existe mesmo, e não foi renomeada por baixo do teste. */
    @Test
    @DisplayName("o arquivo de caminhos continua onde o teste espera")
    void arquivoExiste() {
        assertThat(PATHS).exists();
    }
}
