package com.proautokimium.api.Infrastructure.services.machine;

import com.proautokimium.api.Application.DTOs.prostock.machine.CreateRegisterDTO;
import com.proautokimium.api.Application.DTOs.prostock.machine.ResponseRegisterDTO;
import com.proautokimium.api.Application.DTOs.prostock.machine.UpdateRegisterDTO;
import com.proautokimium.api.Application.DTOs.prostock.machine.ScheduleChangeDTO;
import com.proautokimium.api.Application.DTOs.prostock.machine.ScheduleSlipDTO;
import com.proautokimium.api.Infrastructure.repositories.prostock.MachineScheduleChangeRepository;
import com.proautokimium.api.Infrastructure.repositories.prostock.ProductInventoryRepository;
import com.proautokimium.api.domain.entities.prostock.machine.MachineScheduleChange;
import com.proautokimium.api.domain.enums.MachineStatus;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
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

    private final MachineScheduleChangeRepository scheduleChangeRepository;
    private final MachineReconciliationService reconciliationService;
    private final Clock clock;

    /**
     * O único nome de campo que aparece fora do `retrato`.
     *
     * O Hub conta adiamentos, e adiamento é só este campo. A string está aqui e
     * não solta nos dois lugares porque errar a letra não daria erro nenhum:
     * daria um Hub que conta zero para sempre.
     */
    private static final String CAMPO_PREVISAO = "previsao";

    public RegisterService(RegisterRepository registerRepository,
                           ProductInventoryRepository productRepository,
                           MachineScheduleChangeRepository scheduleChangeRepository,
                           MachineReconciliationService reconciliationService, Clock clock) {
        this.scheduleChangeRepository = scheduleChangeRepository;
        this.registerRepository = registerRepository;
        this.productRepository = productRepository;
        this.reconciliationService = reconciliationService;
        this.clock = clock;
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

        if(dto.adjustStock()){
            int delta = MachineReconciliationService.stockDeltaFor(null, register.getStatus());
            reconciliationService.applyScheduleStockChange(machine, delta, LocalDateTime.now(clock));
        }
        return registerRepository.save(register);
    }

    @Transactional
    public MachineRegister update(UpdateRegisterDTO dto, UUID registerId){
        MachineRegister register = registerRepository.findById(registerId)
                .orElseThrow(MachineRegisterNotFoundException::new);

        // Lido ANTES do fromDto: depois dele os valores antigos já se perderam,
        // e não há como saber de onde cada campo veio.
        Map<String, String> antes = retrato(register);
        // Pelo mesmo motivo, e é o que decide se o estoque anda.
        MachineStatus statusAnterior = register.getStatus();

        register.fromDto(dto);
        registrarAlteracoes(register, antes, retrato(register), dto.motivoAlteracaoPrevisao());

        if(dto.adjustStock()){
            int delta = MachineReconciliationService.stockDeltaFor(statusAnterior, register.getStatus());
            reconciliationService.applyScheduleStockChange(register.getMachine(), delta, LocalDateTime.now(clock));
        }

        return registerRepository.save(register);
    }

    /**
     * O retrato de uma programação, campo a campo, como texto.
     *
     * Tirado antes e depois do `fromDto`, a diferença entre os dois retratos
     * **é** o histórico. Um mapa e não oito variáveis: o que muda de um campo
     * para o outro é o valor, não a estrutura, e é a mesma razão pela qual a
     * tabela tem uma coluna `campo` em vez de oito tabelas. Um nono campo entra
     * aqui, numa linha, e o resto continua funcionando sem saber.
     *
     * `LinkedHashMap` porque a ordem é a da tela — quando duas alterações caem
     * no mesmo instante, o histórico as mostra na ordem em que a pessoa as vê.
     *
     * Observação fica de fora, por escolha do time.
     *
     * Tudo vira texto porque a coluna é `varchar`: data em ISO-8601, status
     * pela chave do enum e não pelo rótulo. O rótulo muda quando alguém edita
     * uma tradução, e um histórico que muda de conteúdo sozinho não é
     * histórico.
     */
    private static Map<String, String> retrato(MachineRegister register) {
        Map<String, String> valores = new LinkedHashMap<>();
        valores.put("nomeCliente", texto(register.getNomeCliente()));
        valores.put("regiao",      texto(register.getRegiao()));
        valores.put("solicitante", texto(register.getSolicitante()));
        valores.put(CAMPO_PREVISAO, register.getPrevisaoEntrega() == null
                ? null : register.getPrevisaoEntrega().toString());
        valores.put("consultor",   texto(register.getConsultor()));
        valores.put("tecnico",     texto(register.getTecnico()));
        valores.put("tag",         texto(register.getTag()));
        valores.put("status",      register.getStatus() == null
                ? null : register.getStatus().name());
        return valores;
    }

    /**
     * Vazio e nulo são a mesma ausência.
     *
     * Sem isto, uma tela que manda `""` onde o banco tem `null` gravaria uma
     * alteração de nada para nada, em toda edição, em todo campo em branco.
     */
    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    /**
     * Uma linha de histórico para cada campo que mudou. Nenhuma para os outros.
     *
     * O motivo é opcional e vale para a edição inteira: quando alguém troca a
     * previsão e o técnico de uma vez, a mesma justificativa fica nas duas
     * linhas. Separar um motivo por campo exigiria um diálogo por campo, e
     * ninguém preencheria oito.
     *
     * Preencher um campo em branco **não** conta como alteração: completar um
     * cadastro não é decisão a justificar, e não há valor anterior de onde a
     * mudança tenha partido. Vale para os oito campos.
     *
     * Apagar conta. É o oposto e é o par que protege a regra: uma versão que
     * ignorasse tudo que envolve vazio faria limpar o técnico de uma linha
     * sumir do histórico sem deixar rastro.
     */
    private void registrarAlteracoes(MachineRegister register,
                                     Map<String, String> antes,
                                     Map<String, String> depois,
                                     String motivo) {
        String justificativa = texto(motivo);

        antes.forEach((campo, anterior) -> {
            // Preencher um campo vazio não é alteração: não havia valor de onde
            // ter saído. A tela já não pergunta o motivo nesse caso, e sem esta
            // guarda o banco guardaria o que a tela decidiu não perguntar.
            if (anterior == null) return;

            String novo = depois.get(campo);
            if (Objects.equals(anterior, novo)) return;

            scheduleChangeRepository.save(new MachineScheduleChange(
                    register, campo, anterior, novo, justificativa));
        });
    }

    /** O histórico de adiamentos de uma programação, mais recente primeiro. */
    /**
     * Todos os adiamentos desde uma data, com de quem são.
     *
     * O Hub agrega isto: quantos no mês, quantas máquinas adiaram mais de uma
     * vez, qual o atraso mediano. A conta fica na tela de propósito — são
     * dezenas de linhas por mês, e cada recorte novo viraria um endpoint novo.
     */
    public List<ScheduleSlipDTO> slipsSince(LocalDateTime from){
        return scheduleChangeRepository.findSince(from)
                .stream()
                // Adiamento é só previsão. Sem este filtro, trocar um técnico
                // entraria na conta de "adiamentos no mês" do Hub.
                .filter(c -> CAMPO_PREVISAO.equals(c.getCampo()))
                // Preencher a data pela primeira vez não é adiamento: não havia
                // de onde adiar. O histórico da linha guarda; a conta do Hub,
                // não. É o que mantém o número igual ao de antes desta mudança.
                .filter(c -> c.getValorAnterior() != null)
                .map(c -> new ScheduleSlipDTO(
                        c.getRegister().getId(),
                        c.getRegister().getNomeCliente(),
                        c.getRegister().getMachine().getName(),
                        data(c.getValorAnterior()),
                        data(c.getValorNovo()),
                        c.getMotivo(),
                        c.getChangedAt()
                )).toList();
    }

    /**
     * O caminho de volta do texto para data.
     *
     * Sem perda: quem gravou foi `LocalDateTime.toString()`, que é ISO-8601, e
     * é o mesmo formato que a V91 usou no backfill das linhas antigas — de
     * propósito, e a razão de ela usar `to_char` e não um cast direto, que sai
     * com espaço no lugar do `T` e estoura aqui.
     */
    private static LocalDateTime data(String valor) {
        return valor == null ? null : LocalDateTime.parse(valor);
    }

    /**
     * O histórico de uma programação, mais recente primeiro — todos os campos.
     *
     * O nome ficou de quando só havia previsão. Renomear mexe no controller e
     * fica para o commit de dívida de nome, junto com
     * `MotivoDaAlteracaoObrigatorioException`.
     */
    public List<ScheduleChangeDTO> listarAlteracoesDePrevisao(UUID registerId){
        return scheduleChangeRepository.findByRegisterIdOrderByChangedAtDesc(registerId)
                .stream()
                .map(c -> new ScheduleChangeDTO(
                        c.getId(),
                        c.getCampo(),
                        c.getValorAnterior(),
                        c.getValorNovo(),
                        c.getMotivo(),
                        c.getChangedBy(),
                        c.getChangedAt()))
                .toList();
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
