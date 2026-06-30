package com.proautokimium.api.Infrastructure.services.fuelsupply;

import com.proautokimium.api.Infrastructure.abstractions.excel.ExcelReader;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Service;

import com.proautokimium.api.domain.entities.FuelSupply;

@Service
public class FuelSupplyReaderService extends ExcelReader<FuelSupply> {

	@Override
	protected int getFirstDataRow(){
		return 1;
	}

	@Override
	protected FuelSupply mapRow(Row row) {
		FuelSupply fuel = new FuelSupply();

		fuel.setDriverName(getString(row, 0));
		fuel.setFuelSupplyDate(getDate(row, 1));
		fuel.setUf(getString(row,3));
		fuel.setPlate(getString(row, 4));
		fuel.setActualHodometer(getInteger(row, 5));
		fuel.setFuelType(getString(row, 6));
		fuel.setLiters(getDouble(row, 7));
		fuel.setTotalValue(getDouble(row, 8));
		fuel.setPrice(getDouble(row, 9));
		fuel.setDiferenceHodometer(getDouble(row, 11));
		fuel.setAverageKm(getDouble(row, 12));

		return fuel;
	}
}
