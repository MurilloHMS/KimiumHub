package com.proautokimium.api.Infrastructure.abstractions.excel;

import java.io.InputStream;
import java.util.List;

public abstract class ExcelReader<T> {

	protected abstract List<T> getDataByExcel(InputStream stream) throws Exception;
}
