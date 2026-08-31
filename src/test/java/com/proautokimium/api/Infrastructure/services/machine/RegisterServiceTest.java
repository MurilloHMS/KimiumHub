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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * O histórico de alterações da programação.
 *
 * O que estes testes protegem é a **fronteira do que conta como alteração**.
 * Errar para o lado generoso enche o histórico de linhas dizendo que nada
 * mudou; errar para o lado apertado perde justamente a alteração que alguém
 * foi procurar. Os dois lados falham em silêncio: nenhum dá erro, os dois dão
 * uma tela que ninguém consegue mais ler.
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

    // ─── O retrato: o que conta como alteração ───────────────────────────────

    /**
     * Um registro já preenchido com **os mesmos valores que o `dto` manda**.
     *
     * É a peça que faz o resto valer alguma coisa. Com um registro vazio,
     * qualquer update mudaria os oito campos de uma vez, e todo teste veria oito
     * linhas de histórico — inclusive os que existem para provar que **nenhuma**
     * foi gravada. Igualando tudo menos o campo sob teste, o que sobra na
     * captura é exatamente o que mudou.
     */
    private MachineRegister registroCompleto(LocalDateTime previsao) {
        MachineRegister register = new MachineRegister(new ProductInventory());
        register.setNomeCliente("Cliente");
        register.setTag("T-1");
        register.setSolicitante("Solicitante");
        register.setStatus(MachineStatus.DISPONIVEL);
        register.setObservacao("Observação");
        register.setPrevisaoEntrega(previsao);
        register.setTecnico("Técnico");
        register.setRegiao("Região");
        register.setConsultor("Consultor");
        return register;
    }

    private List<MachineScheduleChange> linhasGravadas(int quantas) {
        ArgumentCaptor<MachineScheduleChange> captor =
                ArgumentCaptor.forClass(MachineScheduleChange.class);
        verify(scheduleChangeRepository, times(quantas)).save(captor.capture());
        return captor.getAllValues();
    }

    /**
     * **O teste que impede o histórico de virar lixo.**
     *
     * Sem ele, o jeito mais fácil de escrever o serviço — gravar uma linha por
     * campo do retrato — passaria em todos os outros testes desta classe. Cada
     * PUT deixaria oito linhas, e em uma semana o histórico de uma máquina teria
     * centenas de entradas dizendo que nada mudou.
     */
    @Test
    @DisplayName("Update que não muda nada não grava linha nenhuma")
    void campoQueNaoMudouNaoGeraLinha() {
        registroExistenteQueSalva(registroCompleto(ONTEM));

        service.update(dto(ONTEM, null), REGISTER_ID);

        verifyNoInteractions(scheduleChangeRepository);
    }

    /**
     * Vazio e nulo são a mesma ausência.
     *
     * A tela manda `""` onde o banco tem `null` — é o que um input de texto
     * limpo produz. Sem normalizar, cada edição gravaria uma alteração de nada
     * para nada em todo campo em branco, e o histórico afundaria junto.
     */
    @Test
    @DisplayName("Campo nulo no banco e string em branco no DTO não é alteração")
    void brancoENuloSaoAMesmaCoisa() {
        MachineRegister register = registroCompleto(ONTEM);
        register.setConsultor(null);
        registroExistenteQueSalva(register);

        service.update(new UpdateRegisterDTO("Cliente", "T-1", "Solicitante",
                MachineStatus.DISPONIVEL, "Observação", ONTEM,
                "Técnico", "Região", "   ", null, false), REGISTER_ID);

        verifyNoInteractions(scheduleChangeRepository);
    }

    /**
     * Preencher um campo vazio **conta**, e isto é a regra invertida.
     *
     * Antes, completar um cadastro não gerava histórico — a razão era não cobrar
     * justificativa de quem só informa algo. Com o motivo opcional essa razão
     * deixou de existir, e "consultor: — → Marcos" é justamente o que se procura
     * ao abrir o histórico.
     */
    @Test
    @DisplayName("Preencher um campo vazio gera linha, com valor anterior nulo")
    void preencherCampoVazioContaComoAlteracao() {
        MachineRegister register = registroCompleto(ONTEM);
        register.setConsultor(null);
        registroExistenteQueSalva(register);

        service.update(dto(ONTEM, null), REGISTER_ID);

        MachineScheduleChange linha = linhasGravadas(1).get(0);
        assertThat(linha.getCampo()).isEqualTo("consultor");
        assertThat(linha.getValorAnterior()).isNull();
        assertThat(linha.getValorNovo()).isEqualTo("Consultor");
    }

    // ─── O motivo, agora opcional ────────────────────────────────────────────

    /**
     * A regra que **saiu**. Este teste afirmava o contrário até esta mudança.
     *
     * Obrigar justificativa ensinava a digitar "ok" para passar da tela. O campo
     * continua sendo perguntado quando a previsão muda, e agora aceita vazio.
     */
    @Test
    @DisplayName("Mudar a previsão sem motivo passa, e grava com motivo nulo")
    void mudarPrevisaoSemMotivoAgoraGrava() {
        registroExistenteQueSalva(registroCompleto(ONTEM));

        assertThatCode(() -> service.update(dto(SEMANA_QUE_VEM, null), REGISTER_ID))
                .doesNotThrowAnyException();

        assertThat(linhasGravadas(1).get(0).getMotivo()).isNull();
    }

    /** Motivo em branco não vira `"   "` no banco: vira ausência. */
    @Test
    @DisplayName("Motivo só com espaços é gravado como nulo")
    void motivoEmBrancoViraNulo() {
        registroExistenteQueSalva(registroCompleto(ONTEM));

        service.update(dto(SEMANA_QUE_VEM, "   "), REGISTER_ID);

        assertThat(linhasGravadas(1).get(0).getMotivo()).isNull();
    }

    // ─── O que fica gravado ──────────────────────────────────────────────────

    @Test
    @DisplayName("Adiamento grava o campo, o antes, o depois e o motivo")
    void adiamentoGravaCampoEValores() {
        registroExistenteQueSalva(registroCompleto(ONTEM));

        service.update(dto(SEMANA_QUE_VEM, "  Peça atrasada no fornecedor  "), REGISTER_ID);

        MachineScheduleChange linha = linhasGravadas(1).get(0);
        assertThat(linha.getCampo()).isEqualTo("previsao");
        assertThat(linha.getValorAnterior()).isEqualTo(ONTEM.toString());
        assertThat(linha.getValorNovo()).isEqualTo(SEMANA_QUE_VEM.toString());
        // `trim` no motivo: espaço sobrando vira ruído em relatório.
        assertThat(linha.getMotivo()).isEqualTo("Peça atrasada no fornecedor");
    }

    /**
     * Apagar a previsão também é alteração — e é a mais grave, porque a máquina
     * some das "Próximas saídas" sem ninguém perceber.
     */
    @Test
    @DisplayName("Apagar a previsão grava com valor novo nulo")
    void apagarPrevisaoTambemEhAlteracao() {
        registroExistenteQueSalva(registroCompleto(ONTEM));

        service.update(dto(null, "Cliente cancelou"), REGISTER_ID);

        MachineScheduleChange linha = linhasGravadas(1).get(0);
        assertThat(linha.getCampo()).isEqualTo("previsao");
        assertThat(linha.getValorAnterior()).isEqualTo(ONTEM.toString());
        assertThat(linha.getValorNovo()).isNull();
    }

    /**
     * O status vai pela **chave** do enum, nunca pelo rótulo.
     *
     * Rótulo é texto de tela e muda quando alguém corrige uma tradução. Um
     * histórico cujo conteúdo muda sozinho depois de gravado não é histórico.
     */
    @Test
    @DisplayName("Status é gravado pela chave do enum")
    void statusVaiPelaChaveDoEnum() {
        registroExistenteQueSalva(registroCompleto(ONTEM));

        service.update(new UpdateRegisterDTO("Cliente", "T-1", "Solicitante",
                MachineStatus.AGUARDANDO_AQUISICAO, "Observação", ONTEM,
                "Técnico", "Região", "Consultor", null, false), REGISTER_ID);

        MachineScheduleChange linha = linhasGravadas(1).get(0);
        assertThat(linha.getCampo()).isEqualTo("status");
        assertThat(linha.getValorAnterior()).isEqualTo("DISPONIVEL");
        assertThat(linha.getValorNovo()).isEqualTo("AGUARDANDO_AQUISICAO");
    }

    /**
     * **O teste que justifica o mapa.**
     *
     * Uma edição mexe em quantos campos a pessoa quiser, e cada um vira sua
     * própria linha. Um serviço que só olhasse a previsão — que é o que existia
     * antes — passaria em quase todos os outros testes daqui e falharia neste.
     *
     * O motivo é um só e vale para a edição inteira: separar um motivo por campo
     * exigiria um diálogo por campo, e ninguém preencheria oito.
     */
    @Test
    @DisplayName("Duas mudanças na mesma edição geram duas linhas, com o mesmo motivo")
    void duasMudancasGeramDuasLinhas() {
        registroExistenteQueSalva(registroCompleto(ONTEM));

        service.update(new UpdateRegisterDTO("Cliente", "T-1", "Solicitante",
                MachineStatus.DISPONIVEL, "Observação", SEMANA_QUE_VEM,
                "Outro técnico", "Região", "Consultor", "Reprogramado", false), REGISTER_ID);

        List<MachineScheduleChange> linhas = linhasGravadas(2);

        assertThat(linhas).extracting(MachineScheduleChange::getCampo)
                .containsExactlyInAnyOrder("previsao", "tecnico");
        assertThat(linhas).extracting(MachineScheduleChange::getMotivo)
                .containsOnly("Reprogramado");
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
        registroExistenteQueSalva(registroCompleto(ONTEM));

        service.update(dto(SEMANA_QUE_VEM, "Peça atrasada"), REGISTER_ID);
        service.update(dto(SEMANA_QUE_VEM.plusDays(7), "Técnico de férias"), REGISTER_ID);

        List<MachineScheduleChange> linhas = linhasGravadas(2);

        assertThat(linhas).extracting(MachineScheduleChange::getMotivo)
                .containsExactly("Peça atrasada", "Técnico de férias");

        // A segunda alteração parte de onde a primeira chegou.
        assertThat(linhas.get(1).getValorAnterior()).isEqualTo(SEMANA_QUE_VEM.toString());
    }

    // ─── O contrato do Hub ───────────────────────────────────────────────────

    private MachineScheduleChange alteracao(String campo, String anterior, String novo) {
        ProductInventory machine = new ProductInventory();
        machine.setName("Máquina");

        MachineRegister register = new MachineRegister(machine);
        register.setNomeCliente("Cliente");

        return new MachineScheduleChange(register, campo, anterior, novo, "motivo");
    }

    /**
     * **O teste que protege o Hub.**
     *
     * O Hub conta adiamentos e calcula atraso mediano em cima de datas. Depois
     * desta mudança a tabela guarda texto e guarda todo campo, então duas coisas
     * podem quebrá-lo em silêncio: contar troca de técnico como adiamento, e
     * devolver a data como texto. Nenhuma das duas dá erro — dão número errado,
     * e ninguém confere um número que sempre esteve ali.
     */
    @Test
    @DisplayName("slipsSince traz só previsão, e a data volta a ser data")
    void slipsSinceIgnoraOsOutrosCampos() {
        when(scheduleChangeRepository.findSince(any())).thenReturn(List.of(
                alteracao("previsao", ONTEM.toString(), SEMANA_QUE_VEM.toString()),
                alteracao("tecnico", "Marcos", "Joana")));

        var slips = service.slipsSince(ONTEM);

        assertThat(slips).hasSize(1);
        assertThat(slips.get(0).previsaoAnterior()).isEqualTo(ONTEM);
        assertThat(slips.get(0).previsaoNova()).isEqualTo(SEMANA_QUE_VEM);
    }

    /**
     * Preencher a data pela primeira vez entra no histórico da linha, mas não na
     * conta do Hub: não havia de onde adiar. Sem este filtro `previsaoAnterior`
     * chegaria nula na tela, e o cálculo do atraso mediano viria envenenado.
     */
    @Test
    @DisplayName("Primeiro preenchimento da previsão não conta como adiamento")
    void slipsSinceIgnoraOPrimeiroPreenchimento() {
        when(scheduleChangeRepository.findSince(any())).thenReturn(List.of(
                alteracao("previsao", null, SEMANA_QUE_VEM.toString())));

        assertThat(service.slipsSince(ONTEM)).isEmpty();
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
