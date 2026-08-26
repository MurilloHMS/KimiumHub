package com.proautokimium.api.Infrastructure.services.machine;

import com.proautokimium.api.Application.DTOs.prostock.machine.ReconcileDTO;
import com.proautokimium.api.Infrastructure.repositories.prostock.ProductInventoryRepository;
import com.proautokimium.api.Infrastructure.repositories.prostock.ProductMovementRepository;
import com.proautokimium.api.Infrastructure.repositories.prostock.RegisterRepository;
import com.proautokimium.api.domain.entities.prostock.MovementInventory;
import com.proautokimium.api.domain.entities.prostock.ProductInventory;
import com.proautokimium.api.domain.entities.prostock.machine.MachineRegister;
import com.proautokimium.api.domain.enums.MachineStatus;
import com.proautokimium.api.domain.exceptions.machine.ReconciliationMismatchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * A conciliação entre estoque e programação.
 *
 * O que estes testes protegem é a recusa. Uma conciliação que aceita conta
 * errada cria exatamente a divergência que ela existe para evitar — e cria
 * escondida, porque os dois lançamentos parecem corretos olhando separado.
 */
@ExtendWith(MockitoExtension.class)
class MachineReconciliationServiceTest {

    private static final String CODE = "MAQ-001";
    private static final LocalDateTime FIXED_DATE = LocalDateTime.of(2026, 9, 10, 12, 0);

    @Mock private ProductInventoryRepository productRepository;
    @Mock private ProductMovementRepository movementRepository;
    @Mock private RegisterRepository registerRepository;

    @InjectMocks private MachineReconciliationService service;

    private ProductInventory machine;

    @BeforeEach
    void setUp() {
        machine = new ProductInventory();
        machine.setSystemCode(CODE);
        // O id vem do banco em produção. Aqui é obrigatório preencher: a
        // comparação "é da mesma máquina?" usa `getId()`, e sem ele o teste
        // falha por NullPointer sem provar nada sobre a regra.
        machine.id = UUID.randomUUID();
    }

    // ─── Apoio ───────────────────────────────────────────────────────────────

    private void machineExists() {
        when(productRepository.findBySystemCode(CODE)).thenReturn(Optional.of(machine));
    }

    private void currentStockIs(int quantity) {
        MovementInventory last = new MovementInventory();
        last.setQuantity(quantity);
        when(movementRepository.findTopByProductOrderByMovementDateDescIdDesc(machine))
                .thenReturn(Optional.of(last));
    }

    private ReconcileDTO entrada(int howMany) {
        return new ReconcileDTO(CODE, howMany, LocalDateTime.now(), List.of(), howMany);
    }

    private ReconcileDTO saida(List<UUID> ids) {
        return new ReconcileDTO(CODE, -ids.size(), LocalDateTime.now(), ids, 0);
    }

    private MachineRegister schedule(UUID id, MachineStatus status, ProductInventory owner) {
        MachineRegister register = new MachineRegister(owner);
        register.setStatus(status);
        when(registerRepository.findById(id)).thenReturn(Optional.of(register));
        return register;
    }

    // ─── A conta tem que fechar ──────────────────────────────────────────────

    /**
     * **O teste central do módulo.**
     *
     * Saída de 3 com 2 programações escolhidas tiraria 3 do estoque e apenas 2
     * da programação. Os dois lançamentos parecem certos isolados, e a
     * divergência só apareceria semanas depois, sem ninguém saber a origem.
     */
    @Test
    @DisplayName("Saída de 3 com 2 programações escolhidas é recusada")
    void saidaComContaAbertaEhRecusada() {
        machineExists();

        ReconcileDTO dto = new ReconcileDTO(CODE, -3, LocalDateTime.now(),
                List.of(UUID.randomUUID(), UUID.randomUUID()), 0);

        assertThatThrownBy(() -> service.reconcile(dto))
                .isInstanceOf(ReconciliationMismatchException.class)
                .hasMessageContaining("precisa de 3");

        verifyNoInteractions(registerRepository);
        verify(movementRepository, never()).save(any());
    }

    @Test
    @DisplayName("Entrada que cria número diferente do delta é recusada")
    void entradaComContaAbertaEhRecusada() {
        machineExists();

        ReconcileDTO dto = new ReconcileDTO(CODE, 3, LocalDateTime.now(), List.of(), 2);

        assertThatThrownBy(() -> service.reconcile(dto))
                .isInstanceOf(ReconciliationMismatchException.class);

        verify(movementRepository, never()).save(any());
    }

    @Test
    @DisplayName("Delta zero é recusado")
    void deltaZeroEhRecusado() {
        machineExists();

        ReconcileDTO dto = new ReconcileDTO(CODE, 0, LocalDateTime.now(), List.of(), 0);

        assertThatThrownBy(() -> service.reconcile(dto))
                .isInstanceOf(ReconciliationMismatchException.class);
    }

    /**
     * A mesma programação duas vezes passaria pela contagem — a lista tem
     * tamanho 2 — mas só uma máquina sairia do galpão.
     */
    @Test
    @DisplayName("A mesma programação escolhida duas vezes é recusada")
    void programacaoRepetidaEhRecusada() {
        machineExists();
        UUID mesma = UUID.randomUUID();

        ReconcileDTO dto = new ReconcileDTO(CODE, -2, LocalDateTime.now(),
                List.of(mesma, mesma), 0);

        assertThatThrownBy(() -> service.reconcile(dto))
                .isInstanceOf(ReconciliationMismatchException.class)
                .hasMessageContaining("mais de uma vez");

        verify(movementRepository, never()).save(any());
    }

    // ─── Entrada ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Entrada de 2 cria 2 programações DISPONIVEL e grava o estoque somado")
    void entradaCriaProgramacoesEGravaEstoque() {
        machineExists();
        currentStockIs(5);

        service.reconcile(entrada(2));

        ArgumentCaptor<MachineRegister> novas = ArgumentCaptor.forClass(MachineRegister.class);
        verify(registerRepository, times(2)).save(novas.capture());

        assertThat(novas.getAllValues())
                .allSatisfy(r -> assertThat(r.getStatus()).isEqualTo(MachineStatus.DISPONIVEL));

        // Nascem sem destino: é o que as coloca na lista "Sem previsão" do Hub.
        assertThat(novas.getAllValues())
                .allSatisfy(r -> assertThat(r.getPrevisaoEntrega()).isNull());

        ArgumentCaptor<MovementInventory> movimento = ArgumentCaptor.forClass(MovementInventory.class);
        verify(movementRepository).save(movimento.capture());
        // A tabela guarda o absoluto resultante, não a diferença.
        assertThat(movimento.getValue().getQuantity()).isEqualTo(7);
    }

    /** Máquina sem movimento nenhum começa do zero, não é erro. */
    @Test
    @DisplayName("Máquina nova, sem histórico, entra a partir de zero")
    void maquinaSemHistoricoComecaDoZero() {
        machineExists();
        when(movementRepository.findTopByProductOrderByMovementDateDescIdDesc(machine))
                .thenReturn(Optional.empty());

        service.reconcile(entrada(3));

        ArgumentCaptor<MovementInventory> movimento = ArgumentCaptor.forClass(MovementInventory.class);
        verify(movementRepository).save(movimento.capture());
        assertThat(movimento.getValue().getQuantity()).isEqualTo(3);
    }

    // ─── Saída ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Saída marca as escolhidas como ENTREGUE e baixa o estoque")
    void saidaEntregaProgramacoesEBaixaEstoque() {
        machineExists();
        currentStockIs(5);

        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        MachineRegister primeira = schedule(a, MachineStatus.DISPONIVEL, machine);
        MachineRegister segunda = schedule(b, MachineStatus.RESERVADA, machine);

        service.reconcile(saida(List.of(a, b)));

        assertThat(primeira.getStatus()).isEqualTo(MachineStatus.ENTREGUE);
        assertThat(segunda.getStatus()).isEqualTo(MachineStatus.ENTREGUE);

        ArgumentCaptor<MovementInventory> movimento = ArgumentCaptor.forClass(MovementInventory.class);
        verify(movementRepository).save(movimento.capture());
        assertThat(movimento.getValue().getQuantity()).isEqualTo(3);
    }

    /**
     * Aceitar isto tiraria do estoque de um modelo a unidade de outro — e os
     * dois ficariam errados de uma vez.
     */
    @Test
    @DisplayName("Programação de outra máquina é recusada, e o movimento não é gravado")
    void programacaoDeOutraMaquinaEhRecusada() {
        machineExists();
        currentStockIs(5);

        ProductInventory outra = new ProductInventory();
        outra.setSystemCode("MAQ-999");
        outra.id = UUID.randomUUID();

        UUID id = UUID.randomUUID();
        schedule(id, MachineStatus.DISPONIVEL, outra);

        assertThatThrownBy(() -> service.reconcile(saida(List.of(id))))
                .isInstanceOf(ReconciliationMismatchException.class)
                .hasMessageContaining("outra máquina");

        // A prova de que a ordem importa: as programações são escritas antes do
        // movimento, então a falha impede o movimento de existir.
        verify(movementRepository, never()).save(any());
    }

    @Test
    @DisplayName("Programação já entregue não pode ser entregue de novo")
    void programacaoJaEntregueEhRecusada() {
        machineExists();
        currentStockIs(5);

        UUID id = UUID.randomUUID();
        schedule(id, MachineStatus.ENTREGUE, machine);

        assertThatThrownBy(() -> service.reconcile(saida(List.of(id))))
                .isInstanceOf(ReconciliationMismatchException.class)
                .hasMessageContaining("não está em estoque");

        verify(movementRepository, never()).save(any());
    }

    @Test
    @DisplayName("Saída maior que o estoque é recusada antes de tocar na programação")
    void estoqueNegativoEhRecusado() {
        machineExists();
        currentStockIs(1);

        ReconcileDTO dto = new ReconcileDTO(CODE, -2, LocalDateTime.now(),
                List.of(UUID.randomUUID(), UUID.randomUUID()), 0);

        assertThatThrownBy(() -> service.reconcile(dto))
                .isInstanceOf(ReconciliationMismatchException.class)
                .hasMessageContaining("estoque em -1");

        verifyNoInteractions(registerRepository);
        verify(movementRepository, never()).save(any());
    }

    // ─── A regra do status (Parte 4) ─────────────────────────────────────────

    /**
     * `stockDeltaFor` é lógica pura: sem banco, sem entidade, sem mock.
     *
     * Por isso cada caso é uma linha. É ela que decide se a tela vai perguntar
     * alguma coisa antes de gravar — errar aqui não quebra teste nenhum de
     * integração, só separa os dois números em silêncio.
     */
    @Test
    @DisplayName("Sair do estoque para ENTREGUE tira 1; voltar devolve 1")
    void entregarTiraEDevolverRepoe() {
        assertThat(MachineReconciliationService.stockDeltaFor(
                MachineStatus.DISPONIVEL, MachineStatus.ENTREGUE)).isEqualTo(-1);
        assertThat(MachineReconciliationService.stockDeltaFor(
                MachineStatus.RESERVADA, MachineStatus.ENTREGUE)).isEqualTo(-1);
        assertThat(MachineReconciliationService.stockDeltaFor(
                MachineStatus.REFORMA, MachineStatus.ENTREGUE)).isEqualTo(-1);

        assertThat(MachineReconciliationService.stockDeltaFor(
                MachineStatus.ENTREGUE, MachineStatus.DISPONIVEL)).isEqualTo(1);
    }

    /**
     * **A segunda metade da regra, a que não é óbvia.**
     *
     * "Só ENTREGUE" sozinho baixaria 1 de um estoque onde a máquina nunca
     * entrou: AGUARDANDO_AQUISICAO é máquina que ainda não chegou, e a Parte 3
     * nem a lista como candidata a sair.
     */
    @Test
    @DisplayName("Entregar o que nunca esteve em estoque não mexe em nada")
    void entregarOQueNaoEstavaEmEstoqueNaoMexe() {
        assertThat(MachineReconciliationService.stockDeltaFor(
                MachineStatus.AGUARDANDO_AQUISICAO, MachineStatus.ENTREGUE)).isZero();
        assertThat(MachineReconciliationService.stockDeltaFor(
                MachineStatus.LIBERAR_EQUIPAMENTOS, MachineStatus.ENTREGUE)).isZero();
        assertThat(MachineReconciliationService.stockDeltaFor(
                MachineStatus.ENTREGUE, MachineStatus.AGUARDANDO_AQUISICAO)).isZero();
    }

    /** Reservar não é entregar: a máquina continua no galpão. */
    @Test
    @DisplayName("Andar entre status de estoque não move nada")
    void andarDentroDoEstoqueNaoMove() {
        assertThat(MachineReconciliationService.stockDeltaFor(
                MachineStatus.DISPONIVEL, MachineStatus.RESERVADA)).isZero();
        assertThat(MachineReconciliationService.stockDeltaFor(
                MachineStatus.RESERVADA, MachineStatus.REFORMA)).isZero();
        assertThat(MachineReconciliationService.stockDeltaFor(
                MachineStatus.DISPONIVEL, MachineStatus.DISPONIVEL)).isZero();
    }

    /**
     * Linha nova não tem "antes". Nascer em estoque é entrada; nascer ENTREGUE
     * é registro do que já saiu, e não pode somar nada.
     */
    @Test
    @DisplayName("Linha nova soma 1 só se nascer em estoque")
    void linhaNovaSomaSoSeNascerEmEstoque() {
        assertThat(MachineReconciliationService.stockDeltaFor(null, MachineStatus.DISPONIVEL)).isEqualTo(1);
        assertThat(MachineReconciliationService.stockDeltaFor(null, MachineStatus.REFORMA)).isEqualTo(1);

        assertThat(MachineReconciliationService.stockDeltaFor(null, MachineStatus.ENTREGUE)).isZero();
        assertThat(MachineReconciliationService.stockDeltaFor(null, MachineStatus.AGUARDANDO_AQUISICAO)).isZero();
    }

    // ─── O lançamento do ±1 ──────────────────────────────────────────────────

    @Test
    @DisplayName("applyScheduleStockChange grava o estoque somado")
    void ajusteDeProgramacaoGravaOEstoque() {
        currentStockIs(4);

        service.applyScheduleStockChange(machine, 1, FIXED_DATE);

        ArgumentCaptor<MovementInventory> movimento = ArgumentCaptor.forClass(MovementInventory.class);
        verify(movementRepository).save(movimento.capture());
        assertThat(movimento.getValue().getQuantity()).isEqualTo(5);
        assertThat(movimento.getValue().getMovementDate()).isEqualTo(FIXED_DATE);
    }

    /** Transição que não cruza a fronteira não pode virar linha no histórico. */
    @Test
    @DisplayName("Delta zero não grava movimento nenhum")
    void deltaZeroNaoGravaMovimento() {
        service.applyScheduleStockChange(machine, 0, FIXED_DATE);

        verifyNoInteractions(movementRepository);
    }

    @Test
    @DisplayName("Ajuste que deixaria o estoque negativo é recusado")
    void ajusteNegativoEhRecusado() {
        currentStockIs(0);

        assertThatThrownBy(() -> service.applyScheduleStockChange(machine, -1, FIXED_DATE))
                .isInstanceOf(ReconciliationMismatchException.class)
                .hasMessageContaining("estoque em -1");

        verify(movementRepository, never()).save(any());
    }

    /** REFORMA está no galpão, mesmo sem poder ser vendida — e pode sair. */
    @Test
    @DisplayName("Máquina em REFORMA conta como estoque e pode ser entregue")
    void reformaContaComoEstoque() {
        machineExists();
        currentStockIs(2);

        UUID id = UUID.randomUUID();
        MachineRegister emReforma = schedule(id, MachineStatus.REFORMA, machine);

        assertThatCode(() -> service.reconcile(saida(List.of(id))))
                .doesNotThrowAnyException();

        assertThat(emReforma.getStatus()).isEqualTo(MachineStatus.ENTREGUE);
    }
}
