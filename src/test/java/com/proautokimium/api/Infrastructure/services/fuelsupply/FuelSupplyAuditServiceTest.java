package com.proautokimium.api.Infrastructure.services.fuelsupply;

import com.proautokimium.api.Application.DTOs.fuelsupply.FuelSupplyImportResultDTO;
import com.proautokimium.api.Application.DTOs.fuelsupply.FuelSupplyImportRowDTO;
import com.proautokimium.api.Application.DTOs.fuelsupply.FuelSupplyPreviewRowDTO;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.FuelSupplyRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.DepartmentRepository;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.FuelSupply;
import com.proautokimium.api.domain.entities.humanResources.Department;
import com.proautokimium.api.domain.entities.humanResources.Team;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A conferência da planilha de abastecimentos.
 *
 * <p><b>Os dois defeitos que estavam em produção</b>, e que esta classe existe
 * para não deixar voltar:
 *
 * <ol>
 *   <li>motorista que não casava com nenhum funcionário caía calado em
 *       {@code SEM_DEPARTAMENTO} — e o relatório de combustível agrupa por
 *       departamento, então o número saía errado sem ninguém ver;</li>
 *   <li>reenviar a mesma planilha gravava o mês inteiro de novo, porque nada
 *       olhava o que já estava no banco.</li>
 * </ol>
 *
 * <p>A planilha dos testes é montada pelo próprio {@code FuelSupplyWriterService}:
 * é o mesmo arquivo que a pessoa baixa e preenche, então o teste passa pelo
 * escritor e pelo leitor de verdade — e não por um mock que concorda com tudo.
 */
@ExtendWith(MockitoExtension.class)
class FuelSupplyAuditServiceTest {

    @Mock
    private FuelSupplyRepository repository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private DepartmentRepository departmentRepository;

    private final FuelSupplyWriterService writer = new FuelSupplyWriterService();

    private FuelSupplyAuditService service;

    private static final LocalDate DATA = LocalDate.of(2026, 8, 14);

    @BeforeEach
    void setUp() {
        service = new FuelSupplyAuditService(
                new FuelSupplyReaderService(), repository, employeeRepository, departmentRepository);
    }

    // ---------------------------------------------------------------- fixtures

    private static FuelSupply abastecimento(String motorista, LocalDate data, double valor) {
        FuelSupply fs = new FuelSupply();
        fs.setDriverName(motorista);
        fs.setFuelSupplyDate(data);
        fs.setUf("SP");
        fs.setPlate("ABC1D23");
        fs.setActualHodometer(123456);
        fs.setFuelType("DIESEL S10");
        fs.setLiters(87.5);
        fs.setTotalValue(valor);
        fs.setPrice(5.99);
        fs.setDiferenceHodometer(642);
        fs.setAverageKm(7.34);
        return fs;
    }

    private static Department departamento(String nome) {
        Department d = new Department(nome);
        d.id = UUID.randomUUID();
        return d;
    }

    private static Employee funcionario(String nome, Department departamento) {
        Employee e = new Employee();
        e.setName(nome);
        e.setTeam(new Team("Frota", departamento));
        return e;
    }

    private byte[] planilhaCom(FuelSupply... linhas) throws Exception {
        return writer.write(List.of(linhas));
    }

    private List<FuelSupplyPreviewRowDTO> conferir(byte[] planilha) throws Exception {
        return service.preview(new ByteArrayInputStream(planilha));
    }

    // ----------------------------------------------------------------- preview

    /**
     * <b>O defeito número um.</b> Antes, o departamento vinha preenchido com
     * {@code SEM_DEPARTAMENTO} e ninguém ficava sabendo. Em branco, a tela
     * obriga alguém a escolher.
     */
    @Test
    @DisplayName("Motorista que nao casa com funcionario vem SEM departamento, e marcado")
    void motoristaDesconhecidoVemEmBranco() throws Exception {
        when(employeeRepository.findAll()).thenReturn(List.of());
        when(repository.findByFuelSupplyDateBetween(any(), any())).thenReturn(List.of());

        List<FuelSupplyPreviewRowDTO> linhas =
                conferir(planilhaCom(abastecimento("JOAO DA SILVA", DATA, 524.13)));

        assertThat(linhas).hasSize(1);
        assertThat(linhas.getFirst().motoristaEncontrado()).isFalse();
        assertThat(linhas.getFirst().departmentId())
                .as("em branco e' o conserto: cair em SEM_DEPARTAMENTO desanda o relatorio calado")
                .isNull();
        assertThat(linhas.getFirst().departmentName()).isNull();
    }

    @Test
    @DisplayName("Motorista que casa traz o departamento do setor dele, ja sugerido")
    void motoristaConhecidoTrazODepartamento() throws Exception {
        Department logistica = departamento("LOGISTICA");
        when(employeeRepository.findAll())
                .thenReturn(List.of(funcionario("Joao da Silva", logistica)));
        when(repository.findByFuelSupplyDateBetween(any(), any())).thenReturn(List.of());

        List<FuelSupplyPreviewRowDTO> linhas =
                conferir(planilhaCom(abastecimento("JOAO DA SILVA", DATA, 524.13)));

        assertThat(linhas.getFirst().motoristaEncontrado())
                .as("o casamento ignora caixa e espaco: a planilha vem de fora")
                .isTrue();
        assertThat(linhas.getFirst().departmentId()).isEqualTo(logistica.getId());
        assertThat(linhas.getFirst().departmentName()).isEqualTo("LOGISTICA");
    }

    /**
     * <b>O cartão de combustível e o cadastro escrevem o mesmo nome diferente.</b>
     *
     * <p>Medido em 2026-09-24 com a planilha de agosto: dos 34 motoristas, 17
     * casavam comparando só em minúsculas. O acento sozinho respondia por 6
     * deles — "Márcio Gabe Silveira" no cartão, "Marcio Gabe Silveira" no
     * cadastro. É a mesma pessoa, e o relatório saía com ela fora do
     * departamento.
     */
    @Test
    @DisplayName("Acento a mais ou a menos nao separa a pessoa do cadastro dela")
    void acentoNaoSeparaAPessoa() throws Exception {
        Department comercial = departamento("COMERCIAL");
        when(employeeRepository.findAll())
                .thenReturn(List.of(funcionario("Marcio Gabe Silveira", comercial)));
        when(repository.findByFuelSupplyDateBetween(any(), any())).thenReturn(List.of());

        List<FuelSupplyPreviewRowDTO> linhas =
                conferir(planilhaCom(abastecimento("Márcio Gabe Silveira", DATA, 524.13)));

        assertThat(linhas.getFirst().motoristaEncontrado()).isTrue();
        assertThat(linhas.getFirst().departmentName()).isEqualTo("COMERCIAL");
    }

    /**
     * As partículas aparecem e somem entre um cadastro e outro sem mudar de
     * quem se está falando: "Regimilso Oliveira Pereira" no cartão e
     * "Regimilso de Oliveira Pereira" no cadastro são a mesma pessoa.
     */
    @Test
    @DisplayName("O 'de' que um cadastro tem e o outro nao, tambem nao separa")
    void particulaNaoSeparaAPessoa() throws Exception {
        Department comercial = departamento("COMERCIAL");
        when(employeeRepository.findAll())
                .thenReturn(List.of(funcionario("Regimilso de Oliveira Pereira", comercial)));
        when(repository.findByFuelSupplyDateBetween(any(), any())).thenReturn(List.of());

        List<FuelSupplyPreviewRowDTO> linhas =
                conferir(planilhaCom(abastecimento("Regimilso Oliveira Pereira", DATA, 524.13)));

        assertThat(linhas.getFirst().motoristaEncontrado()).isTrue();
    }

    /**
     * <b>Onde a normalização para, de propósito.</b> "Fabio Lola" não é
     * "Fabio Lola da Silva": pode ser, e pode não ser. Aqui um palpite errado
     * grava abastecimento no departamento de outra pessoa, e o relatório do
     * mês sai torto sem ninguém ver. Quem decide é a conferência.
     */
    @Test
    @DisplayName("Nome incompleto NAO casa: sobrenome que falta e outra pessoa")
    void nomeIncompletoNaoCasa() throws Exception {
        when(employeeRepository.findAll())
                .thenReturn(List.of(funcionario("Fabio Lola da Silva", departamento("MANUTENCAO"))));
        when(repository.findByFuelSupplyDateBetween(any(), any())).thenReturn(List.of());

        List<FuelSupplyPreviewRowDTO> linhas =
                conferir(planilhaCom(abastecimento("Fabio Lola", DATA, 524.13)));

        assertThat(linhas.getFirst().motoristaEncontrado())
                .as("adivinhar sobrenome grava no departamento errado, calado")
                .isFalse();
        assertThat(linhas.getFirst().departmentId()).isNull();
    }

    /**
     * <b>O defeito número dois.</b> A duplicata é <i>marcada</i>, não removida:
     * dois abastecimentos do mesmo motorista no mesmo dia pelo mesmo valor
     * acontecem, e quem decide é quem confere.
     */
    @Test
    @DisplayName("Linha que ja esta no banco vem marcada, e continua na lista")
    void duplicataEMarcadaENaoSumida() throws Exception {
        when(employeeRepository.findAll()).thenReturn(List.of());
        when(repository.findByFuelSupplyDateBetween(any(), any()))
                .thenReturn(List.of(abastecimento("Joao da Silva", DATA, 524.13)));

        List<FuelSupplyPreviewRowDTO> linhas =
                conferir(planilhaCom(abastecimento("JOAO DA SILVA", DATA, 524.13)));

        assertThat(linhas)
                .as("sumir com a linha tira da pessoa a decisao que a tela existe para dar")
                .hasSize(1);
        assertThat(linhas.getFirst().jaExiste()).isTrue();
    }

    /**
     * Um centavo de diferença é outro abastecimento. O teste existe porque a
     * comparação passa por {@code Math.round(valor * 100)}: arredondar para
     * reais faria 524,13 e 524,14 virarem a mesma linha.
     */
    @Test
    @DisplayName("Um centavo de diferenca ja nao e duplicata")
    void centavoDeDiferencaNaoEDuplicata() throws Exception {
        when(employeeRepository.findAll()).thenReturn(List.of());
        when(repository.findByFuelSupplyDateBetween(any(), any()))
                .thenReturn(List.of(abastecimento("JOAO DA SILVA", DATA, 524.13)));

        List<FuelSupplyPreviewRowDTO> linhas =
                conferir(planilhaCom(abastecimento("JOAO DA SILVA", DATA, 524.14)));

        assertThat(linhas.getFirst().jaExiste()).isFalse();
    }

    /**
     * O passo inteiro existe para isto: <b>conferir não grava</b>. Era o que o
     * {@code /upload} fazia na mesma requisição da leitura.
     */
    @Test
    @DisplayName("Conferir nao grava nada")
    void conferirNaoGrava() throws Exception {
        when(employeeRepository.findAll()).thenReturn(List.of());
        when(repository.findByFuelSupplyDateBetween(any(), any())).thenReturn(List.of());

        conferir(planilhaCom(abastecimento("JOAO DA SILVA", DATA, 524.13)));

        verify(repository, never()).save(any());
        verify(repository, never()).saveAll(anyList());
    }

    /**
     * O número da linha é o que a pessoa vai procurar no Excel dela. Linha em
     * branco no meio da planilha — o Excel cria uma ao formatar ou ao apagar
     * conteúdo — não pode deslocar a contagem: a partir dela, índice de lista e
     * linha de planilha divergem, e a mensagem "confira a linha 47" passa a
     * apontar para a linha errada.
     */
    @Test
    @DisplayName("Linha em branco no meio nao entra, e nao desloca a numeracao")
    void linhaEmBrancoNoMeio() throws Exception {
        when(employeeRepository.findAll()).thenReturn(List.of());
        when(repository.findByFuelSupplyDateBetween(any(), any())).thenReturn(List.of());

        byte[] comBuraco;
        byte[] cheia = planilhaCom(
                abastecimento("JOAO DA SILVA", DATA, 524.13),
                abastecimento("MARIA SOUZA", DATA, 310.00));

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(cheia));
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            // Empurra a segunda linha para baixo, deixando a linha 3 da planilha
            // vazia — do jeito que uma planilha de verdade chega.
            Sheet sheet = wb.getSheetAt(0);
            sheet.shiftRows(2, 2, 1);

            wb.write(bos);
            comBuraco = bos.toByteArray();
        }

        List<FuelSupplyPreviewRowDTO> linhas = conferir(comBuraco);

        assertThat(linhas).hasSize(2);
        assertThat(linhas.get(0).linha()).isEqualTo(2);
        assertThat(linhas.get(1).linha())
                .as("a segunda esta na linha 4 da planilha, e nao na 3")
                .isEqualTo(4);
    }

    // ---------------------------------------------------------------- importar

    @Test
    @DisplayName("Grava com o departamento que a conferencia escolheu")
    void gravaComODepartamentoEscolhido() {
        Department escolhido = departamento("MANUTENCAO");
        when(departmentRepository.findAllById(anyList())).thenReturn(List.of(escolhido));

        FuelSupplyImportResultDTO resultado =
                service.importar(List.of(linhaImportavel(2, escolhido.getId())));

        assertThat(resultado.gravadas()).isEqualTo(1);
        assertThat(resultado.recusadas()).isZero();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FuelSupply>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());

        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().getFirst().getDepartment()).isEqualTo(escolhido);
        assertThat(captor.getValue().getFirst().getDriverName()).isEqualTo("JOAO DA SILVA");
    }

    /**
     * Tudo ou nada. Gravar as boas e recusar as ruins deixaria a pessoa sem
     * saber quais já entraram — e reenviar a planilha corrigida duplicaria
     * todas as outras.
     */
    @Test
    @DisplayName("Uma linha recusada cancela a remessa inteira")
    void umaLinhaRecusadaCancelaTudo() {
        Department escolhido = departamento("MANUTENCAO");
        when(departmentRepository.findAllById(anyList())).thenReturn(List.of(escolhido));

        FuelSupplyImportResultDTO resultado = service.importar(List.of(
                linhaImportavel(2, escolhido.getId()),
                linhaImportavel(3, null)));

        assertThat(resultado.gravadas()).isZero();
        assertThat(resultado.recusadas()).isEqualTo(1);
        assertThat(resultado.motivos().getFirst())
                .as("a mensagem tem que dizer QUAL linha, senao nao serve para consertar nada")
                .startsWith("Linha 3:");

        verify(repository, never()).saveAll(anyList());
    }

    /**
     * As colunas checadas aqui são NOT NULL no banco. Sem a checagem, a remessa
     * morre num erro de constraint que não diz qual linha era.
     */
    @Test
    @DisplayName("Linha sem data e recusada com a linha no motivo")
    void linhaSemDataERecusada() {
        Department escolhido = departamento("MANUTENCAO");
        when(departmentRepository.findAllById(anyList())).thenReturn(List.of(escolhido));

        FuelSupplyImportResultDTO resultado = service.importar(List.of(
                new FuelSupplyImportRowDTO(7, null, "SP", "ABC1D23", "JOAO DA SILVA",
                        escolhido.getId(), 123456, 642, 7.34, "DIESEL S10", 87.5, 5.99, 524.13)));

        assertThat(resultado.gravadas()).isZero();
        assertThat(resultado.motivos()).containsExactly("Linha 7: a data do abastecimento está em branco.");
        verify(repository, never()).saveAll(anyList());
    }

    /**
     * O departamento pode ter sido excluído entre a conferência e a gravação —
     * as duas são requisições diferentes, com a pessoa olhando a tela no meio.
     */
    @Test
    @DisplayName("Departamento que sumiu entre conferir e gravar e recusado, nao estoura")
    void departamentoQueSumiu() {
        when(departmentRepository.findAllById(anyList())).thenReturn(List.of());

        FuelSupplyImportResultDTO resultado =
                service.importar(List.of(linhaImportavel(5, UUID.randomUUID())));

        assertThat(resultado.recusadas()).isEqualTo(1);
        assertThat(resultado.motivos().getFirst()).contains("não existe mais");
        verify(repository, never()).saveAll(anyList());
    }

    private static FuelSupplyImportRowDTO linhaImportavel(int linha, UUID departmentId) {
        return new FuelSupplyImportRowDTO(
                linha, DATA, "SP", "ABC1D23", "JOAO DA SILVA", departmentId,
                123456, 642, 7.34, "DIESEL S10", 87.5, 5.99, 524.13);
    }
}
