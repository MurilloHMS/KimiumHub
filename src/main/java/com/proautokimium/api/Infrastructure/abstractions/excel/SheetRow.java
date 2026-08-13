package com.proautokimium.api.Infrastructure.abstractions.excel;

import java.time.LocalDate;

public interface SheetRow {
    String string(int index);
    double number(int index);
    int integer(int index);
    LocalDate date(int index);
}
