package com.proautokimium.api.Infrastructure.services.machine;

import com.proautokimium.api.Application.DTOs.prostock.machine.ReconcileDTO;
import com.proautokimium.api.Infrastructure.exceptions.product.ProductNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.prostock.ProductInventoryRepository;
import com.proautokimium.api.Infrastructure.repositories.prostock.ProductMovementRepository;
import com.proautokimium.api.Infrastructure.repositories.prostock.RegisterRepository;
import com.proautokimium.api.domain.entities.prostock.MovementInventory;
import com.proautokimium.api.domain.entities.prostock.ProductInventory;
import com.proautokimium.api.domain.entities.prostock.machine.MachineRegister;
import com.proautokimium.api.domain.enums.MachineStatus;
import com.proautokimium.api.domain.exceptions.machine.MachineRegisterNotFoundException;
import com.proautokimium.api.domain.exceptions.machine.ReconciliationMismatchException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Concilia o estoque de máquina com a programação.
 *
 * É o único lugar do sistema que escreve nos dois lados, e isso é desenho.
 * `includeMovement` e `RegisterService.update` continuam mexendo cada um no
 * seu — se um disparasse o outro, sincronização de mão dupla viraria laço
 * infinito. Quem quer conciliar chama aqui, de propósito.
 */
@Service
public class MachineReconciliationService {

    public MachineReconciliationService(ProductInventoryRepository productRepository, ProductMovementRepository movementRepository, RegisterRepository registerRepository) {
        this.productRepository = productRepository;
        this.movementRepository = movementRepository;
        this.registerRepository = registerRepository;
    }

    /**
     * Lança o movimento e ajusta a programação, ou não faz nada.
     *
     * A ordem não é indiferente: as programações são escritas **antes** do
     * movimento porque são a parte mais frágil — id que não existe, máquina
     * errada, status impossível. A transação desfaria tudo de qualquer forma,
     * mas deixar a falha provável acontecer primeiro evita um log com uma
     * escrita e um erro, que custa caro para quem depura depois.
     */
    @Transactional
    public void reconcile(ReconcileDTO dto){
        ProductInventory machine = productRepository.findBySystemCode(dto.systemCode())
                .orElseThrow(ProductNotFoundException::new);

        List<UUID> toDeliver = dto.registersToDeliver() == null ? List.of() : dto.registersToDeliver();
        int toCreate = dto.registersToCreate() == null ? 0 : dto.registersToCreate();

        assertCountsBalance(dto.delta(), toDeliver, toCreate);

        int resulting = currentStock(machine) + dto.delta();
        if(resulting < 0){
            throw new ReconciliationMismatchException("O lançamento deixaria o estoque em " + resulting + ".");
        }

        if(dto.delta() > 0){
            createSchedules(machine, toCreate);
        }else {
            deliverSchedules(machine, toDeliver);
        }

        saveMovement(machine, resulting, dto.movementDate());
    }

    /**
     * Quanto o estoque anda quando o status de uma programação muda.
     *
     * Lógica pura de propósito: sem repositório e sem entidade, ela se testa
     * com uma linha por caso — e é ela que decide se a tela pergunta algo.
     *
     * A regra é "só ENTREGUE", mas com uma segunda metade que não é óbvia: o
     * ajuste só vale quando o **outro lado** da transição está em estoque. Sem
     * isso, AGUARDANDO_AQUISICAO → ENTREGUE baixaria 1 de um estoque onde a
     * máquina nunca entrou.
     */
    public static int stockDeltaFor(MachineStatus before, MachineStatus after) {
        // Linha nova não tem "antes". Nascer em estoque é entrada.
        if (before == null) return IN_STOCK.contains(after) ? 1 : 0;

        if (IN_STOCK.contains(before) && after == MachineStatus.ENTREGUE) return -1;
        if (before == MachineStatus.ENTREGUE && IN_STOCK.contains(after)) return 1;

        return 0;
    }

    /**
     * Lança a movimentação de uma programação que mudou de lado.
     *
     * Reaproveita o `currentStock` e o `saveMovement` do `reconcile`, então
     * continua existindo **um único lugar** que escreve movimento de máquina.
     *
     * Chamado de dentro do `RegisterService`, que já é `@Transactional`: a
     * linha e o movimento caem juntos ou não caem.
     */
    @Transactional
    public void applyScheduleStockChange(ProductInventory machine, int delta, LocalDateTime when){
        if(delta == 0) return;

        int resulting = currentStock(machine) + delta;
        if(resulting < 0){
            throw new ReconciliationMismatchException("O lançamento deixaria o estoque em " + resulting + ".");
        }
        saveMovement(machine, resulting, when);
    }

    /**
     * Nascem sem cliente e sem previsão.
     *
     * É de propósito: a máquina chegou, mas ninguém decidiu o destino dela
     * ainda. Elas caem exatamente na lista "Sem previsão" do Hub, que é onde
     * alguém vai encontrá-las para programar.
     */
    private void createSchedules(ProductInventory machine, int count){
        for(int i = 0; i < count; i++){
            MachineRegister schedule = new MachineRegister(machine);
            schedule.setStatus(MachineStatus.DISPONIVEL);
            registerRepository.save(schedule);
        }
    }

    /**
     * As duas checagens que o `assertCountsBalance` não conseguia fazer.
     *
     * Lá só existiam números; aqui existe a entidade. Programação de outra
     * máquina tiraria do estoque de um modelo a unidade de outro, e uma já
     * entregue faria o movimento sair como se algo tivesse saído do galpão sem
     * nada ter saído.
     */
    private void deliverSchedules(ProductInventory machine, List<UUID> ids){
        for(UUID id : ids){
            MachineRegister schedule = registerRepository.findById(id)
                    .orElseThrow(MachineRegisterNotFoundException::new);

            if(!schedule.getMachine().getId().equals(machine.getId())){
                throw new ReconciliationMismatchException("Uma das programações escolhidas é de outra máquina");
            }

            if(!IN_STOCK.contains(schedule.getStatus())){
                throw new ReconciliationMismatchException("Uma das programações escolhidas não está em estoque");
            }
            schedule.setStatus(MachineStatus.ENTREGUE);
            registerRepository.save(schedule);
        }
    }

    private void saveMovement(ProductInventory machine, int resulting, LocalDateTime when) {
        MovementInventory movement = new MovementInventory();
        movement.setProduct(machine);
        movement.setQuantity(resulting);
        movement.setMovementDate(when != null ? when : LocalDateTime.now());
        movementRepository.save(movement);
    }

    /** Sem movimento nenhum o estoque é zero — não é erro, é máquina nova. */
    private int currentStock(ProductInventory machine) {
        return movementRepository
                .findTopByProductOrderByCreatedAtDescIdDesc(machine)
                .map(MovementInventory::getQuantity)
                .orElse(0);
    }

    /**
     * Confere se o movimento e as programações contam a mesma história.
     *
     * Não escreve nada: só recusa. Assim a exceção sai antes de qualquer coisa
     * chegar no banco, e o método fica sendo lógica pura — sem campo, sem
     * repositório, fácil de testar.
     */
    private static void assertCountsBalance(int delta, List<UUID> toDeliver, int toCreate){
        if(delta == 0)
            throw new ReconciliationMismatchException("Informe uma quantidade diferente de Zero");

        if(delta > 0){
            if (!toDeliver.isEmpty()) {
                throw new ReconciliationMismatchException("Entrada não entrega programação");
            }

            if(toCreate != delta) {
                throw new ReconciliationMismatchException("Entrada de " + delta + " precisa criar " + delta + " programações, e vieram " + toCreate + ".");
            }
            return;
        }

        int outgoing  = -delta;

        if(toCreate != 0)
            throw new ReconciliationMismatchException("Saída não cria programação.");

        if(toDeliver.size() != outgoing){
            throw new ReconciliationMismatchException("Saída de " + outgoing + " precisa de " + outgoing + " programações, e vieram " + toDeliver.size() + ".");
        }

        // Duas vezes a mesma programação passaria pela contagem acima e tiraria
        // uma máquina só do galpão.
        if (toDeliver.stream().distinct().count() != toDeliver.size()) {
            throw new ReconciliationMismatchException("A mesma programação foi escolhida mais de uma vez.");
        }
    }

    /**
     * O que conta como "está no galpão".
     *
     * REFORMA entra: fisicamente a máquina está lá, mesmo sem poder ser
     * vendida. LIBERAR_EQUIPAMENTOS e AGUARDANDO_AQUISICAO ficam de fora.
     */
    public static final Set<MachineStatus> IN_STOCK = EnumSet.of(
            MachineStatus.DISPONIVEL, MachineStatus.RESERVADA, MachineStatus.REFORMA);

    private final ProductInventoryRepository productRepository;
    private final ProductMovementRepository movementRepository;
    private final RegisterRepository registerRepository;


}
