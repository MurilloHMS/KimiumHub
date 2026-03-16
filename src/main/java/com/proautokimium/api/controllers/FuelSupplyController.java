package com.proautokimium.api.controllers;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.proautokimium.api.Application.DTOs.fuelsupply.FuelSupplyReportFilterDTO;
import com.proautokimium.api.Infrastructure.services.fuelsupply.FuelSupplyReportService;
import com.proautokimium.api.domain.enums.Department;
import com.proautokimium.api.domain.enums.ReportFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.proautokimium.api.Infrastructure.interfaces.fuelsupply.IFuelSupplyReader;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.services.fuelsupply.FuelSupplyService;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.FuelSupply;


@RestController
@RequestMapping("api/fuelsupply")
public class FuelSupplyController {
	
	@Autowired
	IFuelSupplyReader reader;
	
	@Autowired
	FuelSupplyService service;

	@Autowired
	FuelSupplyReportService reportService;
	
	@Autowired
	EmployeeRepository employeeRepository;

	@PostMapping("/upload")
	public ResponseEntity<?> importFuel(@RequestParam MultipartFile file) {

		try (InputStream is = file.getInputStream()) {

			List<FuelSupply> fuelSupplies = reader.getFuelSuppliesByExcel(is);
			List<Employee> employees = employeeRepository.findAll();

			Map<String, Employee> employeeMap = employees.stream()
					.collect(Collectors.toMap(
							e -> e.getName().toLowerCase().trim(),
							e -> e,
							(a, b) -> a
					));

			fuelSupplies.forEach(fs -> {

				String driverName = fs.getDriverName();

				Employee emp = driverName == null
						? null
						: employeeMap.get(driverName.toLowerCase().trim());

				Department department =
						(emp != null && emp.getDepartment() != null)
								? emp.getDepartment()
								: Department.SEM_DEPARTAMENTO;

				fs.setDepartment(department);
			});

			service.insertByRange(fuelSupplies);

			return ResponseEntity.ok("Importação concluída com sucesso!");

		} catch (Exception e) {
			e.printStackTrace();

			return ResponseEntity.internalServerError()
					.body("Erro ao importar arquivo: " + e.getMessage());
		}
	}

	@PostMapping
	public ResponseEntity<byte[]> generateReport(@RequestBody FuelSupplyReportFilterDTO dto) {
		return reportService.generateReport(dto);
	}

}
