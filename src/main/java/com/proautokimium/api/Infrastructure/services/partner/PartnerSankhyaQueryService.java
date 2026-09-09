package com.proautokimium.api.Infrastructure.services.partner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Infrastructure.abstractions.sankhya.SankhyaSqlReader;
import com.proautokimium.api.Infrastructure.services.sankhya.SankhyaQueryService;
import com.proautokimium.api.Infrastructure.utils.LinhaSankhya;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;


@Service
public class PartnerSankhyaQueryService extends SankhyaSqlReader {

    protected PartnerSankhyaQueryService(SankhyaQueryService sankhyaQueryService, ObjectMapper mapper) {
        super(sankhyaQueryService, mapper);
    }

    @Override
    protected String directory() {
        return "sql/partners/";
    }

    @Override
    protected List<String> files() {
        return List.of("customers", "partner_by_code");
    }

    public List<LinhaSankhya> clientes(LocalDate desde){
        return execute("customers", "DECLARE @DESDE DATE='" + desde + "';");
    }

    /**
     * Um parceiro pelo código.
     *
     * <p>O parâmetro é {@code int}, e não texto: ele vem da URL, e um
     * {@code @PathVariable} concatenado direto no SQL é injeção. Convertido
     * antes, o que chega aqui já passou por um tipo.
     *
     * <p>Devolve lista vazia quando o código não existe no ERP — quem traduz
     * isso em 404 é quem chamou.
     */
    public List<LinhaSankhya> parceiroPorCodigo(int codParceiro){
        return execute("partner_by_code", "DECLARE @CODPARC INT=" + codParceiro + ";");
    }
}
