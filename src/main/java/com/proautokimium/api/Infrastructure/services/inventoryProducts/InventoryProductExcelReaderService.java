package com.proautokimium.api.Infrastructure.services.inventoryProducts;

import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Service;

import com.proautokimium.api.Infrastructure.abstractions.excel.ExcelReader;
import com.proautokimium.api.domain.entities.prostock.ProductInventory;

@Service
public class InventoryProductExcelReaderService extends ExcelReader<ProductInventory>{

	@Override
	protected int getFirstDataRow(){
		return 1;
	}

	@Override
	protected ProductInventory mapRow(Row row){
		ProductInventory product = new ProductInventory();

		product.setSystemCode(getString(row, 0));
		product.setName(getString(row, 1));
		product.setActive(true);
		product.setMinimumStock(0);

		return product;
	}

}
