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
        return List.of("customers");
    }

    public List<LinhaSankhya> clientes(LocalDate desde){
        return execute("customers", "DECLARE @DESDE DATE='" + desde + "';");
    }
}
