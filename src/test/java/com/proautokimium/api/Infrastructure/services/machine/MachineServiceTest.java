package com.proautokimium.api.Infrastructure.services.machine;

import com.proautokimium.api.Application.DTOs.prostock.machine.MachineDTO;
import com.proautokimium.api.Infrastructure.repositories.prostock.ProductInventoryRepository;
import com.proautokimium.api.domain.entities.prostock.ProductInventory;
import com.proautokimium.api.domain.enums.MachineStatus;
import com.proautokimium.api.domain.enums.MachineType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Máquina deixou de ter cadastro próprio: o que sobrou é uma leitura sobre
 * produtos. Os testes de criar, alterar, excluir e movimentar sumiram junto com
 * o código — inclusive os dois que documentavam o `delete()` de condição
 * invertida, que nunca apagou nada.
 */
@ExtendWith(MockitoExtension.class)
class MachineServiceTest {

    @Mock
    ProductInventoryRepository productInventoryRepository;

    @InjectMocks
    MachineService service;

    private ProductInventory machine() {
        ProductInventory product = new ProductInventory();
        product.setSystemCode("SYS001");
        product.setName("CAPO NT 300");
        product.setActive(true);
        product.setMinimumStock(2);
        product.setMachine(true);
        product.setBrand("Kimium");
        product.setMachineType(MachineType.CAPO);
        product.setMachineStatus(MachineStatus.DISPONIVEL);
        return product;
    }

    @Test
    @DisplayName("Deve listar apenas os produtos marcados como máquina")
    void deveListarApenasProdutosMarcadosComoMaquina() {
        when(productInventoryRepository.findByIsMachineTrue()).thenReturn(List.of(machine()));

        List<MachineDTO> machines = service.getAllMachines();

        // O filtro é da consulta, não do serviço: é o que substitui o
        // `WHERE type='MACHINE'` que o discriminador dava de graça.
        verify(productInventoryRepository).findByIsMachineTrue();
        assertThat(machines).hasSize(1);
    }

    @Test
    @DisplayName("Deve levar os atributos do modelo para o DTO")
    void deveLevarAtributosDoModeloParaODto() {
        when(productInventoryRepository.findByIsMachineTrue()).thenReturn(List.of(machine()));

        MachineDTO dto = service.getAllMachines().getFirst();

        assertThat(dto.systemCode()).isEqualTo("SYS001");
        assertThat(dto.name()).isEqualTo("CAPO NT 300");
        assertThat(dto.brand()).isEqualTo("Kimium");
        assertThat(dto.machineType()).isEqualTo(MachineType.CAPO);
        assertThat(dto.machineStatus()).isEqualTo(MachineStatus.DISPONIVEL);
        assertThat(dto.minimum_stock()).isEqualTo(2);
        assertThat(dto.active()).isTrue();
    }

    @Test
    @DisplayName("Sem máquina cadastrada, devolve lista vazia em vez de estourar")
    void devolveListaVaziaQuandoNaoHaMaquina() {
        when(productInventoryRepository.findByIsMachineTrue()).thenReturn(List.of());

        assertThat(service.getAllMachines()).isEmpty();
    }
}
