package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.client.ClientInviteDTO;
import com.proautokimium.api.Application.DTOs.client.ClientUserDTO;
import com.proautokimium.api.Application.DTOs.partners.CustomerRequestDTO;
import com.proautokimium.api.Application.DTOs.partners.reconciliation.ReconciliationApplyDTO;
import com.proautokimium.api.Application.DTOs.partners.reconciliation.ReconciliationDTO;
import com.proautokimium.api.Application.DTOs.partners.reconciliation.ReconciliationResultDTO;
import com.proautokimium.api.Infrastructure.services.partner.CustomerReconciliationService;
import com.proautokimium.api.Infrastructure.services.partner.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller para gerenciar cadastros dos clientes
 */
@RestController
@RequestMapping("api/customer")
@Tag(name = "Clientes", description = "CRUD Clientes")
public class CustomerController {
    /** O cadastro de clientes e o disparo de e-mails leem os mesmos registros. */
    private static final String LER_CLIENTES =
            "hasAnyAuthority('company/customers:CONSULTAR', "
            + "'communication/email:CONSULTAR', 'communication/newsletter:CONSULTAR')";


    @Autowired
    CustomerService service;

    @Autowired
    CustomerReconciliationService reconciliationService;

    /**
     * Recebe dados e registra cliente
     * @param customer DTO - Dados do cliente
     * @return HttpStatus Created (201)
     */
    @PreAuthorize("hasAuthority('company/customers:INCLUIR')")
    @PostMapping
    @Operation(summary = "Cria cliente", description = "Registra novo cliente")
    public ResponseEntity<String> CreateCustomer(@RequestBody @NotNull @Valid CustomerRequestDTO customer){
        service.createCustomer(customer);
        return ResponseEntity.status(HttpStatus.CREATED).body("Cliente cadastrado com sucesso!");
    }

    /**
     * Recebe planilha e registra clientes
     * @param file Planilha Excel com dados dos clientes
     * @return HttpStatus Created (201)
     */
    @PreAuthorize("hasAuthority('company/customers:INCLUIR')")
    @PostMapping("upload")
    @Operation(summary = "Cria cliente via Excel", description = "Cadastra clientes via planilha")
    public ResponseEntity<String> createCustomersByExcel(@RequestParam MultipartFile file){
        service.includeCustomersByExcel(file);
        return ResponseEntity.status(HttpStatus.CREATED).body("Clientes cadastrado com sucesso via planilha!");
    }

    /**
     * Obtém lista de clientes
     * @return Lista de clientes
     */
    @PreAuthorize(LER_CLIENTES)
    @GetMapping
    @Operation(summary = "Obtém lista de clientes", description = "Retorna lista de cadastro dos clientes")
    public ResponseEntity<Object> GetAllCustomer(){
        return service.getAllCustomers();
    }

    /**
     * Obtém lista de e-mail dos clientes
     * @return Lista de E-mails
     */
    @PreAuthorize(LER_CLIENTES)
    @GetMapping("only-email")
    @Operation(summary = "Obtém lista de emails", description = "Retorna lista de emails dos clientes")
    public ResponseEntity<Object> GetAllCustomerEmail(){
        return service.getAllCustomersEmail();
    }

    /**
     * Recebe dados para atualização do cadastro do cliente
     * @param dto Dados para atualizar cliente
     * @return HttpStatus OK (200)
     */
    @PreAuthorize("hasAuthority('company/customers:ALTERAR')")
    @PutMapping
    @Operation(summary = "Atualiza Cliente", description = "Recebe dados para atualizar cliente")
    public ResponseEntity<String> UpdateCustomer(@RequestBody @NotNull @Valid CustomerRequestDTO dto){
        service.UpdateCustomer(dto);
        return ResponseEntity.status(HttpStatus.OK).body("Cliente atualizado com sucesso!");
    }

    /**
     * Recebe ID do parceiro e excluí cadastro
     * @param codParceiro Código interno Sankhya
     * @return HttpStatus OK (200)
     */
    @PreAuthorize("hasAuthority('company/customers:EXCLUIR')")
    @DeleteMapping
    @Operation(summary = "Deleta cliente", description = "Excluí do sistema o registro do cliente")
    public ResponseEntity<String> DeleteCustomer(@RequestBody @NotNull @Valid String codParceiro){
    	 service.DeleteCustomer(codParceiro);
    	 return ResponseEntity.status(HttpStatus.OK).body("Cliente deletado com sucesso!");
    }

    /** Quem tem acesso ao portal por este cliente. */
    @PreAuthorize("hasAuthority('company/customers:CONSULTAR')")
    @GetMapping("{codParceiro}/users")
    @Operation(summary = "Acessos do cliente")
    public ResponseEntity<List<ClientUserDTO>> users(@PathVariable String codParceiro) {
        return ResponseEntity.ok(service.listAccess(codParceiro));
    }

    /**
     * O que o Sankhya tem e o cadastro daqui não tem, e vice-versa.
     *
     * <p><b>{@code GET} porque não grava nada.</b> A prévia mostra o que
     * aconteceria; quem escreve é o aplicar, e ele exigirá {@code INCLUIR} e
     * {@code ALTERAR}. Por isso aqui basta {@code CONSULTAR}.
     *
     * <p><b>Recebe meses, e não uma data.</b> A tela oferece "últimos 12/24/36
     * meses", e quem deriva a data é a API — do mesmo jeito que a prévia da
     * newsletter recebe mês e ano em vez de intervalo. Com a data vindo pronta,
     * a tela e o servidor podem discordar sobre o que é "12 meses"; assim, não.
     *
     * <p>O mínimo é 1: com zero ou negativo a data cairia no futuro, a consulta
     * voltaria vazia, e a tela diria "nada mudou" quando o errado era o
     * parâmetro. O máximo evita pedir a base inteira sem querer.
     */
    @PreAuthorize("hasAuthority('company/customers:CONSULTAR')")
    @GetMapping("reconciliation")
    @Operation(summary = "Concilia com o Sankhya", description = "Compara o cadastro com o ERP e devolve o que mudou, sem gravar")
    public ResponseEntity<ReconciliationDTO> reconciliation(
            @RequestParam(defaultValue = "12") @Min(1) @Max(120) int months) {
        return ResponseEntity.ok(reconciliationService.preview(LocalDate.now().minusMonths(months)));
    }

    /**
     * Grava as linhas escolhidas na conciliação.
     *
     * <p>Exige as <b>duas</b> permissões de escrita: o mesmo lote pode criar
     * cliente e alterar cliente, e separar em dois endpoints faria a pessoa
     * aplicar duas vezes para uma decisão só.
     *
     * <p>O corpo diz apenas QUEM foi marcado. O servidor reconsulta o ERP e
     * decide o que fazer — se a tela mandasse "criar" e a linha já existisse,
     * obedecer criaria duplicata.
     */
    @PreAuthorize("hasAuthority('company/customers:INCLUIR') and hasAuthority('company/customers:ALTERAR')")
    @PostMapping("reconciliation")
    @Operation(summary = "Aplica a conciliação", description = "Grava as linhas escolhidas, uma transação por linha")
    public ResponseEntity<ReconciliationResultDTO> applyReconciliation(
            @RequestParam(defaultValue = "12") @Min(1) @Max(120) int months,
            @RequestBody @Valid ReconciliationApplyDTO body) {
        return ResponseEntity.ok(
                reconciliationService.apply(LocalDate.now().minusMonths(months), body));
    }

    /** Convida uma pessoa desta empresa para o portal do cliente. */
    @PostMapping("{codParceiro}/users")
    @Operation(summary = "Convida um acesso", description = "Cria o convite de primeiro acesso e envia o link por e-mail")
    @PreAuthorize("hasAuthority('company/customers:CONFIGURAR')")
    public ResponseEntity<String> invite(@PathVariable String codParceiro,
                                         @RequestBody @Valid ClientInviteDTO dto) {
        service.inviteAccess(codParceiro, dto.email());
        return ResponseEntity.ok("Convite enviado para " + dto.email() + ".");
    }
    
}
