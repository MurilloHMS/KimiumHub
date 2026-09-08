package com.proautokimium.api.Infrastructure.services.newsletter.sankhya;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Infrastructure.services.sankhya.SankhyaQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O cabeçalho de parâmetros das seis consultas.
 *
 * **Os arquivos `.sql` já trazem o próprio `DECLARE`**, com junho fixo — foi
 * assim que eles rodaram na medição contra a base. Prefixar outro daria
 * `The variable name '@DATA_INICIO' has already been declared`, e o erro só
 * apareceria contra o ERP de verdade: nenhum teste que não fale SQL Server
 * pega isso.
 */
@ExtendWith(MockitoExtension.class)
class NewsletterSankhyaQueryServiceTest {

    @Mock SankhyaQueryService sankhyaQueryService;

    private NewsletterSankhyaQueryService service;

    @BeforeEach
    void setUp() {
        service = new NewsletterSankhyaQueryService(sankhyaQueryService, new ObjectMapper());
        service.loadSql();
    }

    @Test
    @DisplayName("troca o DECLARE do arquivo em vez de empilhar outro")
    void trocaODeclare() {
        String sql = service.comParametros(NewsletterSankhyaQueryService.CLIENTES,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        assertThat(contar(sql, "@DATA_INICIO DATE"))
                .as("dois DECLARE da mesma variável e o SQL Server recusa a consulta inteira")
                .isEqualTo(1);
        assertThat(sql).contains("DECLARE @DATA_INICIO DATE='2026-08-01'");
        assertThat(sql).contains("DECLARE @DATA_FIM DATE='2026-08-31'");
        assertThat(sql).doesNotContain("2026-06-01");
    }

    /**
     * `@CODPARC` continua declarado, e nulo.
     *
     * As seis consultas o usam em `(@CODPARC IS NULL OR ...)`. Some a
     * declaração e todas as seis param de compilar no servidor — mas só lá.
     */
    @Test
    @DisplayName("mantém o @CODPARC que as consultas usam")
    void mantemOCodparc() {
        String sql = service.comParametros(NewsletterSankhyaQueryService.HORAS,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertThat(sql).contains("DECLARE @CODPARC INT=NULL");
        assertThat(sql).contains("@CODPARC IS NULL");
    }

    @Test
    @DisplayName("o corpo da consulta segue inteiro depois do cabeçalho")
    void mantemOCorpo() {
        String sql = service.comParametros(NewsletterSankhyaQueryService.FATURAMENTO,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertThat(sql).contains("faturamento_total");
        assertThat(sql).contains("GROUP BY CAB.CODPARC");
    }

    /** Os seis arquivos existem no classpath — se um sumir, a subida quebra. */
    @Test
    @DisplayName("carrega as seis consultas")
    void carregaAsSeis() {
        for (String file : new String[]{
                NewsletterSankhyaQueryService.CLIENTES, NewsletterSankhyaQueryService.FATURAMENTO,
                NewsletterSankhyaQueryService.VISITAS, NewsletterSankhyaQueryService.PECAS,
                NewsletterSankhyaQueryService.HORAS, NewsletterSankhyaQueryService.PRODUTO}) {

            assertThat(service.comParametros(file, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)))
                    .as(file)
                    .contains("SELECT");
        }
    }

    private int contar(String texto, String trecho) {
        int total = 0, i = 0;
        while ((i = texto.indexOf(trecho, i)) >= 0) {
            total++;
            i += trecho.length();
        }
        return total;
    }
}
