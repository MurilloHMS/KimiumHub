package com.proautokimium.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ContentDisposition;
import org.springframework.web.bind.annotation.RestController;

import com.proautokimium.api.Application.DTOs.partners.CreateEmployeeRequestDTO;
import com.proautokimium.api.Application.DTOs.partners.EmployeeDTO;
import com.proautokimium.api.Application.DTOs.partners.EmployeeResponseDTO;
import com.proautokimium.api.Application.DTOs.partners.PartnerRecipientDTO;
import com.proautokimium.api.Infrastructure.services.partner.EmployeeService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import com.proautokimium.api.Application.DTOs.partners.ErpPartnerDTO;
import com.proautokimium.api.Infrastructure.services.partner.ErpPartnerLookupService;
import com.proautokimium.api.Infrastructure.services.partner.PendingSiteAccessReportService;

/**
 * Responsável pelo cadastro dos funcionários
 */
@RestController
@RequestMapping("api/employee")
@Tag(name = "Funcionários", description = "CRUD Funcionários")
public class EmployeeController {
    /**
     * A lista de funcionários alimenta oito telas.
     *
     * Cadastro, calculadoras, equipamentos, notificações, reembolsos, férias,
     * alertas de estoque e disparo de e-mails leem o mesmo store. Exigir uma
     * tela só deixaria as outras sete com o combo vazio — e combo vazio não
     * parece falta de permissão, parece cadastro faltando.
     *
     * **Esta lista precisa crescer junto com o sistema.** Tela nova que leia o
     * store e não entre aqui abre vazia, sem erro nenhum.
     */
    private static final String LER_FUNCIONARIOS =
            "hasAnyAuthority('rh/employees:CONSULTAR', 'rh/calculators:CONSULTAR', "
            + "'rh/equipment-assignments:CONSULTAR', 'rh/notifications:CONSULTAR', "
            + "'rh/reimbursements:CONSULTAR', 'rh/vacation-requests:CONSULTAR', "
            + "'stock/alerts:CONSULTAR', 'communication/email:CONSULTAR', "
            // A administração escolhe o funcionário ao vincular uma conta.
            + "'settings/admin:CONSULTAR')";


	private final EmployeeService service;
	private final ErpPartnerLookupService erpPartnerLookup;
	private final PendingSiteAccessReportService pendingSiteAccessReport;

	public EmployeeController(EmployeeService service, ErpPartnerLookupService erpPartnerLookup,
							  PendingSiteAccessReportService pendingSiteAccessReport) {
		this.erpPartnerLookup = erpPartnerLookup;
		this.service = service;
		this.pendingSiteAccessReport = pendingSiteAccessReport;
	}

	@PreAuthorize(LER_FUNCIONARIOS)
	@GetMapping
	@Operation(summary = "Obtém funcionários", description = "Retorna lista de funcionários")
	public ResponseEntity<List<EmployeeResponseDTO>> getEmployes(){
		return ResponseEntity.ok(service.getAllEmployes());
	}

	@PreAuthorize(LER_FUNCIONARIOS)
	@GetMapping("only-email")
	@Operation(summary = "Obtém e-mails dos funcionários", description = "Retorna lista dos e-mails dos funcionários")
	public ResponseEntity<List<PartnerRecipientDTO>> getEmployesEmail(){
		return ResponseEntity.ok(service.getAllEmployesEmail());
	}

	/**
	 * Cria cadastro do funcionário
	 * @param dto Dados de cadastro
	 * @return Entidade Funcionário Cadastrado
	 */
	@PreAuthorize("hasAuthority('rh/employees:INCLUIR')")
	@PostMapping
	@Operation(summary = "Cria Funcionário", description = "Registra os dados do funcionário")
	public ResponseEntity<EmployeeResponseDTO> createEmploye(@RequestBody @Valid @NotNull CreateEmployeeRequestDTO dto){
		return ResponseEntity.status(HttpStatus.CREATED).body(service.createEmployee(dto));
	}

	/**
	 * Atualiza o cadastro do funcionário
	 * @param dto Dados de atualização
	 * @return Entidade Funcionário Atualizado
	 */
	@PreAuthorize("hasAuthority('rh/employees:ALTERAR')")
	@PutMapping
	@Operation(summary = "Atualiza Funcionário", description = "Atualiza os dados do funcionário")
	public ResponseEntity<EmployeeResponseDTO> updateEmploye(@RequestBody @Valid @NotNull EmployeeDTO dto){
		return ResponseEntity.status(HttpStatus.OK).body(service.updateEmployee(dto));
	}

    /**
     * Um parceiro do Sankhya, para preencher o cadastro.
     *
     * <p><b>Exige INCLUIR, e não CONSULTAR.</b> Com CONSULTAR, qualquer um que
     * vê a lista de funcionários poderia varrer o ERP um código por vez colhendo
     * nome, CPF e e-mail. A rota existe dentro do formulário de cadastro, e a
     * permissão acompanha o uso.
     *
     * <p>O código é {@code int} na assinatura: ele vem da URL, e um
     * {@code @PathVariable} concatenado direto no SQL é injeção. Não numérico o
     * Spring recusa com 400 antes de o método rodar — e antes de qualquer coisa
     * chegar ao Sankhya.
     */
    @PreAuthorize("hasAuthority('rh/employees:INCLUIR')")
    @GetMapping("erp/{codParceiro}")
    @Operation(summary = "Busca um parceiro no Sankhya", description = "Preenche o cadastro de funcionário a partir do CODPARC")
    public ResponseEntity<ErpPartnerDTO> fromErp(@PathVariable int codParceiro) {
        return ResponseEntity.ok(erpPartnerLookup.byCode(codParceiro));
    }

    /**
     * O relatório dos funcionários ativos que ainda não entraram no site.
     *
     * BAIXAR, e não CONSULTAR: é obter um arquivo do que já existe no sistema.
     * A V125 liberou essa ação para quem já consultava funcionários — sem ela,
     * a ação nova nasceria fechada para o RH inteiro.
     */
    @PreAuthorize("hasAuthority('rh/employees:BAIXAR')")
    @GetMapping("site-access/pending/report")
    @Operation(summary = "Relatório dos sem acesso ao site", description = "Funcionários ativos que ainda não fizeram o primeiro acesso, em xlsx ou pdf")
    public ResponseEntity<byte[]> pendingSiteAccessReport(@RequestParam(defaultValue = "xlsx") String format) {
        boolean pdf = "pdf".equalsIgnoreCase(format);
        if (!pdf && !"xlsx".equalsIgnoreCase(format)) {
            return ResponseEntity.badRequest().build();
        }
        byte[] file = pdf ? pendingSiteAccessReport.pdf() : pendingSiteAccessReport.excel();
        String fileName = pendingSiteAccessReport.fileName(pdf ? "pdf" : "xlsx");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString())
                .contentType(pdf ? MediaType.APPLICATION_PDF
                        : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }
}
