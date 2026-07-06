package com.proautokimium.api.controllers;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.proautokimium.api.Application.DTOs.fuelsupply.FuelSupplyReportFilterDTO;
import com.proautokimium.api.Infrastructure.services.fuelsupply.FuelSupplyReaderService;
import com.proautokimium.api.Infrastructure.services.fuelsupply.FuelSupplyReportService;
import com.proautokimium.api.domain.enums.Department;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.services.fuelsupply.FuelSupplyService;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.FuelSupply;

/**
 * Responsável por coletar dados e gerar relatório
 */
@Slf4j
@RestController
@RequestMapping("api/fuelsupply")
@Tag(name = "Abastecimento", description = "Controle dos abastecimentos")
public class FuelSupplyController {

	@Autowired
	FuelSupplyReaderService reader;
	
	@Autowired
	FuelSupplyService service;

	@Autowired
	FuelSupplyReportService reportService;
	
	@Autowired
	EmployeeRepository employeeRepository;

	/**
	 * Coleta dados via planilha
	 * @param file Arquivo xlsx com dados dos combustíveis
	 * @return HttpStatus OK (200)
	 */
	@PostMapping("/upload")
	@Operation(summary = "Upload Dados", description = "Upload dos dados de abastecimento")
	public ResponseEntity<?> importFuel(@RequestParam MultipartFile file) {

		try (InputStream is = file.getInputStream()) {

			List<FuelSupply> fuelSupplies = reader.getDataByExcel(is);
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
			log.error("Ocorreu um erro ao importar o combustível: {}", e.getMessage());

			return ResponseEntity.internalServerError()
					.body("Erro ao importar arquivo: " + e.getMessage());
		}
	}

	/**
	 * Gera relatório de combustíveis
	 * @param dto Dados do filtro
	 * @return PDF com relatório
	 */
	@PostMapping
	@Operation(summary = "Gerar relatório", description = "Gera o relatório de combustíveis")
	public ResponseEntity<byte[]> generateReport(@RequestBody FuelSupplyReportFilterDTO dto) {
		return reportService.generateReport(dto);
	}

}
