package com.proautokimium.api.Infrastructure.services.sales;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Infrastructure.abstractions.sankhya.SankhyaSqlReader;
import com.proautokimium.api.Infrastructure.services.sankhya.SankhyaQueryService;
import com.proautokimium.api.Infrastructure.utils.LinhaSankhya;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * As consultas do checklist no Sankhya, todas paginadas.
 *
 * <p><b>O DbExplorer corta em 5.000 linhas</b> e só avisa no {@code burstLimit}
 * (medido em 2026-09-29: 6.853 clientes voltavam como 5.000, sem erro). Cada
 * arquivo pede {@value #PAGE} linhas depois da última chave, e esta classe
 * repete até vir uma página incompleta.
 *
 * <p>Os parâmetros entram no {@code DECLARE} já como {@code int}: nada que vem
 * de fora é concatenado no SQL.
 */
@Service
public class ChecklistSankhyaQueryService extends SankhyaSqlReader {

    /** O {@code TOP} dos arquivos. Mudou lá, muda aqui. */
    static final int PAGE = 4000;

    /** Trava de segurança: 30 páginas = 120 mil linhas, muito acima do real. */
    private static final int MAX_PAGES = 30;

    public ChecklistSankhyaQueryService(SankhyaQueryService sankhyaQueryService, ObjectMapper mapper) {
        super(sankhyaQueryService, mapper);
    }

    @Override
    protected String directory() {
        return "sql/checklist/";
    }

    @Override
    protected List<String> files() {
        return List.of("customers", "products", "prices");
    }

    public List<LinhaSankhya> customers() {
        List<LinhaSankhya> all = new ArrayList<>();
        int last = 0;
        for (int page = 0; page < MAX_PAGES; page++) {
            List<LinhaSankhya> rows = execute("customers", "DECLARE @ULTIMO_CLIENTE INT = " + last + ";");
            all.addAll(rows);
            if (rows.size() < PAGE) {
                return all;
            }
            last = rows.get(rows.size() - 1).inteiro("CODPARC");
        }
        return all;
    }

    public List<LinhaSankhya> products() {
        List<LinhaSankhya> all = new ArrayList<>();
        int last = 0;
        for (int page = 0; page < MAX_PAGES; page++) {
            List<LinhaSankhya> rows = execute("products", "DECLARE @ULTIMO_PRODUTO INT = " + last + ";");
            all.addAll(rows);
            if (rows.size() < PAGE) {
                return all;
            }
            last = rows.get(rows.size() - 1).inteiro("CODPROD");
        }
        return all;
    }

    public List<LinhaSankhya> prices() {
        List<LinhaSankhya> all = new ArrayList<>();
        int lastTable = 0;
        int lastProduct = 0;
        for (int page = 0; page < MAX_PAGES; page++) {
            List<LinhaSankhya> rows = execute("prices", "DECLARE @ULTIMA_TABELA INT = " + lastTable
                    + ", @ULTIMO_PRODUTO INT = " + lastProduct + ";");
            all.addAll(rows);
            if (rows.size() < PAGE) {
                return all;
            }
            LinhaSankhya last = rows.get(rows.size() - 1);
            lastTable = last.inteiro("NUTAB");
            lastProduct = last.inteiro("CODPROD");
        }
        return all;
    }
}
