package com.proautokimium.api.Infrastructure.services.machine;

import com.proautokimium.api.Application.DTOs.prostock.machine.CreateRegisterDTO;
import com.proautokimium.api.Application.DTOs.prostock.machine.ResponseRegisterDTO;
import com.proautokimium.api.Application.DTOs.prostock.machine.UpdateRegisterDTO;
import com.proautokimium.api.Application.DTOs.prostock.machine.ScheduleChangeDTO;
import com.proautokimium.api.Infrastructure.repositories.prostock.MachineScheduleChangeRepository;
import com.proautokimium.api.Infrastructure.repositories.prostock.ProductInventoryRepository;
import com.proautokimium.api.domain.entities.prostock.machine.MachineScheduleChange;
import com.proautokimium.api.domain.exceptions.machine.MotivoDaAlteracaoObrigatorioException;

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

    public RegisterService(RegisterRepository registerRepository,
                           ProductInventoryRepository productRepository,
                           MachineScheduleChangeRepository scheduleChangeRepository) {
        this.scheduleChangeRepository = scheduleChangeRepository;
        this.registerRepository = registerRepository;
        this.productRepository = productRepository;
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
        return registerRepository.save(register);
    }

    @Transactional
    public MachineRegister update(UpdateRegisterDTO dto, UUID registerId){
        MachineRegister register = registerRepository.findById(registerId)
                .orElseThrow(MachineRegisterNotFoundException::new);

        // Lido ANTES do fromDto: depois dele a data antiga já se perdeu, e não
        // há como saber de onde a previsão veio.
        LocalDateTime anterior = register.getPrevisaoEntrega();

        register.fromDto(dto);
        registrarAlteracaoDePrevisao(register, anterior, dto);

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
