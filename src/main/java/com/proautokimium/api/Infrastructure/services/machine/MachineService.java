package com.proautokimium.api.Infrastructure.services.machine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.machine.MachineDTO;
import com.proautokimium.api.Infrastructure.repositories.MachineRepository;
import com.proautokimium.api.domain.entities.ProductMachine;
import com.proautokimium.api.domain.exceptions.machine.MachineAlreadyExistsException;
import com.proautokimium.api.domain.exceptions.machine.MachineNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MachineService {

    @Autowired
    MachineRepository machineRepository;

    @Autowired
    ObjectMapper mapper;

    @Transactional
    public void save(MachineDTO dto) {

        if (dto.id() != null && machineRepository.existsById(dto.id()))
            throw new MachineAlreadyExistsException();

        ProductMachine machine = mapper.convertValue(dto, ProductMachine.class);
        machineRepository.save(machine);
    }


    @Transactional
    public void update(MachineDTO dto) {

        ProductMachine machine = machineRepository.findById(dto.id())
                .orElseThrow(MachineNotFoundException::new);

        machine.setName(dto.name());
        machine.setSystemCode(dto.systemCode());
        machine.setMachineStatus(dto.machineStatus());
        machine.setBrand(dto.brand());
        machine.setActive(dto.active());
        machine.setMinimum_Stock(dto.minimum_stock());
        machine.setMachineType(dto.machineType());

        machineRepository.save(machine);
    }

    public List<MachineDTO> getAllMachines(){
        return machineRepository.findAll().stream().map(machine -> mapper.convertValue(machine, MachineDTO.class))
        .toList();
    }


}
