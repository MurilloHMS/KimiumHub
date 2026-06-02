package com.proautokimium.api.Infrastructure.services.partner;

import com.proautokimium.api.Infrastructure.abstractions.excel.ExcelReader;
import org.springframework.stereotype.Service;

import com.proautokimium.api.domain.entities.Customer;
import com.proautokimium.api.domain.valueObjects.Email;

import org.apache.poi.ss.usermodel.Row;

@Service
public class PartnerReaderService extends ExcelReader<Customer> {

	@Override
	protected int getFirstDataRow() {
		return 2;
	}

	@Override
	protected Customer mapRow(Row row) {
		Customer customer = new Customer();

		customer.setCodParceiro(getString(row, 0));
		customer.setCodigoMatriz(getString(row, 1));
		customer.setName(getString(row, 3));
		customer.setEmail(new Email(getString(row, 4)));

		return customer;
	}
}
