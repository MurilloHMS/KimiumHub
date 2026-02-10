package com.proautokimium.api.Infrastructure.services.machine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.machine.MachineDTO;
import com.proautokimium.api.Infrastructure.repositories.MachineRepository;
import com.proautokimium.api.domain.entities.ProductMachine;
import com.proautokimium.api.domain.exceptions.machine.MachineAlreadyExistsException;
import com.proautokimium.api.domain.exceptions.machine.MachineNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

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
        machine.setMachineStatus(dto.status());
        machine.setBrand(dto.brand());
        machine.setActive(dto.active());
        machine.setMinimum_Stock(dto.minimum_stock());
        machine.setMachineType(dto.type());

        machineRepository.save(machine);
    }

    public List<MachineDTO> getAllMachines(){
        return machineRepository.findAll().stream().map(machine -> mapper.convertValue(machine, MachineDTO.class))
        .toList();
    }


}
