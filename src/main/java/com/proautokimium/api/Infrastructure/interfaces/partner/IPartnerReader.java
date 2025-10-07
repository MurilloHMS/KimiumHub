package com.proautokimium.api.Infrastructure.interfaces.partner;

import java.io.InputStream;
import java.util.List;

import com.proautokimium.api.domain.entities.Customer;

public interface IPartnerReader {
	List<Customer> getCustomersByExcel(InputStream stream) throws Exception;
}
