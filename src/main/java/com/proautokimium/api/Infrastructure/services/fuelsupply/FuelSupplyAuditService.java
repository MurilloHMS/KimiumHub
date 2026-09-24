package com.proautokimium.api.Infrastructure.services.fuelsupply;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.proautokimium.api.Application.DTOs.fuelsupply.FuelSupplyImportResultDTO;
import com.proautokimium.api.Application.DTOs.fuelsupply.FuelSupplyImportRowDTO;
import com.proautokimium.api.Application.DTOs.fuelsupply.FuelSupplyPreviewRowDTO;
import com.proautokimium.api.Infrastructure.abstractions.excel.ReadRow;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.FuelSupplyRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.DepartmentRepository;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.FuelSupply;
import com.proautokimium.api.domain.entities.humanResources.Department;

/**
 * A conferência da planilha de abastecimentos: ler, diagnosticar, e só gravar
 * depois que alguém olhou.
 *
 * <p><b>O que estava acontecendo.</b> O {@code /upload} lia, casava motorista,
 * atribuía departamento e salvava numa tacada só, respondendo sempre
 * "Importação concluída com sucesso!". Dois defeitos moravam nesse silêncio:
 * motorista que não casava caía calado em {@code SEM_DEPARTAMENTO} — e o
 * relatório agrupa por departamento —, e reenviar a mesma planilha gravava o
 * mês inteiro de novo.
 *
 * <p>Por isso são dois passos. O {@link #preview} lê e <b>não grava nada</b>;
 * o {@link #importar} grava o que voltou da tela, e nada além.
 */
@Service
public class FuelSupplyAuditService {

    /**
     * As partículas dos nomes em português.
     *
     * <p>Elas aparecem e somem entre um cadastro e outro sem mudar de quem se
     * está falando, então não entram na comparação.
     */
    private static final Set<String> PARTICULAS = Set.of("de", "da", "do", "dos", "das", "e");

    private final FuelSupplyReaderService reader;
    private final FuelSupplyRepository repository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    public FuelSupplyAuditService(FuelSupplyReaderService reader,
                                  FuelSupplyRepository repository,
                                  EmployeeRepository employeeRepository,
                                  DepartmentRepository departmentRepository) {
        this.reader = reader;
        this.repository = repository;
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
    }

    /**
     * Lê a planilha e devolve o diagnóstico de cada linha. <b>Não grava.</b>
     */
    public List<FuelSupplyPreviewRowDTO> preview(InputStream stream) throws Exception {

        List<ReadRow<FuelSupply>> lidas = reader.readRows(stream).stream()
                .filter(r -> temConteudo(r.valor()))
                .toList();

        Map<String, Employee> funcionariosPorNome = employeeRepository.findAll().stream()
                .filter(e -> e.getName() != null)
                .collect(Collectors.toMap(
                        e -> chaveDeNome(e.getName()),
                        e -> e,
                        (a, b) -> a
                ));

        Set<String> jaGravados = chavesJaGravadas(lidas);

        List<FuelSupplyPreviewRowDTO> linhas = new ArrayList<>(lidas.size());

        for (ReadRow<FuelSupply> lida : lidas) {
            FuelSupply fs = lida.valor();

            Employee funcionario = fs.getDriverName() == null
                    ? null
                    : funcionariosPorNome.get(chaveDeNome(fs.getDriverName()));

            Department departamento = departamentoDoFuncionario(funcionario);

            linhas.add(new FuelSupplyPreviewRowDTO(
                    lida.linha(),
                    fs.getDriverName(),
                    fs.getFuelSupplyDate(),
                    fs.getUf(),
                    fs.getPlate(),
                    fs.getActualHodometer(),
                    fs.getFuelType(),
                    fs.getLiters(),
                    fs.getTotalValue(),
                    fs.getPrice(),
                    fs.getDiferenceHodometer(),
                    fs.getAverageKm(),
                    departamento != null ? departamento.getId() : null,
                    departamento != null ? departamento.getName() : null,
                    funcionario != null,
                    jaGravados.contains(chaveDeDuplicata(
                            fs.getDriverName(), fs.getFuelSupplyDate(), fs.getTotalValue()))
            ));
        }

        return linhas;
    }

    /**
     * Grava as linhas que a conferência aprovou.
     *
     * <p>Valida todas antes de gravar qualquer uma: uma linha recusada cancela
     * a remessa inteira. É o que deixa o reenvio seguro — a pessoa corrige o
     * que faltava e manda de novo sem precisar descobrir quais já entraram.
     */
    @Transactional
    public FuelSupplyImportResultDTO importar(List<FuelSupplyImportRowDTO> linhas) {

        if (linhas == null || linhas.isEmpty()) {
            return new FuelSupplyImportResultDTO(0, 0, List.of());
        }

        Map<UUID, Department> departamentos = departamentoPorId(linhas);

        List<String> motivos = new ArrayList<>();
        List<FuelSupply> aGravar = new ArrayList<>(linhas.size());

        for (FuelSupplyImportRowDTO linha : linhas) {

            Optional<String> problema = problemaDa(linha, departamentos);

            if (problema.isPresent()) {
                motivos.add("Linha " + linha.linha() + ": " + problema.get());
                continue;
            }

            aGravar.add(entidadeDe(linha, departamentos.get(linha.departmentId())));
        }

        if (!motivos.isEmpty()) {
            return new FuelSupplyImportResultDTO(0, motivos.size(), motivos);
        }

        repository.saveAll(aGravar);

        return new FuelSupplyImportResultDTO(aGravar.size(), 0, List.of());
    }

    /**
     * O que impede esta linha de ser gravada, se é que algo impede.
     *
     * <p>As colunas conferidas aqui são <b>NOT NULL</b> no banco: sem esta
     * checagem a remessa morre num erro de constraint, que não diz qual linha
     * era nem o que faltava nela.
     */
    private Optional<String> problemaDa(FuelSupplyImportRowDTO linha, Map<UUID, Department> departamentos) {

        if (linha.fuelSupplyDate() == null) {
            return Optional.of("a data do abastecimento está em branco.");
        }
        if (vazio(linha.driverName())) {
            return Optional.of("o nome do motorista está em branco.");
        }
        if (vazio(linha.plate())) {
            return Optional.of("a placa está em branco.");
        }
        if (vazio(linha.uf())) {
            return Optional.of("a UF está em branco.");
        }
        if (linha.uf().trim().length() > 2) {
            return Optional.of("a UF tem que ter duas letras, e veio com " + linha.uf().trim().length() + ".");
        }
        if (vazio(linha.fuelType())) {
            return Optional.of("o tipo de combustível está em branco.");
        }
        if (linha.departmentId() == null) {
            return Optional.of("escolha o departamento.");
        }
        if (!departamentos.containsKey(linha.departmentId())) {
            return Optional.of("o departamento escolhido não existe mais no cadastro.");
        }

        return Optional.empty();
    }

    private FuelSupply entidadeDe(FuelSupplyImportRowDTO linha, Department departamento) {
        FuelSupply fs = new FuelSupply();

        fs.setFuelSupplyDate(linha.fuelSupplyDate());
        fs.setUf(linha.uf().trim().toUpperCase());
        fs.setPlate(linha.plate().trim());
        fs.setDriverName(linha.driverName().trim());
        fs.setDepartment(departamento);
        fs.setActualHodometer(linha.actualHodometer());
        fs.setDiferenceHodometer(linha.diferenceHodometer());
        fs.setAverageKm(linha.averageKm());
        fs.setFuelType(linha.fuelType().trim());
        fs.setLiters(linha.liters());
        fs.setPrice(linha.price());
        fs.setTotalValue(linha.totalValue());

        return fs;
    }

    private Map<UUID, Department> departamentoPorId(List<FuelSupplyImportRowDTO> linhas) {
        List<UUID> ids = linhas.stream()
                .map(FuelSupplyImportRowDTO::departmentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return departmentRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Department::getId, d -> d));
    }

    /**
     * O departamento sugerido, vindo do SETOR do funcionário.
     *
     * <p>Devolve <b>{@code null}</b> quando não dá para saber, e isso é o
     * conserto: antes caía em {@code SEM_DEPARTAMENTO} sem avisar ninguém, e o
     * relatório agrupava por um balde que não é departamento de ninguém. Em
     * branco, quem confere escolhe.
     */
    private Department departamentoDoFuncionario(Employee funcionario) {
        if (funcionario != null && funcionario.getTeam() != null) {
            return funcionario.getTeam().getDepartment();
        }
        return null;
    }

    /**
     * As chaves de duplicata que já estão no banco, dentro do período que a
     * planilha cobre.
     *
     * <p>Uma consulta só, e não uma por linha: a planilha do mês tem algumas
     * centenas de linhas e cobre trinta dias.
     */
    private Set<String> chavesJaGravadas(List<ReadRow<FuelSupply>> lidas) {

        List<LocalDate> datas = lidas.stream()
                .map(r -> r.valor().getFuelSupplyDate())
                .filter(Objects::nonNull)
                .sorted()
                .toList();

        if (datas.isEmpty()) {
            return Set.of();
        }

        Set<String> chaves = new HashSet<>();

        for (FuelSupply gravado : repository.findByFuelSupplyDateBetween(
                datas.getFirst(), datas.getLast())) {

            chaves.add(chaveDeDuplicata(
                    gravado.getDriverName(), gravado.getFuelSupplyDate(), gravado.getTotalValue()));
        }

        return chaves;
    }

    /**
     * Motorista, data e valor — foi o que ele pediu para comparar.
     *
     * <p>O valor entra em centavos inteiros: {@code double} não fecha
     * igualdade, e 524.13 lido da planilha não é necessariamente o mesmo
     * {@code double} que voltou do banco.
     *
     * <p>Duas linhas idênticas de verdade existem — dois abastecimentos do
     * mesmo motorista no mesmo dia pelo mesmo valor acontecem. Por isso isto
     * marca, e não bloqueia: quem decide é quem confere.
     */
    private static String chaveDeDuplicata(String motorista, LocalDate data, double valor) {
        return chaveDeNome(motorista) + "|" + data + "|" + Math.round(valor * 100);
    }

    /**
     * O nome reduzido ao que dá para comparar entre dois cadastros diferentes.
     *
     * <p>Minúsculas, <b>sem acento</b>, com um espaço só entre palavras e
     * <b>sem as partículas</b> de/da/do/dos/das/e.
     *
     * <p><b>Medido em 2026-09-24</b>, com a planilha de agosto (34 motoristas)
     * contra os 93 funcionários do cadastro: comparando só em minúsculas,
     * 17 casavam. Tirar acento e espaço duplo resolve 6 — o cartão escreve
     * "Márcio Gabe Silveira" e o cadastro tem "Marcio Gabe Silveira". Ignorar
     * as partículas resolve mais 1: "Regimilso Oliveira Pereira" contra
     * "Regimilso de Oliveira Pereira".
     *
     * <p>Os 10 que sobram têm nome <b>diferente</b>, não escrito diferente:
     * uns vêm sem o último sobrenome ("Fabio Lola" para "Fabio Lola da
     * Silva"), outros têm o sobrenome trocado ("Moacir Xisto" para "Moacir
     * Sixto"). Casar nome incompleto é adivinhação, e aqui ela grava
     * abastecimento no departamento errado — então esses ficam para a
     * conferência resolver.
     */
    static String chaveDeNome(String nome) {
        if (nome == null) {
            return "";
        }

        String semAcento = java.text.Normalizer.normalize(nome, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        StringBuilder chave = new StringBuilder();

        for (String palavra : semAcento.toLowerCase().trim().split("\\s+")) {
            if (palavra.isEmpty() || PARTICULAS.contains(palavra)) {
                continue;
            }
            if (!chave.isEmpty()) {
                chave.append(' ');
            }
            chave.append(palavra);
        }

        return chave.toString();
    }

    /**
     * Linha que existe na planilha e não tem nada dentro.
     *
     * <p>O Excel cria linha ao formatar, ao apagar conteúdo, ao arrastar a
     * seleção. Sem este filtro elas viram linhas de conferência em branco, e a
     * pessoa fica com quinze recusas de "o nome do motorista está em branco"
     * para linhas que ela nunca escreveu.
     */
    private static boolean temConteudo(FuelSupply fs) {
        return !vazio(fs.getDriverName()) || fs.getFuelSupplyDate() != null;
    }

    private static boolean vazio(String s) {
        return s == null || s.isBlank();
    }
}
