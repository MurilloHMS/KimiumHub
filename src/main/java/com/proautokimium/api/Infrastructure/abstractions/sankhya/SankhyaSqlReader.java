package com.proautokimium.api.Infrastructure.abstractions.sankhya;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Infrastructure.services.sankhya.SankhyaQueryService;
import com.proautokimium.api.Infrastructure.utils.LinhaSankhya;
import com.proautokimium.api.Infrastructure.utils.SankhyaRows;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class SankhyaSqlReader {

    private final SankhyaQueryService sankhyaQueryService;
    private final ObjectMapper mapper;

    public SankhyaSqlReader(SankhyaQueryService sankhyaQueryService, ObjectMapper mapper) {
        this.sankhyaQueryService = sankhyaQueryService;
        this.mapper = mapper;
    }

    protected abstract String directory();
    protected abstract List<String> files();

    private final Map<String, String> sqls = new HashMap<>();

    @PostConstruct
    void loadSql(){
        for(String file : files()){
            var resource = new ClassPathResource(directory() + file + ".sql");

            try(var entry = resource.getInputStream()){
                sqls.put(file, new String(entry.readAllBytes(), StandardCharsets.UTF_8));
            }catch (IOException e){
                throw new UncheckedIOException("Não foi possível ler: " + file, e);
            }
        }
    }

    /**
     * Ao passar o Declare, inclua o ';' ao final da linha para não ocorrer erro
     * @param file arquivo Sql com o script
     * @param declare parametro Sql para incluir no arquivo
     * @return {@link LinhaSankhya}
     */
    protected final List<LinhaSankhya> execute(String file, String declare){
        String response = sankhyaQueryService.query(withParameters(file, declare));
        return SankhyaRows.emLinhas(response, mapper);
    }

    protected final String withParameters(String file, String declare){
        String sql = sqls.get(file);

        if (sql == null) {
            throw new IllegalArgumentException("Consulta desconhecida: " + file);
        }

        int endOfFirstRow = sql.indexOf('\n');
        if(endOfFirstRow > 0 && sql.substring(0, endOfFirstRow).trim().toUpperCase().startsWith("DECLARE")){
            sql = sql.substring(endOfFirstRow + 1);
        }

        return declare + sql;
    }

    protected final Map<String, List<LinhaSankhya>> findAll(String declare){
        Map<String, List<LinhaSankhya>> result = new HashMap<>();

        for(String file : files()){
            result.put(file, execute(file, declare));
        }
        return result;
    }
}
