package com.proautokimium.api.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.proautokimium.api.Infrastructure.interfaces.fuelsupply.IFuelSupplyReader;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.services.fuelsupply.FuelSupplyService;
import com.proautokimium.api.domain.entities.FuelSupply;


@RestController
@RequestMapping("api/fuelsupply")
public class FuelSupplyController {
	
	@Autowired
	IFuelSupplyReader reader;
	
	@Autowired
	FuelSupplyService service;
	
	@Autowired
	EmployeeRepository employeeRepository;
	
	@PostMapping("upload")
	public ResponseEntity<?> importfuel(@RequestParam MultipartFile file){
		
		try {
			List<FuelSupply> fuelSupplies = reader.getFuelSuppliesByExcel(file.getInputStream());
//			List<Employee> employees = employeeRepository.findAll();
			
//			Map<String,	Employee> employeeMap = employees.stream().collect(Collectors.toMap(e -> e.getName().toLowerCase(), e -> e));
//			
//			
//			fuelSupplies.forEach(fs -> {
//				String driverName = fs.getDriverName();
//				
//				if(driverName != null) {
//					Employee emp = employeeMap.get(driverName.toLowerCase());
//					if(emp != null) {
//						fs.setDepartment(emp.getDepartamento());
//					}
//				}
//			});
			
			service.insertByRange(fuelSupplies);
			 return ResponseEntity.ok("Importação concluída com sucesso!");

	    } catch (Exception e) {
	        e.printStackTrace();
	        return ResponseEntity.internalServerError()
	                .body("Erro ao importar arquivo: " + e.getMessage());
	    }
	}
}
