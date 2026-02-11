package com.proautokimium.api.Infrastructure.services.machine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.machine.MachineDTO;
import com.proautokimium.api.Application.DTOs.machine.MachineMovementDTO;
import com.proautokimium.api.Infrastructure.repositories.MachineMovementRepository;
import com.proautokimium.api.Infrastructure.repositories.MachineRepository;
import com.proautokimium.api.domain.entities.MovementMachine;
import com.proautokimium.api.domain.entities.ProductMachine;
import com.proautokimium.api.domain.exceptions.machine.MachineAlreadyExistsException;
import com.proautokimium.api.domain.exceptions.machine.MachineMovementNotFoundException;
import com.proautokimium.api.domain.exceptions.machine.MachineNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MachineService {

    @Autowired
    MachineRepository machineRepository;

    @Autowired
    MachineMovementRepository machineMovementRepository;

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

    @Transactional
    public void delete(UUID id){
        if(id == null || machineRepository.existsById(id))
            throw new MachineNotFoundException();

        machineRepository.deleteById(id);
    }

    public List<MachineMovementDTO> getMovementsByMachineId(UUID id){
        return machineMovementRepository.findMovementsByMachineId(id)
                .stream().map(mov -> mapper.convertValue(mov, MachineMovementDTO.class)).toList();
    }

    @Transactional
    public void createMovement(MachineMovementDTO dto, UUID machine_id){
        ProductMachine machine = machineRepository.findById(machine_id)
                .orElseThrow(MachineNotFoundException::new);

        MovementMachine mov = new MovementMachine();
        mov.setMovementDate(dto.movementDate());
        mov.setQuantity(dto.quantity());
        mov.setMachine(machine);
        machineMovementRepository.save(mov);
    }

    @Transactional
    public void updateMovement(MachineMovementDTO dto, UUID machine_id){
        MovementMachine mov = machineMovementRepository.findById(dto.id())
                .orElseThrow(MachineMovementNotFoundException::new);

        mov.setMovementDate(dto.movementDate());
        mov.setQuantity(dto.quantity());

        machineMovementRepository.save(mov);
    }

    @Transactional
    public void deleteMovement(UUID id){
        if(id == null || !machineMovementRepository.existsById(id))
            throw new MachineMovementNotFoundException();

        machineMovementRepository.deleteById(id);
    }

}
