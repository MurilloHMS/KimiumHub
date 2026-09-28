package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Infrastructure.exceptions.humanResources.EmptyReimbursementReportException;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.ReimbursementRepository;
import com.proautokimium.api.Infrastructure.services.storage.ReimbursementStorageService;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.Reimbursement;
import com.proautokimium.api.domain.enums.Department;
import com.proautokimium.api.domain.enums.humanResources.ReimbursementStatus;
import com.proautokimium.api.domain.exceptions.humanResources.InvalidRequestDataException;
import com.proautokimium.api.domain.exceptions.partners.EmployeeNotFoundException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.proautokimium.api.domain.enums.humanResources.ReimbursementStatus.APPROVED;
import static com.proautokimium.api.domain.enums.humanResources.ReimbursementStatus.PAID;
import static com.proautokimium.api.domain.enums.humanResources.ReimbursementStatus.PENDING;
import static com.proautokimium.api.domain.enums.humanResources.ReimbursementStatus.REJECTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * **O comprovante para a diretoria, gerado de verdade.**
 *
 * Jasper e PDFBox rodam de fato; o texto é lido de volta do PDF. Um mock do
 * Jasper nunca pegaria o que mais quebra aqui: template que não compila,
 * campo com nome errado (sai em branco, sem erro), fonte que não embute.
 *
 * Os valores são os do desenho aprovado: três funcionários, R$ 1.694,90.
 */
@ExtendWith(MockitoExtension.class)
class ReimbursementReportServiceTest {

    private static final LocalDate FROM = LocalDate.of(2026, 9, 1);
    private static final LocalDate TO = LocalDate.of(2026, 9, 30);
    private static final List<ReimbursementStatus> SEM_RECUSADO = List.of(PENDING, APPROVED, PAID);

    @Mock ReimbursementRepository repository;
    @Mock EmployeeRepository employeeRepository;
    @Mock UserRepository userRepository;
    @Mock ReimbursementStorageService storage;

    @TempDir Path disco;

    private ReimbursementReportService service;
    private Employee carla;
    private Employee ana;
    private Employee diego;
    private Employee juliana;

    @BeforeEach
    void setUp() throws Exception {
        Clock clock = Clock.fixed(LocalDateTime.of(2026, 9, 28, 14, 32)
                .atZone(ZoneId.of("America/Sao_Paulo")).toInstant(), ZoneId.of("America/Sao_Paulo"));
        service = new ReimbursementReportService(repository, employeeRepository, userRepository,
                new ReimbursementReceiptAnnexes(storage), clock);

        carla = funcionario("Carla Mendes", "1001", Department.ADMINISTRATIVO);
        ana = funcionario("Ana Beatriz Rocha", "1042", Department.AUTOMOTIVO);
        diego = funcionario("Diego Fernandes Lima", "1107", Department.EQUIPAMENTOS);
        juliana = funcionario("Juliana Prates Moura", "1063", Department.ADMINISTRATIVO);

        // quem emite é a Carla, do RH
        lenient().when(userRepository.findByLoginWithEmployee("carla.rh")).thenReturn(Optional.empty());
        lenient().when(employeeRepository.findByUsername("carla.rh")).thenReturn(Optional.of(carla));
        lenient().when(storage.resolve(anyString())).thenAnswer(inv -> disco.resolve(inv.<String>getArgument(0)));
    }

    private static Employee funcionario(String nome, String codigo, Department setor) throws Exception {
        Employee e = new Employee();
        e.setName(nome);
        e.setCodParceiro(codigo);
        e.setDepartment(setor);
        Field id = com.proautokimium.api.domain.abstractions.Entity.class.getDeclaredField("id");
        id.setAccessible(true);
        id.set(e, UUID.randomUUID());
        return e;
    }

    private Reimbursement pedido(Employee quem, int dia, String valor, String categoria, String arquivo,
                                 ReimbursementStatus status) {
        Reimbursement r = Reimbursement.request(quem, LocalDate.of(2026, 9, dia), new BigDecimal(valor),
                categoria, "Visita técnica", arquivo, quem.getCodParceiro() + "/" + arquivo,
                LocalDateTime.of(2026, 9, dia, 9, 0));
        if (status != PENDING) {
            r.approve(carla, "Conferido com a rota.", LocalDateTime.of(2026, 9, dia + 1, 10, 0));
        }
        if (status == PAID) {
            r.pay(LocalDate.of(2026, 9, dia + 5), LocalDateTime.of(2026, 9, dia + 5, 10, 0));
        }
        return r;
    }

    /**
     * O texto do PDF com as quebras de linha viradas espaço: célula estreita
     * quebra "Pendente, Aprovado, Pago" em duas linhas, e isso é o desenho.
     */
    private static String texto(byte[] pdf) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(doc).replaceAll("\\s+", " ");
        }
    }

    private static int paginas(byte[] pdf) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            return doc.getNumberOfPages();
        }
    }

    // ─── Todos os funcionários ───────────────────────────────────────────────

    @Test
    @DisplayName("todos os funcionários: cabeçalho diz o filtro, subtotais e total batem com o desenho")
    void todosOsFuncionarios() throws Exception {
        when(repository.findForReport(FROM, TO, SEM_RECUSADO)).thenReturn(List.of(
                pedido(ana, 3, "180.00", "Combustível", "a1.jpg", PAID),
                pedido(ana, 3, "320.00", "Hospedagem", "a2.jpg", PAID),
                pedido(ana, 17, "96.50", "Alimentação", "a3.jpg", PENDING),
                pedido(diego, 9, "212.40", "Combustível", "d1.jpg", PAID),
                pedido(diego, 22, "92.00", "Pedágio", "d2.jpg", APPROVED),
                pedido(juliana, 11, "400.00", "Hospedagem", "j1.jpg", PAID),
                pedido(juliana, 25, "394.00", "Alimentação", "j2.jpg", APPROVED)));

        byte[] pdf = service.generate(FROM, TO, List.of(PAID, PENDING, APPROVED), null, "carla.rh");
        String t = texto(pdf);

        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        assertThat(t).contains("01/09/2026 a 30/09/2026", "Todos os funcionários",
                "3 funcionários com solicitações", "Pendente, Aprovado, Pago", "Não incluído: Recusado",
                "28/09/2026 14:32", "por Carla Mendes");
        assertThat(t).as("subtotais por funcionário, somados pelo Jasper")
                .contains("R$ 596,50", "R$ 304,40", "R$ 794,00");
        assertThat(t).as("total geral e rótulo").contains("R$ 1.694,90",
                "Total geral do período · 7 solicitações · 3 funcionários");
        assertThat(t).as("resumo por status").contains("Aprovado, a pagar", "Pendente de análise");
        assertThat(t).as("trilha da análise").contains("Conferido com a rota.");
        assertThat(t).as("sem anexos no relatório de todos").doesNotContain("Anexo A-1")
                .contains("emita o comprovante de um funcionário por vez");
        assertThat(t).as("assinaturas").contains("Aprovado pela diretoria");
        // "Página 2 de 0" já saiu uma vez: PAGE_COUNT conta registros, não páginas.
        int total = paginas(pdf);
        assertThat(t).as("numeração com o total de páginas")
                .contains("Página 1 de " + total, "Página " + total + " de " + total);
    }

    @Test
    @DisplayName("com os quatro status escolhidos, o cabeçalho diz 'Todos os status'")
    void todosOsStatus() throws Exception {
        List<ReimbursementStatus> todos = List.of(PENDING, APPROVED, PAID, REJECTED);
        Reimbursement recusado = pedido(ana, 4, "50.00", "Outros", "x.jpg", PENDING);
        recusado.reject(carla, "Sem nota fiscal", LocalDateTime.of(2026, 9, 5, 9, 0));
        when(repository.findForReport(FROM, TO, todos)).thenReturn(List.of(recusado));

        String t = texto(service.generate(FROM, TO, todos, null, "carla.rh"));

        assertThat(t).contains("Todos os status", "Recusado", "Sem nota fiscal");
    }

    // ─── Um funcionário, com os comprovantes anexados ────────────────────────

    @Test
    @DisplayName("um funcionário: imagem, PDF e arquivo sumido viram anexos, e a numeração cobre tudo")
    void umFuncionarioComAnexos() throws Exception {
        Files.createDirectories(disco.resolve("1042"));
        BufferedImage foto = new BufferedImage(300, 480, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(foto, "png", disco.resolve("1042/nota.png").toFile());
        try (PDDocument recibo = new PDDocument()) {
            recibo.addPage(new PDPage());
            recibo.addPage(new PDPage());
            recibo.save(disco.resolve("1042/hotel.pdf").toFile());
        }
        // "sumiu.jpg" nunca é criado: o registro órfão

        when(employeeRepository.findById(ana.getId())).thenReturn(Optional.of(ana));
        when(repository.findForReportByEmployee(ana, FROM, TO, SEM_RECUSADO)).thenReturn(List.of(
                pedido(ana, 3, "180.00", "Combustível", "nota.png", PAID),
                pedido(ana, 3, "320.00", "Hospedagem", "hotel.pdf", PAID),
                pedido(ana, 17, "96.50", "Alimentação", "sumiu.jpg", PENDING)));

        byte[] pdf = service.generate(FROM, TO, SEM_RECUSADO, ana.getId(), "carla.rh");
        String t = texto(pdf);

        int paginas;
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            paginas = doc.getNumberOfPages();
        }
        assertThat(t).contains("FUNCIONÁRIO", "Ana Beatriz Rocha", "Cód. 1042 · Automotivo",
                "Total · 3 solicitações", "R$ 596,50");
        assertThat(t).as("um anexo por pedido, na ordem da tabela")
                .contains("Anexo A-1", "Anexo A-2", "Anexo A-3");
        assertThat(t).as("PDF anexado com as páginas originais")
                .contains("Comprovante em PDF com 2 páginas, anexadas a seguir.");
        assertThat(t).as("arquivo sumido não derruba o documento")
                .contains("O arquivo deste comprovante não foi encontrado no servidor.");
        // relatório + (imagem: 1) + (PDF: identificação 1 + originais 2) + (sumido: 1)
        assertThat(paginas).isGreaterThanOrEqualTo(6);
        assertThat(t).as("a última página sabe o total, contando os anexos")
                .contains("Página " + paginas + " de " + paginas);
    }

    // ─── Recusas ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("fim antes do início é recusado sem consultar nada")
    void fimAntesDoInicio() {
        InvalidRequestDataException e = assertThrows(InvalidRequestDataException.class,
                () -> service.generate(TO, FROM, SEM_RECUSADO, null, "carla.rh"));
        assertThat(e.getMessage()).isEqualTo("O fim do período não pode ser antes do início");
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("período acima de um ano é recusado")
    void periodoLongoDemais() {
        assertThrows(InvalidRequestDataException.class, () -> service.generate(
                LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 2), SEM_RECUSADO, null, "carla.rh"));
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("um ano exato é aceito")
    void umAnoExato() {
        LocalDate de = LocalDate.of(2025, 10, 1);
        LocalDate ate = LocalDate.of(2026, 9, 30);
        when(repository.findForReport(de, ate, SEM_RECUSADO)).thenReturn(List.of());

        // passa da validação e chega à consulta — que volta vazia
        assertThrows(EmptyReimbursementReportException.class,
                () -> service.generate(de, ate, SEM_RECUSADO, null, "carla.rh"));
    }

    @Test
    @DisplayName("sem status escolhido é recusado")
    void semStatus() {
        assertThrows(InvalidRequestDataException.class,
                () -> service.generate(FROM, TO, List.of(), null, "carla.rh"));
        assertThrows(InvalidRequestDataException.class,
                () -> service.generate(FROM, TO, null, null, "carla.rh"));
    }

    @Test
    @DisplayName("sem período é recusado")
    void semPeriodo() {
        assertThrows(InvalidRequestDataException.class,
                () -> service.generate(null, TO, SEM_RECUSADO, null, "carla.rh"));
    }

    @Test
    @DisplayName("funcionário inexistente é 404")
    void funcionarioInexistente() {
        UUID id = UUID.randomUUID();
        when(employeeRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(EmployeeNotFoundException.class,
                () -> service.generate(FROM, TO, SEM_RECUSADO, id, "carla.rh"));
    }

    /** Um PDF vazio iria à diretoria parecendo "não houve gasto". */
    @Test
    @DisplayName("filtro sem resultado recusa, em vez de gerar documento vazio")
    void semResultado() {
        when(repository.findForReport(any(), any(), any())).thenReturn(List.of());

        assertThrows(EmptyReimbursementReportException.class,
                () -> service.generate(FROM, TO, SEM_RECUSADO, null, "carla.rh"));
    }
}
