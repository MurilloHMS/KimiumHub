package com.proautokimium.api.Infrastructure.services.newsletter.sankhya;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Infrastructure.services.sankhya.SankhyaQueryService;
import com.proautokimium.api.Infrastructure.utils.LinhaSankhya;
import com.proautokimium.api.Infrastructure.utils.SankhyaRows;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * As seis consultas da newsletter.
 *
 * **Seis, e não uma.** A consulta original é uma só, com oito CTE, e não passa
 * pelo endpoint: `WITH`, `CAST(<agregado> AS DECIMAL(18,2))` e
 * `TRY_CAST(... AS TIME)` derrubam a conexão com o servidor. Separadas, cada
 * uma é testável sozinha, e quando falha o erro diz qual.
 *
 * A junção por `codigo_cliente` acontece em Java, no
 * {@code NewsletterPreviaService}.
 */
@Service
public class NewsletterSankhyaQueryService {

    public static final String CLIENTES = "1_clientes";
    public static final String FATURAMENTO = "2_faturamento";
    public static final String VISITAS = "3_visitas";
    public static final String PECAS = "4_pecas";
    public static final String HORAS = "5_horas";
    public static final String PRODUTO = "6_produto";

    private static final List<String> FILES =
            List.of(CLIENTES, FATURAMENTO, VISITAS, PECAS, HORAS, PRODUTO);

    private final SankhyaQueryService sankhyaQueryService;
    private final ObjectMapper mapper;

    private final Map<String, String> sqls = new HashMap<>();

    public NewsletterSankhyaQueryService(SankhyaQueryService queryService, ObjectMapper mapper) {
        this.sankhyaQueryService = queryService;
        this.mapper = mapper;
    }

    /**
     * Lê os seis arquivos uma vez, na subida.
     *
     * Se um sumir, a aplicação não sobe — e isso é o desejado: o erro aparece no
     * deploy, e não na primeira vez que alguém pedir a prévia do mês.
     *
     * Charset explícito porque sem ele vale o padrão da JVM, que muda entre a
     * máquina de desenvolvimento e o container; o acento dos comentários viraria
     * lixo só em produção.
     */
    @PostConstruct
    void loadSql() {
        for (String file : FILES) {
            var resource = new ClassPathResource("sql/newsletter/" + file + ".sql");

            try (var entry = resource.getInputStream()) {
                sqls.put(file, new String(entry.readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new UncheckedIOException("Não foi possível ler: " + file, e);
            }
        }
    }

    /**
     * Troca o cabeçalho de parâmetros do arquivo pelo do período pedido.
     *
     * **Os arquivos já trazem o próprio `DECLARE`**, com junho fixo — foi assim
     * que eles rodaram na medição. Prefixar outro daria
     * "The variable name '@DATA_INICIO' has already been declared", então a
     * primeira linha sai e a nova entra no lugar.
     *
     * Concatenar aqui é seguro porque `LocalDate` já passou por um tipo: não
     * existe string arbitrária nesta montagem.
     */
    String comParametros(String file, LocalDate de, LocalDate ate) {
        String sql = sqls.get(file);

        if (sql == null) {
            throw new IllegalArgumentException("Consulta desconhecida: " + file);
        }

        int fimDaPrimeiraLinha = sql.indexOf('\n');
        if (fimDaPrimeiraLinha > 0
                && sql.substring(0, fimDaPrimeiraLinha).trim().toUpperCase().startsWith("DECLARE")) {
            sql = sql.substring(fimDaPrimeiraLinha + 1);
        }

        return "DECLARE @DATA_INICIO DATE='" + de + "';"
                + " DECLARE @DATA_FIM DATE='" + ate + "';"
                + " DECLARE @CODPARC INT=NULL;\n"
                + sql;
    }

    public List<LinhaSankhya> consultar(String file, LocalDate de, LocalDate ate) {
        String resposta = sankhyaQueryService.query(comParametros(file, de, ate));
        return SankhyaRows.emLinhas(resposta, mapper);
    }

    /**
     * As seis, em sequência.
     *
     * Sequencial de propósito: são menos de quatro segundos somados, e o login
     * do Sankhya é uma sessão só — disparar em paralelo abriria a porta para
     * seis autenticações concorrentes num serviço que não pediu isso.
     */
    public Map<String, List<LinhaSankhya>> buscarTudo(LocalDate de, LocalDate ate) {
        Map<String, List<LinhaSankhya>> resultado = new HashMap<>();

        for (String file : FILES) {
            resultado.put(file, consultar(file, de, ate));
        }

        return resultado;
    }
}
