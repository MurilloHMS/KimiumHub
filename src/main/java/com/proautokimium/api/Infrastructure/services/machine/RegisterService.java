package com.proautokimium.api.Infrastructure.services.machine;

import com.proautokimium.api.Application.DTOs.prostock.machine.CreateRegisterDTO;
import com.proautokimium.api.Application.DTOs.prostock.machine.ResponseRegisterDTO;
import com.proautokimium.api.Application.DTOs.prostock.machine.UpdateRegisterDTO;
import com.proautokimium.api.Application.DTOs.prostock.machine.ScheduleChangeDTO;
import com.proautokimium.api.Application.DTOs.prostock.machine.ScheduleSlipDTO;
import com.proautokimium.api.Infrastructure.repositories.prostock.MachineScheduleChangeRepository;
import com.proautokimium.api.Infrastructure.repositories.prostock.ProductInventoryRepository;
import com.proautokimium.api.domain.entities.prostock.machine.MachineScheduleChange;
import com.proautokimium.api.domain.enums.MachineStatus;
import com.proautokimium.api.domain.exceptions.machine.MotivoDaAlteracaoObrigatorioException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import com.proautokimium.api.Infrastructure.repositories.prostock.RegisterRepository;
import com.proautokimium.api.domain.entities.prostock.ProductInventory;
import com.proautokimium.api.domain.entities.prostock.machine.MachineRegister;
import com.proautokimium.api.domain.exceptions.machine.MachineNotFoundException;
import com.proautokimium.api.domain.exceptions.machine.MachineRegisterNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RegisterService {
    private final RegisterRepository registerRepository;
    private final ProductInventoryRepository productRepository;

    private final MachineScheduleChangeRepository scheduleChangeRepository;
    private final MachineReconciliationService reconciliationService;
    private final Clock clock;

    public RegisterService(RegisterRepository registerRepository,
                           ProductInventoryRepository productRepository,
                           MachineScheduleChangeRepository scheduleChangeRepository,
                           MachineReconciliationService reconciliationService, Clock clock) {
        this.scheduleChangeRepository = scheduleChangeRepository;
        this.registerRepository = registerRepository;
        this.productRepository = productRepository;
        this.reconciliationService = reconciliationService;
        this.clock = clock;
    }

    /**
     * Só produto marcado como máquina entra numa programação.
     *
     * Antes essa checagem vinha de graça do discriminador — o repositório de
     * máquina só enxergava `type='MACHINE'`. Com a flag, ela precisa ser dita.
     */
    private ProductInventory machineById(UUID id){
        ProductInventory product = productRepository.findById(id)
                .orElseThrow(MachineNotFoundException::new);

        if (!product.isMachine()) throw new MachineNotFoundException();

        return product;
    }

    @Transactional
    public MachineRegister create(CreateRegisterDTO dto){
        ProductInventory machine = machineById(dto.machineId());

        MachineRegister register = new MachineRegister(machine);
        register.fromDto(dto);

        if(dto.adjustStock()){
            int delta = MachineReconciliationService.stockDeltaFor(null, register.getStatus());
            reconciliationService.applyScheduleStockChange(machine, delta, LocalDateTime.now(clock));
        }
        return registerRepository.save(register);
    }

    @Transactional
    public MachineRegister update(UpdateRegisterDTO dto, UUID registerId){
        MachineRegister register = registerRepository.findById(registerId)
                .orElseThrow(MachineRegisterNotFoundException::new);

        // Lido ANTES do fromDto: depois dele a data antiga já se perdeu, e não
        // há como saber de onde a previsão veio.
        LocalDateTime anterior = register.getPrevisaoEntrega();
        // Pelo mesmo motivo, e é o que decide se o estoque anda.
        MachineStatus statusAnterior = register.getStatus();

        register.fromDto(dto);
        registrarAlteracaoDePrevisao(register, anterior, dto);

        if(dto.adjustStock()){
            int delta = MachineReconciliationService.stockDeltaFor(statusAnterior, register.getStatus());
            reconciliationService.applyScheduleStockChange(register.getMachine(), delta, LocalDateTime.now(clock));
        }

        return registerRepository.save(register);
    }

    /**
     * Adiar exige motivo; preencher pela primeira vez, não.
     *
     * A diferença importa: quem completa um cadastro vazio não está
     * justificando nada, e cobrar texto ali só ensina a digitar "ok" para
     * passar da tela. O que a gestão quer saber é por que a data **mudou**.
     *
     * A exceção sai depois do `fromDto`, e isso é seguro: o método é
     * `@Transactional` e `DomainException` é `RuntimeException`, então o
     * rollback desfaz a alteração inteira. Sem isso, a data mudaria e o motivo
     * não seria gravado — exatamente o buraco que esta regra existe para fechar.
     */
    private void registrarAlteracaoDePrevisao(MachineRegister register,
                                              LocalDateTime anterior,
                                              UpdateRegisterDTO dto) {
        if (anterior == null) return;
        if (Objects.equals(anterior, dto.previsaoEntrega())) return;

        String motivo = dto.motivoAlteracaoPrevisao();
        if (motivo == null || motivo.isBlank()) {
            throw new MotivoDaAlteracaoObrigatorioException();
        }

        scheduleChangeRepository.save(new MachineScheduleChange(
                register, anterior, dto.previsaoEntrega(), motivo.trim()));
    }

    /** O histórico de adiamentos de uma programação, mais recente primeiro. */
    /**
     * Todos os adiamentos desde uma data, com de quem são.
     *
     * O Hub agrega isto: quantos no mês, quantas máquinas adiaram mais de uma
     * vez, qual o atraso mediano. A conta fica na tela de propósito — são
     * dezenas de linhas por mês, e cada recorte novo viraria um endpoint novo.
     */
    public List<ScheduleSlipDTO> slipsSince(LocalDateTime from){
        return scheduleChangeRepository.findSince(from)
                .stream()
                .map(c -> new ScheduleSlipDTO(
                        c.getRegister().getId(),
                        c.getRegister().getNomeCliente(),
                        c.getRegister().getMachine().getName(),
                        c.getPrevisaoAnterior(),
                        c.getPrevisaoNova(),
                        c.getMotivo(),
                        c.getChangedAt()
                )).toList();
    }

    public List<ScheduleChangeDTO> listarAlteracoesDePrevisao(UUID registerId){
        return scheduleChangeRepository.findByRegisterIdOrderByChangedAtDesc(registerId)
                .stream()
                .map(c -> new ScheduleChangeDTO(
                        c.getId(),
                        c.getPrevisaoAnterior(),
                        c.getPrevisaoNova(),
                        c.getMotivo(),
                        c.getChangedBy(),
                        c.getChangedAt()))
                .toList();
    }

    @Transactional
    public void delete(UUID id){
        registerRepository.deleteById(id);
    }

    public List<ResponseRegisterDTO> listarRegistrosPorMaquina(UUID maquinaId){
        ProductInventory machine = machineById(maquinaId);

        return registerRepository.findAllByMachine(machine)
                .stream().map(MachineRegister::toDto).toList();
    }

    public List<ResponseRegisterDTO> listarRegistros() {
        return registerRepository.findAll().stream().map(MachineRegister::toDto).toList();
    }
}
