package com.proautokimium.api.Infrastructure.abstractions.excel;

import java.io.InputStream;
import java.util.List;

public interface SheetSource {
    boolean supports(String filename);
    List<SheetRow> read(InputStream inputStream) throws Exception;
}
