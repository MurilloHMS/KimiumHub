package com.proautokimium.api.controllers;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import com.proautokimium.api.Application.DTOs.fuelsupply.DepartmentOptionDTO;
import com.proautokimium.api.Application.DTOs.fuelsupply.FuelSupplyDTO;
import com.proautokimium.api.Application.DTOs.fuelsupply.FuelSupplyImportResultDTO;
import com.proautokimium.api.Application.DTOs.fuelsupply.FuelSupplyImportRowDTO;
import com.proautokimium.api.Application.DTOs.fuelsupply.FuelSupplyPreviewRowDTO;
import com.proautokimium.api.Application.DTOs.fuelsupply.FuelSupplyReportFilterDTO;
import com.proautokimium.api.Infrastructure.repositories.humanResources.DepartmentRepository;
import com.proautokimium.api.Infrastructure.services.fuelsupply.FuelSupplyAuditService;
import com.proautokimium.api.Infrastructure.services.fuelsupply.FuelSupplyReportService;
import com.proautokimium.api.Infrastructure.services.fuelsupply.FuelSupplyService;
import com.proautokimium.api.Infrastructure.services.fuelsupply.FuelSupplyWriterService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Abastecimentos: conferência da planilha, gravação e relatório.
 *
 * <p><b>São três passos, e eles são separados de propósito.</b> Até 2026-09-24
 * o {@code /upload} lia o arquivo e gravava tudo na mesma requisição,
 * respondendo "Importação concluída com sucesso!" — inclusive quando a
 * gravação falhava, porque o retorno do serviço era descartado, e inclusive
 * quando o motorista não casava com nenhum funcionário, caso em que a linha
 * caía calada no departamento {@code SEM_DEPARTAMENTO} e desandava o relatório.
 *
 * <p>Agora: {@code /preview} lê e diagnostica sem gravar nada, {@code /import}
 * grava o que a conferência aprovou, e o relatório continua onde estava.
 */
@Slf4j
@RestController
@RequestMapping("api/fuelsupply")
@Tag(name = "Abastecimento", description = "Controle dos abastecimentos")
public class FuelSupplyController {

	private static final String PLANILHA =
			"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	@Autowired
	FuelSupplyService service;

	@Autowired
	FuelSupplyAuditService auditService;

	@Autowired
	FuelSupplyReportService reportService;

	@Autowired
	DepartmentRepository departmentRepository;

	@Autowired
	FuelSupplyWriterService writerService;

	/**
	 * Passo 1: lê a planilha e devolve o diagnóstico. <b>Não grava nada.</b>
	 *
	 * @param file arquivo xlsx no formato do modelo
	 * @return uma linha por linha preenchida da planilha, com motorista casado,
	 *         departamento sugerido e marcação de duplicata
	 */
	@PreAuthorize("hasAuthority('company/fuel-supply:INCLUIR')")
	@PostMapping("/preview")
	@Operation(summary = "Conferir planilha",
			description = "Lê a planilha e devolve o diagnóstico de cada linha, sem gravar")
	public ResponseEntity<?> preview(@RequestParam MultipartFile file) {

		try (InputStream is = file.getInputStream()) {

			List<FuelSupplyPreviewRowDTO> linhas = auditService.preview(is);

			return ResponseEntity.ok(linhas);

		} catch (Exception e) {
			log.error("Ocorreu um erro ao ler a planilha de abastecimentos: {}", e.getMessage());

			return ResponseEntity.badRequest()
					.body("Não foi possível ler a planilha: " + e.getMessage());
		}
	}

	/**
	 * Passo 2: grava as linhas que a conferência aprovou.
	 *
	 * <p>Responde <b>422</b> quando alguma linha é recusada — e aí nenhuma é
	 * gravada. O corpo traz os motivos, um por linha, com o número da linha.
	 */
	@PreAuthorize("hasAuthority('company/fuel-supply:INCLUIR')")
	@PostMapping("/import")
	@Operation(summary = "Gravar abastecimentos",
			description = "Grava as linhas conferidas; uma linha recusada cancela a remessa")
	public ResponseEntity<FuelSupplyImportResultDTO> importar(
			@RequestBody List<FuelSupplyImportRowDTO> linhas) {

		FuelSupplyImportResultDTO resultado = auditService.importar(linhas);

		if (resultado.recusadas() > 0) {
			return ResponseEntity.unprocessableEntity().body(resultado);
		}

		return ResponseEntity.ok(resultado);
	}

	/**
	 * Os departamentos para o combo da conferência.
	 *
	 * <p>Vive aqui, e não no RH, porque o {@code GET /api/hr/departments} exige
	 * autoridade de RH: quem cuida de abastecimento abriria a tela com a lista
	 * vazia.
	 */
	@PreAuthorize("hasAnyAuthority('company/fuel-supply:INCLUIR', 'company/fuel-supply:CONSULTAR')")
	@GetMapping("/departments")
	@Operation(summary = "Departamentos", description = "Opções de departamento para a conferência")
	public ResponseEntity<List<DepartmentOptionDTO>> departments() {

		List<DepartmentOptionDTO> opcoes = departmentRepository.findAll().stream()
				.map(d -> new DepartmentOptionDTO(d.getId(), d.getName()))
				.sorted(Comparator.comparing(DepartmentOptionDTO::name,
						String.CASE_INSENSITIVE_ORDER))
				.toList();

		return ResponseEntity.ok(opcoes);
	}

	/**
	 * Passo 3: gera o relatório de combustíveis.
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

	/** O modelo em branco, para quem vai preencher a planilha. */
	@PreAuthorize("hasAnyAuthority('company/fuel-supply:BAIXAR', 'company/fuel-hub:BAIXAR')")
	@GetMapping("/model")
	@Operation(summary = "Baixar modelo", description = "Planilha em branco com os cabeçalhos")
	public ResponseEntity<byte[]> model() throws Exception {
		return planilha(writerService.writeTemplate(), "modelo-abastecimentos.xlsx");
	}

	/**
	 * Os dados do período em Excel, no mesmo formato do modelo — então o
	 * arquivo exportado pode ser corrigido e reenviado.
	 */
	@PreAuthorize("hasAnyAuthority('company/fuel-supply:BAIXAR', 'company/fuel-hub:BAIXAR')")
	@GetMapping("/export")
	@Operation(summary = "Exportar dados", description = "Abastecimentos de um período em xlsx")
	public ResponseEntity<byte[]> export(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) throws Exception {

		return planilha(service.exportByPeriod(start, end),
				"abastecimentos-" + start + "-a-" + end + ".xlsx");
	}

	private ResponseEntity<byte[]> planilha(byte[] conteudo, String nome) {
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nome + "\"")
				.contentType(MediaType.parseMediaType(PLANILHA))
				.body(conteudo);
	}
}
