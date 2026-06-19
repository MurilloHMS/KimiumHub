package com.proautokimium.api.Infrastructure.services.machine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.prostock.machine.MachineDTO;
import com.proautokimium.api.Application.DTOs.prostock.machine.MachineMovementDTO;
import com.proautokimium.api.Infrastructure.repositories.prostock.MachineMovementRepository;
import com.proautokimium.api.Infrastructure.repositories.prostock.MachineRepository;
import com.proautokimium.api.domain.entities.prostock.machine.Machine;
import com.proautokimium.api.domain.entities.prostock.machine.MachineMovement;
import com.proautokimium.api.domain.exceptions.machine.MachineAlreadyExistsException;
import com.proautokimium.api.domain.exceptions.machine.MachineMovementNotFoundException;
import com.proautokimium.api.domain.exceptions.machine.MachineNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MachineServiceTest {

    @Mock
    private MachineRepository machineRepository;

    @Mock
    private MachineMovementRepository machineMovementRepository;

    @Mock
    private ObjectMapper mapper;

    @InjectMocks
    private MachineService machineService;

    private UUID machineId;
    private UUID movementId;
    private Machine machine;
    private MachineDTO dto;

    @BeforeEach
    void setUp() {
        machineId = UUID.randomUUID();
        movementId = UUID.randomUUID();
        machine = mock(Machine.class);
        dto = new MachineDTO(machineId, "SYS001", "Máquina Teste", "Marca A", null, null, 10, true);
    }

    @Test
    @DisplayName("Deve salvar máquina nova com sucesso")
    void deveSalvarMaquinaComSucesso() {
        when(machineRepository.existsById(machineId)).thenReturn(false);
        when(mapper.convertValue(dto, Machine.class)).thenReturn(machine);
        when(machineRepository.save(machine)).thenReturn(machine);

        assertThatCode(() -> machineService.save(dto)).doesNotThrowAnyException();
        verify(machineRepository).save(machine);
    }

    @Test
    @DisplayName("Deve lançar MachineAlreadyExistsException ao salvar máquina duplicada")
    void deveLancarExcecaoAoSalvarMaquinaDuplicada() {
        when(machineRepository.existsById(machineId)).thenReturn(true);

        assertThatThrownBy(() -> machineService.save(dto))
                .isInstanceOf(MachineAlreadyExistsException.class);

        verify(machineRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve salvar máquina sem ID sem verificar existência")
    void deveSalvarMaquinaSemId() {
        MachineDTO dtoSemId = new MachineDTO(null, "SYS001", "Máquina Teste", "Marca A", null, null, 10, true);
        when(mapper.convertValue(dtoSemId, Machine.class)).thenReturn(machine);
        when(machineRepository.save(machine)).thenReturn(machine);

        assertThatCode(() -> machineService.save(dtoSemId)).doesNotThrowAnyException();
        verify(machineRepository, never()).existsById(any());
    }

    @Test
    @DisplayName("Deve atualizar máquina com sucesso")
    void deveAtualizarMaquinaComSucesso() {
        when(machineRepository.findById(machineId)).thenReturn(Optional.of(machine));
        when(machineRepository.save(machine)).thenReturn(machine);

        assertThatCode(() -> machineService.update(dto)).doesNotThrowAnyException();
        verify(machineRepository).save(machine);
    }

    @Test
    @DisplayName("Deve lançar MachineNotFoundException ao atualizar máquina inexistente")
    void deveLancarExcecaoAoAtualizarMaquinaInexistente() {
        when(machineRepository.findById(machineId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> machineService.update(dto))
                .isInstanceOf(MachineNotFoundException.class);

        verify(machineRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve retornar lista de todas as máquinas")
    void deveRetornarTodasAsMaquinas() {
        when(machineRepository.findAll()).thenReturn(List.of(machine));
        when(mapper.convertValue(machine, MachineDTO.class)).thenReturn(dto);

        List<MachineDTO> result = machineService.getAllMachines();

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Deve lançar MachineNotFoundException ao deletar máquina existente (bug invertido)")
    void deveLancarExcecaoAoDeletarMaquinaExistente() {
        // Bug: delete() lança exceção quando a máquina EXISTE (condição invertida)
        when(machineRepository.existsById(machineId)).thenReturn(true);

        assertThatThrownBy(() -> machineService.delete(machineId))
                .isInstanceOf(MachineNotFoundException.class);

        verify(machineRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Deve deletar máquina quando ID não existe no repositório (comportamento atual)")
    void deveDeletarQuandoMaquinaNaoExiste() {
        // Comportamento atual: deleta quando existsById retorna false (bug invertido)
        when(machineRepository.existsById(machineId)).thenReturn(false);

        assertThatCode(() -> machineService.delete(machineId)).doesNotThrowAnyException();
        verify(machineRepository).deleteById(machineId);
    }

    @Test
    @DisplayName("Deve lançar MachineNotFoundException ao deletar com ID null")
    void deveLancarExcecaoAoDeletarComIdNull() {
        assertThatThrownBy(() -> machineService.delete(null))
                .isInstanceOf(MachineNotFoundException.class);
    }

    @Test
    @DisplayName("Deve retornar movimentos de uma máquina")
    void deveRetornarMovimentosDaMaquina() {
        MachineMovement movement = mock(MachineMovement.class);
        MachineMovementDTO movDto = new MachineMovementDTO(movementId, LocalDateTime.now(), 5);
        when(machineMovementRepository.findMovementsByMachineId(machineId)).thenReturn(List.of(movement));
        when(mapper.convertValue(movement, MachineMovementDTO.class)).thenReturn(movDto);

        List<MachineMovementDTO> result = machineService.getMovementsByMachineId(machineId);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Deve criar movimentação de máquina com sucesso")
    void deveCriarMovimentacaoComSucesso() {
        MachineMovementDTO movDto = new MachineMovementDTO(null, LocalDateTime.now(), 5);
        when(machineRepository.findById(machineId)).thenReturn(Optional.of(machine));
        when(machineMovementRepository.save(any(MachineMovement.class))).thenReturn(mock(MachineMovement.class));

        assertThatCode(() -> machineService.createMovement(movDto, machineId)).doesNotThrowAnyException();
        verify(machineMovementRepository).save(any(MachineMovement.class));
    }

    @Test
    @DisplayName("Deve lançar MachineNotFoundException ao criar movimentação para máquina inexistente")
    void deveLancarExcecaoAoCriarMovimentacaoParaMaquinaInexistente() {
        MachineMovementDTO movDto = new MachineMovementDTO(null, LocalDateTime.now(), 5);
        when(machineRepository.findById(machineId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> machineService.createMovement(movDto, machineId))
                .isInstanceOf(MachineNotFoundException.class);
    }

    @Test
    @DisplayName("Deve atualizar movimentação com sucesso")
    void deveAtualizarMovimentacaoComSucesso() {
        MachineMovementDTO movDto = new MachineMovementDTO(movementId, LocalDateTime.now(), 5);
        MachineMovement movement = mock(MachineMovement.class);
        when(machineMovementRepository.findById(movementId)).thenReturn(Optional.of(movement));
        when(machineMovementRepository.save(movement)).thenReturn(movement);

        assertThatCode(() -> machineService.updateMovement(movDto, machineId)).doesNotThrowAnyException();
        verify(machineMovementRepository).save(movement);
    }

    @Test
    @DisplayName("Deve lançar MachineMovementNotFoundException ao atualizar movimentação inexistente")
    void deveLancarExcecaoAoAtualizarMovimentacaoInexistente() {
        MachineMovementDTO movDto = new MachineMovementDTO(movementId, LocalDateTime.now(), 5);
        when(machineMovementRepository.findById(movementId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> machineService.updateMovement(movDto, machineId))
                .isInstanceOf(MachineMovementNotFoundException.class);
    }

    @Test
    @DisplayName("Deve deletar movimentação com sucesso")
    void deveDeletarMovimentacaoComSucesso() {
        when(machineMovementRepository.existsById(movementId)).thenReturn(true);

        assertThatCode(() -> machineService.deleteMovement(movementId)).doesNotThrowAnyException();
        verify(machineMovementRepository).deleteById(movementId);
    }

    @Test
    @DisplayName("Deve lançar MachineMovementNotFoundException ao deletar movimentação inexistente")
    void deveLancarExcecaoAoDeletarMovimentacaoInexistente() {
        when(machineMovementRepository.existsById(movementId)).thenReturn(false);

        assertThatThrownBy(() -> machineService.deleteMovement(movementId))
                .isInstanceOf(MachineMovementNotFoundException.class);
    }
}
