package com.proautokimium.api.Infrastructure.services.machine;

import com.proautokimium.api.Application.DTOs.prostock.machine.MachineDTO;
import com.proautokimium.api.Infrastructure.repositories.prostock.ProductInventoryRepository;
import com.proautokimium.api.domain.entities.prostock.ProductInventory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * O que sobrou do módulo de máquina depois que ela virou produto: uma leitura.
 *
 * Cadastrar, alterar e excluir máquina agora é cadastrar produto marcando
 * `isMachine` — não existe mais tabela nem tipo separado. Este serviço é uma
 * projeção sobre `products`, mantida porque a Programação e o Hub já consomem
 * `GET api/machine` e não ganhariam nada em mudar de endereço.
 */
@Service
public class MachineService {

    private final ProductInventoryRepository productInventoryRepository;

    public MachineService(ProductInventoryRepository productInventoryRepository) {
        this.productInventoryRepository = productInventoryRepository;
    }

    public List<MachineDTO> getAllMachines(){
        return productInventoryRepository.findByIsMachineTrue()
                .stream()
                .map(p -> new MachineDTO(
                        p.getId(),
                        p.getSystemCode(),
                        p.getName(),
                        p.getBrand(),
                        p.getMachineType(),
                        p.getMachineStatus(),
                        p.getMinimumStock(),
                        p.isActive()))
                .toList();
    }
}
