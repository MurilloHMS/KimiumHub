package com.proautokimium.api.controllers.prostock;

import com.proautokimium.api.Application.DTOs.machine.MachineAlertConfigDTO;
import com.proautokimium.api.Application.DTOs.prostock.machine.CreateRegisterDTO;
import com.proautokimium.api.Application.DTOs.prostock.machine.ReconcileDTO;
import com.proautokimium.api.Application.DTOs.prostock.machine.UpdateRegisterDTO;
import com.proautokimium.api.Infrastructure.services.machine.MachineAlertService;
import com.proautokimium.api.Infrastructure.services.machine.MachineReconciliationService;
import com.proautokimium.api.Infrastructure.services.machine.MachineService;
import com.proautokimium.api.Infrastructure.services.machine.RegisterService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("api/machine")
public class MachineController {
    /**
     * As leituras que alimentam os stores compartilhados.
     *
     * O Hub, as Movimentações e a Programação leem as mesmas máquinas e os
     * mesmos registros — os stores são um só. Exigir uma tela específica
     * aqui esvaziaria as outras duas sem erro nenhum na tela.
     */
    private static final String LER_ESTOQUE =
            "hasAnyAuthority('stock/hub:CONSULTAR', 'stock/programacao:CONSULTAR', "
            + "'stock/movements:CONSULTAR', 'stock/inventory-hub:CONSULTAR')";


    @Autowired
    private MachineService service;

    @Autowired
    private RegisterService registerService;

    @Autowired
    private MachineAlertService alertService;

    @Autowired
    private MachineReconciliationService reconciliationService;

    @PreAuthorize(LER_ESTOQUE)
    @GetMapping
    public ResponseEntity<Object> getMachines(){
        return ResponseEntity.status(HttpStatus.OK).body(service.getAllMachines());
    }

    /**
     * As duas contagens de cada máquina, para o Hub mostrar onde elas separaram.
     *
     * Devolve todas, não só as divergentes: ver que treze máquinas batem é
     * metade da informação, e sem isso a lista vazia seria indistinguível de
     * uma tela quebrada.
     */
    @PreAuthorize("hasAuthority('stock/hub:CONSULTAR')")
    @GetMapping("/divergences")
    public ResponseEntity<Object> getDivergences(){
        return ResponseEntity.ok(reconciliationService.divergences());
    }

    /*
     * Cadastrar, alterar e excluir máquina agora é cadastrar produto marcando
     * "é máquina", em api/inventory. O GET acima sobrevive como projeção:
     * a Programação e o Hub já o consomem e não ganhariam nada mudando de
     * endereço.
     *
     * A movimentação de máquina também saiu: máquina é produto de estoque, e
     * o estoque dela é lançado pela mesma tela dos demais.
     */

    // Registers

    @PreAuthorize("hasAuthority('stock/programacao:INCLUIR')")
    @PostMapping("/register")
    public ResponseEntity<?> createRegister(@RequestBody CreateRegisterDTO dto){
        registerService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body("Registro da máquina incluído com sucesso");
    }

    @PreAuthorize("hasAuthority('stock/programacao:ALTERAR')")
    @PutMapping("/register/{id}")
    public ResponseEntity<?> updateRegister(@RequestBody UpdateRegisterDTO dto, @PathVariable UUID id){
        registerService.update(dto, id);
        return ResponseEntity.status(HttpStatus.OK).body("Registro da máquina atualizado com sucesso");
    }

    @PreAuthorize("hasAuthority('stock/programacao:EXCLUIR')")
    @DeleteMapping("/register/{id}")
    public ResponseEntity<?> deleteRegister(@PathVariable UUID id){
        registerService.delete(id);
        return ResponseEntity.status(HttpStatus.OK).body("Registro da máquina deletado com sucesso");
    }

    @PreAuthorize(LER_ESTOQUE)
    @GetMapping("/register/{id}")
    public ResponseEntity<?> getRegistersByMachineId(@PathVariable UUID id){
        return ResponseEntity.ok(registerService.listarRegistrosPorMaquina(id));
    }

    /** Histórico de adiamentos de uma programação. */
    @PreAuthorize("hasAuthority('stock/programacao:CONSULTAR')")
    @GetMapping("/register/{id}/schedule-changes")
    public ResponseEntity<?> getScheduleChanges(@PathVariable UUID id){
        return ResponseEntity.ok(registerService.listarAlteracoesDePrevisao(id));
    }

    /**
     * Os adiamentos de um período, para o Hub agregar.
     *
     * `from` é obrigatório: sem recorte, isto cresceria para sempre e um dia
     * traria três anos de histórico numa tela que só quer o mês.
     */
    @PreAuthorize("hasAuthority('stock/hub:CONSULTAR')")
    @GetMapping("/register/schedule-changes")
    public ResponseEntity<?> getScheduleSlips(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from){
        return ResponseEntity.ok(registerService.slipsSince(from.atStartOfDay()));
    }

    @PreAuthorize(LER_ESTOQUE)
    @GetMapping("/register")
    public ResponseEntity<?> getAllRegisters(){
        return ResponseEntity.ok(registerService.listarRegistros());
    }

    @PreAuthorize("hasAuthority('stock/alerts:CONSULTAR')")
    @GetMapping("/alert-config")
    public ResponseEntity<MachineAlertConfigDTO> getAlertConfig(){
        return ResponseEntity.ok(alertService.get());
    }

    @PreAuthorize("hasAuthority('stock/alerts:CONFIGURAR')")
    @PutMapping("/alert-config")
    public ResponseEntity<MachineAlertConfigDTO> saveAlertConfig(@RequestBody @Valid MachineAlertConfigDTO dto){
        return ResponseEntity.ok(alertService.save(dto));
    }

    @PreAuthorize("hasAuthority('stock/alerts:ENVIAR')")
    @PostMapping("/alert-config/test")
    public ResponseEntity<String> testAlerts(){
        int sent = alertService.sendSampleAlert();
        return sent > 0
                ? ResponseEntity.ok(sent + " e-mail(s) de teste enfileirado(s).")
                : ResponseEntity.badRequest().body("Selecione ao menos um destinatario e salve antes de testar.");
    }

    /**
     * Lança o estoque da máquina e ajusta a programação, numa transação só.
     *
     * Não existe endpoint para "só criar programação" ou "só lançar movimento"
     * de máquina — para isso continuam as telas de sempre. Este existe para os
     * dois acontecerem juntos ou nenhum acontecer.
     */
    /**
     * Acerta os dois números de uma máquina que já estava divergente.
     *
     * `POST` e não `GET` porque escreve — cria programação ou lança movimento.
     * A tela mostra o que vai acontecer antes de chamar; aqui não há escolha a
     * fazer, só a conta.
     */
    @PreAuthorize("hasAuthority('stock/hub:ALTERAR')")
    @PostMapping("/{systemCode}/align")
    public ResponseEntity<?> align(@PathVariable String systemCode){
        return ResponseEntity.ok(reconciliationService.align(systemCode));
    }

    @PreAuthorize("hasAnyAuthority('stock/hub:ALTERAR', 'stock/movements:ALTERAR')")
    @PostMapping("/reconcile")
    public ResponseEntity<?> reconcile(@RequestBody @Valid ReconcileDTO dto){
        reconciliationService.reconcile(dto);
        return ResponseEntity.ok("Estoque e programação atualizados.");
    }
}
