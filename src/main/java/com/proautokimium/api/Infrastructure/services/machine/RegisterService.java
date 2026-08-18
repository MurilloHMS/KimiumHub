package com.proautokimium.api.Infrastructure.services.machine;

import com.proautokimium.api.Application.DTOs.prostock.machine.CreateRegisterDTO;
import com.proautokimium.api.Application.DTOs.prostock.machine.ResponseRegisterDTO;
import com.proautokimium.api.Application.DTOs.prostock.machine.UpdateRegisterDTO;
import com.proautokimium.api.Infrastructure.repositories.prostock.ProductInventoryRepository;
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

    public RegisterService(RegisterRepository registerRepository, ProductInventoryRepository productRepository) {
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

        register.fromDto(dto);
        return registerRepository.save(register);
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
