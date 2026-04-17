package com.proautokimium.api.Infrastructure.services.machine;

import com.proautokimium.api.Application.DTOs.prostock.machine.CreateRegisterDTO;
import com.proautokimium.api.Application.DTOs.prostock.machine.ResponseRegisterDTO;
import com.proautokimium.api.Application.DTOs.prostock.machine.UpdateRegisterDTO;
import com.proautokimium.api.Infrastructure.repositories.prostock.MachineRepository;
import com.proautokimium.api.Infrastructure.repositories.prostock.RegisterRepository;
import com.proautokimium.api.domain.entities.prostock.machine.Machine;
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
    private final MachineRepository machineRepository;

    public RegisterService(RegisterRepository registerRepository, MachineRepository machineRepository) {
        this.registerRepository = registerRepository;
        this.machineRepository = machineRepository;
    }

    @Transactional
    public MachineRegister create(CreateRegisterDTO dto){
        Machine machine = machineRepository.findById(dto.machineId())
                .orElseThrow(MachineNotFoundException::new);

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
        Machine machine = machineRepository.findById(maquinaId)
                .orElseThrow(MachineNotFoundException::new);

        return registerRepository.findAllByMachine(machine)
                .stream().map(MachineRegister::toDto).toList();
    }

    public List<ResponseRegisterDTO> listarRegistros() {
        return registerRepository.findAll().stream().map(MachineRegister::toDto).toList();
    }
}
