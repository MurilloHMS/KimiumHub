package com.proautokimium.api.controllers;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.proautokimium.api.Application.DTOs.fuelsupply.FuelSupplyDTO;
import com.proautokimium.api.Application.DTOs.fuelsupply.FuelSupplyReportFilterDTO;
import com.proautokimium.api.Infrastructure.services.fuelsupply.FuelSupplyReaderService;
import com.proautokimium.api.Infrastructure.services.fuelsupply.FuelSupplyReportService;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.DepartmentNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.humanResources.DepartmentRepository;
import com.proautokimium.api.domain.entities.humanResources.Department;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

	@Autowired
	DepartmentRepository departmentRepository;

	/**
	 * Coleta dados via planilha
	 * @param file Arquivo xlsx com dados dos combustíveis
	 * @return HttpStatus OK (200)
	 */
	@PreAuthorize("hasAuthority('company/fuel-supply:INCLUIR')")
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

				fs.setDepartment(departamentoDoFuncionario(emp));
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
	@PreAuthorize("hasAnyAuthority('company/fuel-supply:BAIXAR', 'company/fuel-hub:BAIXAR')")
	@PostMapping
	@Operation(summary = "Gerar relatório", description = "Gera o relatório de combustíveis")
	public ResponseEntity<byte[]> generateReport(@RequestBody FuelSupplyReportFilterDTO dto) {
		return reportService.generateReport(dto);
	}

	@PreAuthorize("hasAnyAuthority('company/fuel-supply:CONSULTAR', 'company/fuel-hub:CONSULTAR')")
	@GetMapping
	@Operation(summary = "Listar abastecimentos", description = "Abastecimentos de um período")
	public ResponseEntity<List<FuelSupplyDTO>> list(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

		return ResponseEntity.ok(service.listByPeriod(start, end));
	}

	/**
	 * O departamento do abastecimento, vindo do SETOR do funcionario.
	 *
	 * O funcionario nao guarda mais departamento proprio — quem decide e o
	 * setor, que pertence a um departamento.
	 *
	 * Motorista sem funcionario casado, funcionario sem setor: a planilha vem
	 * de fora e as tres coisas acontecem. Cai no balde SEM_DEPARTAMENTO, que
	 * existe no cadastro desde a V54, em vez de estourar a importacao inteira
	 * ou gravar nulo numa coluna que nao aceita.
	 */
	private Department departamentoDoFuncionario(Employee emp) {
		if (emp != null && emp.getTeam() != null && emp.getTeam().getDepartment() != null) {
			return emp.getTeam().getDepartment();
		}

		return departmentRepository.findByName("SEM_DEPARTAMENTO")
				.orElseThrow(DepartmentNotFoundException::new);
	}
}
