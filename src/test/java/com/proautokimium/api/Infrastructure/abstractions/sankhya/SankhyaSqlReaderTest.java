package com.proautokimium.api.Infrastructure.abstractions.sankhya;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Infrastructure.services.sankhya.SankhyaQueryService;
import com.proautokimium.api.Infrastructure.utils.LinhaSankhya;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A casca compartilhada dos leitores de SQL do Sankhya.
 *
 * <p>É um Template Method: a base é dona da <b>ordem</b> — carrega, troca o
 * cabeçalho, consulta, traduz — e a subclasse é dona dos <b>passos</b>, que aqui
 * são só dois: a pasta e a lista de arquivos.
 *
 * <p><b>O que estes testes protegem não aparece em nenhuma tela.</b> Um leitor
 * que manda o cabeçalho sem o corpo, ou que empilha dois {@code DECLARE} da
 * mesma variável, compila e passa em qualquer teste que não olhe o texto
 * enviado. O erro só acontece contra o ERP — e a mensagem que volta fala de
 * sintaxe, não do defeito.
 */
@ExtendWith(MockitoExtension.class)
class SankhyaSqlReaderTest {

    @Mock SankhyaQueryService queryService;

    private LeitorDeTeste leitor;

    /** Uma subclasse mínima, apontando para os SQL que já existem no projeto. */
    private static final class LeitorDeTeste extends SankhyaSqlReader {
        LeitorDeTeste(SankhyaQueryService q, ObjectMapper m) { super(q, m); }
        @Override protected String directory() { return "sql/partners/"; }
        @Override protected List<String> files() { return List.of("customers"); }

        String montar(String declare) { return withParameters("customers", declare); }
        List<LinhaSankhya> rodar(String declare) { return execute("customers", declare); }
    }

    @BeforeEach
    void setUp() {
        leitor = new LeitorDeTeste(queryService, new ObjectMapper());
        leitor.loadSql();
    }

    // ── O que a base tem que garantir ─────────────────────────────────────────

    /**
     * <b>O corpo do SQL vai junto.</b>
     *
     * <p>Escrito porque a primeira versão mandava só o cabeçalho: o parâmetro do
     * arquivo não era usado, o {@code loadSql} carregava arquivos que ninguém
     * lia, e a consulta que chegava ao ERP era uma linha de {@code DECLARE} sem
     * {@code SELECT} nenhum. Compilava.
     */
    @Test
    @DisplayName("manda o corpo do SQL, e não só o cabeçalho")
    void sendsTheSqlBody() {
        when(queryService.query(anyString())).thenReturn("{\"fieldsMetadata\":[],\"rows\":[]}");

        leitor.rodar("DECLARE @DESDE DATE='2025-09-09';");

        ArgumentCaptor<String> enviado = ArgumentCaptor.forClass(String.class);
        verify(queryService).query(enviado.capture());

        assertThat(enviado.getValue())
                .as("sem o corpo, o ERP recebe um DECLARE solto")
                .contains("SELECT")
                .contains("FROM TGFPAR");
    }

    /**
     * <b>O cabeçalho é trocado, não empilhado.</b>
     *
     * <p>Os arquivos já trazem um {@code DECLARE} próprio — foi assim que eles
     * rodaram na medição contra a base. Prefixar outro dá
     * "The variable name '@DESDE' has already been declared", e isso só aparece
     * contra o ERP de verdade.
     */
    @Test
    @DisplayName("troca o DECLARE do arquivo em vez de empilhar outro")
    void replacesTheHeader() {
        String sql = leitor.montar("DECLARE @DESDE DATE='2025-09-09';");

        assertThat(contar(sql, "@DESDE DATE"))
                .as("dois DECLARE da mesma variável e o SQL Server recusa a consulta inteira")
                .isEqualTo(1);
        assertThat(sql).startsWith("DECLARE @DESDE DATE='2025-09-09';");
        assertThat(sql).doesNotContain("2025-09-09'\nDECLARE");
    }

    /**
     * O cabeçalho e o corpo não podem ficar grudados numa instrução só.
     *
     * <p>Hoje quem garante isso é o {@code ;} que a subclasse põe no fim do
     * cabeçalho. Este teste existe para quem escrever a próxima subclasse ver
     * que o {@code ;} não é enfeite: sem ele, um arquivo cujo corpo comece com
     * {@code SELECT} vira {@code DECLARE ...'SELECT} — erro de sintaxe.
     */
    @Test
    @DisplayName("o cabeçalho termina em ; e o corpo começa depois dele")
    void headerIsTerminated() {
        String sql = leitor.montar("DECLARE @DESDE DATE='2025-09-09';");

        int fimDoCabecalho = sql.indexOf(';');
        assertThat(fimDoCabecalho).isGreaterThan(0);
        assertThat(sql.substring(0, fimDoCabecalho + 1))
                .as("o que vem antes do ponto-e-vírgula é o DECLARE inteiro")
                .isEqualTo("DECLARE @DESDE DATE='2025-09-09';");
    }

    @Test
    @DisplayName("o corpo do arquivo chega inteiro")
    void keepsTheWholeBody() {
        String sql = leitor.montar("DECLARE @DESDE DATE='x';");

        assertThat(sql).contains("PAR.CLIENTE = 'S'");
        assertThat(sql).contains("UNION");
        assertThat(sql).contains("ORDER BY PAR.CODPARC");
    }

    /** Arquivo que não existe é erro de programação, e a mensagem tem que dizer qual. */
    @Test
    @DisplayName("consulta desconhecida falha dizendo o nome")
    void unknownFileFails() {
        assertThatThrownBy(() -> leitor.withParameters("nao_existe", "DECLARE @X INT=1;"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao_existe");
    }

    /**
     * A subclasse não é chamada para consultar: ela só diz onde estão os
     * arquivos. Se {@code loadSql} não usar o que ela respondeu, o mapa fica
     * vazio e toda consulta vira "Consulta desconhecida".
     */
    @Test
    @DisplayName("carrega a pasta e os arquivos que a subclasse apontou")
    void loadsWhatTheSubclassPointedAt() {
        assertThat(leitor.montar("DECLARE @X INT=1;")).isNotBlank();
    }

    private int contar(String texto, String trecho) {
        int total = 0, i = 0;
        while ((i = texto.indexOf(trecho, i)) >= 0) { total++; i += trecho.length(); }
        return total;
    }
}
