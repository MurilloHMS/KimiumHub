package com.proautokimium.api.Infrastructure.services.machine;

import com.proautokimium.api.Application.DTOs.prostock.machine.CreateRegisterDTO;
import com.proautokimium.api.Application.DTOs.prostock.machine.UpdateRegisterDTO;
import com.proautokimium.api.Infrastructure.repositories.prostock.MachineScheduleChangeRepository;
import com.proautokimium.api.Infrastructure.repositories.prostock.ProductInventoryRepository;
import com.proautokimium.api.Infrastructure.repositories.prostock.RegisterRepository;
import com.proautokimium.api.domain.entities.prostock.ProductInventory;
import com.proautokimium.api.domain.entities.prostock.machine.MachineRegister;
import com.proautokimium.api.domain.entities.prostock.machine.MachineScheduleChange;
import com.proautokimium.api.domain.enums.MachineStatus;
import com.proautokimium.api.domain.exceptions.machine.MotivoDaAlteracaoObrigatorioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * A regra do motivo de adiamento.
 *
 * O que estes testes protegem é a **fronteira**: adiar exige justificativa,
 * preencher pela primeira vez não. Errar para o lado rígido é pior do que
 * parece — cobrar texto de quem só está completando cadastro ensina a digitar
 * "ok" para passar da tela, e aí o campo deixa de valer para quem adia de
 * verdade.
 */
@ExtendWith(MockitoExtension.class)
class RegisterServiceTest {

    private static final UUID REGISTER_ID = UUID.randomUUID();
    private static final LocalDateTime ONTEM = LocalDateTime.of(2026, 9, 1, 10, 0);
    private static final LocalDateTime SEMANA_QUE_VEM = LocalDateTime.of(2026, 9, 15, 10, 0);

    @Mock private RegisterRepository registerRepository;
    @Mock private ProductInventoryRepository productRepository;
    @Mock private MachineScheduleChangeRepository scheduleChangeRepository;
    @Mock private MachineReconciliationService reconciliationService;

    /**
     * Construído na mão, não por `@InjectMocks`, porque o `Clock` não é mock:
     * é um relógio parado de verdade. Um `Clock` mockado devolveria `null` no
     * `instant()` e o `LocalDateTime.now(clock)` estouraria — e a data do
     * movimento é justamente o que se quer afirmar.
     */
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-09-10T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));

    private RegisterService service;

    @BeforeEach
    void setUp() {
        service = new RegisterService(registerRepository, productRepository,
                scheduleChangeRepository, reconciliationService, CLOCK);
    }

    private MachineRegister registroCom(LocalDateTime previsao) {
        MachineRegister register = new MachineRegister(new ProductInventory());
        register.setPrevisaoEntrega(previsao);
        return register;
    }

    /** `adjustStock` fica em false: estes testes são sobre o motivo, não estoque. */
    private UpdateRegisterDTO dto(LocalDateTime previsao, String motivo) {
        return new UpdateRegisterDTO("Cliente", "T-1", "Solicitante",
                MachineStatus.DISPONIVEL, "Observação", previsao,
                "Técnico", "Região", "Consultor", motivo, false);
    }

    /**
     * Só o `findById`, para os casos que **terminam em recusa**.
     *
     * Stubar o `save` aqui faria o Mockito reclamar de stub não usado — e a
     * reclamação seria justa: nesses testes o save não deve acontecer, e é
     * isso que eles afirmam. Separar os dois helpers mantém o strict stubs
     * trabalhando a favor em vez de precisar de `lenient()`.
     */
    private void registroExistente(MachineRegister register) {
        when(registerRepository.findById(REGISTER_ID)).thenReturn(Optional.of(register));
    }

    /** Para os casos que chegam até o fim e gravam. */
    private void registroExistenteQueSalva(MachineRegister register) {
        registroExistente(register);
        when(registerRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    // ─── Não é adiamento ─────────────────────────────────────────────────────

    /**
     * Preencher a previsão de um registro que não tinha data é completar
     * cadastro. Exigir motivo aqui seria pedir justificativa por informar algo.
     */
    @Test
    @DisplayName("Preencher previsão vazia não exige motivo e não gera histórico")
    void preencherPelaPrimeiraVezNaoExigeMotivo() {
        registroExistenteQueSalva(registroCom(null));

        assertThatCode(() -> service.update(dto(SEMANA_QUE_VEM, null), REGISTER_ID))
                .doesNotThrowAnyException();

        verifyNoInteractions(scheduleChangeRepository);
    }

    @Test
    @DisplayName("Alterar outro campo, mantendo a previsão, não exige motivo")
    void mudarOutroCampoNaoExigeMotivo() {
        registroExistenteQueSalva(registroCom(ONTEM));

        assertThatCode(() -> service.update(dto(ONTEM, null), REGISTER_ID))
                .doesNotThrowAnyException();

        verifyNoInteractions(scheduleChangeRepository);
    }

    // ─── É adiamento ─────────────────────────────────────────────────────────

    /**
     * Sem o rollback da transação, a data mudaria e o motivo não seria gravado
     * — exatamente o buraco que a regra existe para fechar. Aqui o que se
     * verifica é que nada foi salvo.
     */
    @Test
    @DisplayName("Mudar a previsão sem motivo é recusado, e nada é gravado")
    void mudarPrevisaoSemMotivoEhRecusado() {
        registroExistente(registroCom(ONTEM));

        assertThatThrownBy(() -> service.update(dto(SEMANA_QUE_VEM, null), REGISTER_ID))
                .isInstanceOf(MotivoDaAlteracaoObrigatorioException.class);

        verify(scheduleChangeRepository, never()).save(any());
        verify(registerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Motivo em branco vale o mesmo que motivo ausente")
    void motivoEmBrancoNaoPassa() {
        registroExistente(registroCom(ONTEM));

        assertThatThrownBy(() -> service.update(dto(SEMANA_QUE_VEM, "   "), REGISTER_ID))
                .isInstanceOf(MotivoDaAlteracaoObrigatorioException.class);
    }

    @Test
    @DisplayName("Mudar a previsão com motivo grava o antes e o depois")
    void adiamentoGravaHistorico() {
        registroExistenteQueSalva(registroCom(ONTEM));

        service.update(dto(SEMANA_QUE_VEM, "  Peça atrasada no fornecedor  "), REGISTER_ID);

        ArgumentCaptor<MachineScheduleChange> captor =
                ArgumentCaptor.forClass(MachineScheduleChange.class);
        verify(scheduleChangeRepository).save(captor.capture());

        MachineScheduleChange gravado = captor.getValue();
        assertThat(gravado.getPrevisaoAnterior()).isEqualTo(ONTEM);
        assertThat(gravado.getPrevisaoNova()).isEqualTo(SEMANA_QUE_VEM);
        // `trim` no motivo: espaço sobrando vira ruído em relatório.
        assertThat(gravado.getMotivo()).isEqualTo("Peça atrasada no fornecedor");
    }

    /**
     * Apagar a previsão também é alteração — e é a mais grave, porque a máquina
     * some das "Próximas saídas" sem ninguém perceber.
     */
    @Test
    @DisplayName("Apagar a previsão exige motivo e grava com data nova nula")
    void apagarPrevisaoTambemEhAlteracao() {
        registroExistente(registroCom(ONTEM));

        assertThatThrownBy(() -> service.update(dto(null, null), REGISTER_ID))
                .isInstanceOf(MotivoDaAlteracaoObrigatorioException.class);

        registroExistenteQueSalva(registroCom(ONTEM));
        service.update(dto(null, "Cliente cancelou"), REGISTER_ID);

        ArgumentCaptor<MachineScheduleChange> captor =
                ArgumentCaptor.forClass(MachineScheduleChange.class);
        verify(scheduleChangeRepository).save(captor.capture());

        assertThat(captor.getValue().getPrevisaoAnterior()).isEqualTo(ONTEM);
        assertThat(captor.getValue().getPrevisaoNova()).isNull();
    }

    /**
     * **O teste que justifica ser tabela e não coluna.**
     *
     * Com `motivo_alteracao` numa coluna de machine_registers, o segundo
     * adiamento apagaria o primeiro — e some justamente a informação que
     * interessa, que é a máquina viver atrasando.
     */
    @Test
    @DisplayName("Dois adiamentos deixam duas linhas de histórico")
    void doisAdiamentosDeixamDuasLinhas() {
        MachineRegister register = registroCom(ONTEM);
        registroExistenteQueSalva(register);

        service.update(dto(SEMANA_QUE_VEM, "Peça atrasada"), REGISTER_ID);
        service.update(dto(SEMANA_QUE_VEM.plusDays(7), "Técnico de férias"), REGISTER_ID);

        ArgumentCaptor<MachineScheduleChange> captor =
                ArgumentCaptor.forClass(MachineScheduleChange.class);
        verify(scheduleChangeRepository, times(2)).save(captor.capture());

        assertThat(captor.getAllValues())
                .extracting(MachineScheduleChange::getMotivo)
                .containsExactly("Peça atrasada", "Técnico de férias");

        // A segunda alteração parte de onde a primeira chegou.
        assertThat(captor.getAllValues().get(1).getPrevisaoAnterior()).isEqualTo(SEMANA_QUE_VEM);
    }

    // ─── Parte 4: a programação mexendo no estoque ───────────────────────────

    private static final UUID MACHINE_ID = UUID.randomUUID();

    private ProductInventory maquina() {
        ProductInventory machine = new ProductInventory();
        machine.setMachine(true);
        return machine;
    }

    private void maquinaExiste(ProductInventory machine) {
        when(productRepository.findById(MACHINE_ID)).thenReturn(Optional.of(machine));
    }

    private CreateRegisterDTO createDto(MachineStatus status, boolean adjustStock) {
        return new CreateRegisterDTO(MACHINE_ID, "Cliente", "T-1", "Solicitante",
                status, "Observação", null, "Técnico", "Região", "Consultor", adjustStock);
    }

    private UpdateRegisterDTO updateDto(MachineStatus status, boolean adjustStock) {
        return new UpdateRegisterDTO("Cliente", "T-1", "Solicitante",
                status, "Observação", ONTEM, "Técnico", "Região", "Consultor", null, adjustStock);
    }

    private MachineRegister registroComStatus(ProductInventory machine, MachineStatus status) {
        MachineRegister register = new MachineRegister(machine);
        register.setPrevisaoEntrega(ONTEM);
        register.setStatus(status);
        return register;
    }

    /**
     * **O teste que protege a importação de planilha.**
     *
     * `programacao-import` cria as linhas pelo mesmo `create`. Se o ajuste
     * fosse inferido do status em vez de pedido, importar 200 linhas lançaria
     * 200 movimentações — e o estoque de todas as máquinas iria para o espaço
     * de uma vez.
     */
    @Test
    @DisplayName("Sem adjustStock, criar linha não encosta no estoque")
    void criarSemPedirAjusteNaoMexeNoEstoque() {
        maquinaExiste(maquina());
        when(registerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.create(createDto(MachineStatus.DISPONIVEL, false));

        verifyNoInteractions(reconciliationService);
    }

    @Test
    @DisplayName("Com adjustStock, linha nova em estoque soma 1 na data do relógio")
    void criarComAjusteSoma() {
        ProductInventory machine = maquina();
        maquinaExiste(machine);
        when(registerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.create(createDto(MachineStatus.DISPONIVEL, true));

        verify(reconciliationService).applyScheduleStockChange(
                eq(machine), eq(1), eq(LocalDateTime.now(CLOCK)));
    }

    /** Nascer ENTREGUE é registro do que já saiu — não pode somar nada. */
    @Test
    @DisplayName("Linha nova nascendo ENTREGUE não soma")
    void criarEntregueNaoSoma() {
        ProductInventory machine = maquina();
        maquinaExiste(machine);
        when(registerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.create(createDto(MachineStatus.ENTREGUE, true));

        verify(reconciliationService).applyScheduleStockChange(eq(machine), eq(0), any());
    }

    @Test
    @DisplayName("Marcar ENTREGUE baixa 1")
    void entregarBaixaUm() {
        ProductInventory machine = maquina();
        registroExistenteQueSalva(registroComStatus(machine, MachineStatus.DISPONIVEL));

        service.update(updateDto(MachineStatus.ENTREGUE, true), REGISTER_ID);

        verify(reconciliationService).applyScheduleStockChange(eq(machine), eq(-1), any());
    }

    /**
     * O caso que ele reportou em 2026-08-26: voltar de ENTREGUE não devolvia a
     * máquina para o estoque.
     */
    @Test
    @DisplayName("Voltar de ENTREGUE devolve 1 ao estoque")
    void voltarDeEntregueDevolveUm() {
        ProductInventory machine = maquina();
        registroExistenteQueSalva(registroComStatus(machine, MachineStatus.ENTREGUE));

        service.update(updateDto(MachineStatus.DISPONIVEL, true), REGISTER_ID);

        verify(reconciliationService).applyScheduleStockChange(eq(machine), eq(1), any());
    }

    /** O status antigo é lido antes do `fromDto`; sem isso o delta sairia zero. */
    @Test
    @DisplayName("Sem adjustStock, mudar status não encosta no estoque")
    void mudarStatusSemPedirAjusteNaoMexe() {
        registroExistenteQueSalva(registroComStatus(maquina(), MachineStatus.DISPONIVEL));

        service.update(updateDto(MachineStatus.ENTREGUE, false), REGISTER_ID);

        verifyNoInteractions(reconciliationService);
    }
}
