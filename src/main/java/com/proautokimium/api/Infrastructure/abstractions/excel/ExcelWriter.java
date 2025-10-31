package com.proautokimium.api.Infrastructure.abstractions.excel;

import java.util.List;

public abstract class ExcelWriter<T> {
	protected abstract byte[] save(List<T> list) throws Exception;
}
