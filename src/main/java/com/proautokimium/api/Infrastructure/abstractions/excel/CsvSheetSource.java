package com.proautokimium.api.Infrastructure.abstractions.excel;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class CsvSheetSource implements SheetSource{

    private static final String DELIMITER = ";";
    @Override
    public boolean supports(String filename) {
        return filename != null && filename.toLowerCase().endsWith(".csv");
    }

    @Override
    public List<SheetRow> read(InputStream stream) throws Exception {
        List<SheetRow> rows = new ArrayList<>();
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;

            while((line = reader.readLine()) != null) {
                if(firstLine) {
                    line = line.replace("\uFEFF", "");
                    firstLine = false;
                }

                if(line.isBlank()) continue;

                rows.add(new CsvSheetRow(line.split(DELIMITER, -1)));
            }
        }
        return rows;
    }
}
