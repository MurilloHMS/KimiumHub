package com.proautokimium.api.Infrastructure.interfaces.fuelsupply;

import java.io.InputStream;
import java.util.List;

import com.proautokimium.api.domain.entities.FuelSupply;

public interface IFuelSupplyReader {
	List<FuelSupply> getFuelSuppliesByExcel(InputStream stream) throws Exception;
}
