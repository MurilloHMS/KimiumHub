package com.proautokimium.api.Infrastructure.services.holerite;

import com.proautokimium.api.Application.DTOs.holerite.HoleritePreviewItemDTO;
import com.proautokimium.api.Application.DTOs.holerite.HoleriteResponseDTO;
import com.proautokimium.api.Application.DTOs.holerite.VincularHoleriteResultDTO;
import com.proautokimium.api.Application.DTOs.pdf.PdfPageInfoExtractorDTO;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.HoleriteDocumentoRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.services.notification.NotificationService;
import com.proautokimium.api.Infrastructure.services.pdf.holerith.HolerithExtractorService;
import com.proautokimium.api.Infrastructure.services.storage.HoleriteStorageService;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.HoleriteDocumento;
import com.proautokimium.api.domain.enums.HoleriteTipo;
import com.proautokimium.api.domain.enums.NotificationType;
import com.proautokimium.api.domain.enums.humanResources.HoleritePreviewStatus;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class HoleriteService {

    private final HolerithExtractorService extractor;
    private final HoleriteStorageService storage;
    private final EmployeeRepository employeeRepository;
    private final HoleriteDocumentoRepository repository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public HoleriteService(HolerithExtractorService extractor,
                           HoleriteStorageService storage,
                           EmployeeRepository employeeRepository,
                           HoleriteDocumentoRepository repository,
                           UserRepository userRepository,
                           NotificationService notificationService) {
        this.extractor = extractor;
        this.storage = storage;
        this.employeeRepository = employeeRepository;
        this.repository = repository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /**
     * Resolve o funcionário a partir do login do usuário autenticado.
     * Prioriza o vínculo explícito (users.employee_id) e só recorre à convenção
     * (username == login) como retrocompatibilidade.
     */
    private Employee resolveEmployee(String login) {
        Employee viaLink = userRepository.findByLoginWithEmployee(login)
                .map(u -> u.getEmployee())
                .orElse(null);
        if (viaLink != null) return viaLink;
        return employeeRepository.findByUsername(login).orElse(null);
    }

    /** Separa o PDF por página, casa cada holerite ao funcionário (por CPF) e armazena o vínculo. */
    // Sem @Transactional de propósito: não existe invariante entre funcionários,
    // e o arquivo vai para o disco ANTES do registro ir para o banco. Com a
    // transação no lote inteiro, um erro na página 137 desfazia 136 registros e
    // deixava os 136 PDFs órfãos no disco.
    public VincularHoleriteResultDTO vincular(MultipartFile file, LocalDate competencia, HoleriteTipo tipo) throws IOException {
        File temp = File.createTempFile("holerite_", ".pdf");
        file.transferTo(temp);

        try {
            List<PdfPageInfoExtractorDTO> infos = extractor.extract(temp.getAbsolutePath());
            int vinculados = 0;
            List<String> naoEncontrados = new ArrayList<>();
            Set<Employee> afetados = new LinkedHashSet<>();
            Set<UUID> jaTem = repository.findEmployeeIdsByCompetenciaAndTipo(competencia, tipo);
            List<String> jaExistiam = new ArrayList<>();

            try (PDDocument doc = Loader.loadPDF(temp)) {
                int total = doc.getNumberOfPages();

                Map<Employee, List<Integer>> employeePages = new LinkedHashMap<>();

                for (int i = 0; i < total; i++) {
                    String cpfRaw = i < infos.size() ? infos.get(i).cpf() : null;
                    String nome   = i < infos.size() ? infos.get(i).nome() : null;
                    String cpfDigits = cpfRaw == null ? "" : cpfRaw.replaceAll("\\D", "");

                    List<Employee> encontrados = cpfDigits.length() >= 11
                            ? employeeRepository.findAllByCpfDigits(cpfDigits)
                            : List.of();

                    // CPF repetido no cadastro não pode derrubar o lote: a página
                    // fica de fora com o motivo, e as outras seguem.
                    if (encontrados.size() > 1) {
                        naoEncontrados.add(ref(nome, i) + " — CPF em mais de um cadastro");
                        continue;
                    }

                    Employee emp = encontrados.size() == 1 ? encontrados.getFirst() : null;

                    if (emp == null) {
                        String rotulo = ref(nome, i);
                        naoEncontrados.add(cpfRaw != null ? rotulo + " (" + cpfRaw + ")" : rotulo);
                        continue;
                    }

                    employeePages.computeIfAbsent(emp, k -> new ArrayList<>()).add(i);
                }

                for (var entry : employeePages.entrySet()) {
                    Employee emp = entry.getKey();
                    List<Integer> pageIndices = entry.getValue();

                    if(jaTem.contains(emp.getId())) {
                        jaExistiam.add(emp.getName());
                        continue;
                    }

                    byte[] pdfBytes = extractPages(doc, pageIndices);
                    String storedPath = storage.save(pdfBytes, emp.getCodParceiro(), competencia, tipo);
                    repository.save(new HoleriteDocumento(emp, competencia, tipo, file.getOriginalFilename(), storedPath));
                    afetados.add(emp);
                    vinculados += pageIndices.size();
                }

                notificarFuncionarios(afetados, competencia, tipo);
                return new VincularHoleriteResultDTO(total, vinculados, naoEncontrados, jaExistiam);
            }
        } finally {
            temp.delete();
        }
    }

    /**
     * Mesma análise do envio, sem escrever nada.
     *
     * O PDF é lido de novo de propósito. Guardar o arquivo entre a prévia e o
     * envio exigiria estado no servidor — e já existe um vazamento desses no
     * PdfController, com um mapa estático que nunca é limpo. Além disso reler é
     * uma funcionalidade: entre conferir e enviar, o RH cadastra quem faltava, e
     * o status precisa mudar.
     */
    public List<HoleritePreviewItemDTO> preview(MultipartFile file, LocalDate competencia, HoleriteTipo tipo) throws IOException {
        File temp = File.createTempFile("holerite_preview_", ".pdf");
        file.transferTo(temp);

        try {
            List<PdfPageInfoExtractorDTO> infos = extractor.extract(temp.getAbsolutePath());
            Set<UUID> jaTem = repository.findEmployeeIdsByCompetenciaAndTipo(competencia, tipo);
            List<HoleritePreviewItemDTO> itens = new ArrayList<>();

            for (int i = 0; i < infos.size(); i++) {
                String cpfRaw = infos.get(i).cpf();
                String nome = infos.get(i).nome();
                String cpfDigits = cpfRaw == null ? "" : cpfRaw.replaceAll("\\D", "");

                if (cpfDigits.length() < 11) {
                    itens.add(new HoleritePreviewItemDTO(i + 1, nome, cpfRaw, null, null, null,
                            HoleritePreviewStatus.CPF_ILEGIVEL));
                    continue;
                }

                List<Employee> encontrados = employeeRepository.findAllByCpfDigits(cpfDigits);

                if (encontrados.size() > 1) {
                    itens.add(new HoleritePreviewItemDTO(i + 1, nome, cpfRaw, null, null, null,
                            HoleritePreviewStatus.CPF_DUPLICADO));
                    continue;
                }

                if (encontrados.isEmpty()) {
                    itens.add(new HoleritePreviewItemDTO(i + 1, nome, cpfRaw, null, null, null,
                            HoleritePreviewStatus.NAO_CADASTRADO));
                    continue;
                }

                Employee emp = encontrados.getFirst();

                HoleritePreviewStatus status =
                        jaTem.contains(emp.getId())                          ? HoleritePreviewStatus.JA_ENVIADO
                                : userRepository.findByEmployee_Id(emp.getId()).isEmpty() ? HoleritePreviewStatus.SEM_USUARIO
                                : HoleritePreviewStatus.PRONTO;

                itens.add(new HoleritePreviewItemDTO(i + 1, nome, cpfRaw,
                        emp.getId(), emp.getName(), emp.getCodParceiro(), status));
            }

            return itens;
        } finally {
            temp.delete();
        }
    }

    /** Notifica (uma vez por funcionário) os usuários cujos holerites foram disponibilizados. */
    private void notificarFuncionarios(Set<Employee> afetados, LocalDate competencia, HoleriteTipo tipo) {
        if (afetados.isEmpty()) return;

        String compLabel = competencia.format(DateTimeFormatter.ofPattern("MM/yyyy"));
        String tipoLabel = tipo == HoleriteTipo.ADIANTAMENTO ? "adiantamento" : "salário";
        String title = "Novo holerite disponível";
        String message = "Seu " + tipoLabel + " de " + compLabel + " já está disponível para download.";

        for (Employee emp : afetados) {
            userRepository.findByEmployee_Id(emp.getId()).ifPresent(user ->
                    notificationService.notify(user.getLogin(), NotificationType.HOLERITE,
                            title, message, "/documentos/holerites"));
        }
    }

    private byte[] extractPage(PDDocument source, int index) throws IOException {
        try (PDDocument single = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            single.importPage(source.getPage(index));
            single.save(baos);
            return baos.toByteArray();
        }
    }

    private byte[] extractPages(PDDocument source, List<Integer> indices) throws IOException{
        try(PDDocument multi = new PDDocument();
            ByteArrayOutputStream baos = new ByteArrayOutputStream()){
            for(int index : indices){
                multi.importPage(source.getPage(index));
            }
            multi.save(baos);
            return baos.toByteArray();
        }
    }

    /** Lista os holerites do funcionário vinculado ao login. */
    public List<HoleriteResponseDTO> listarDoFuncionario(String login) {
        Employee emp = resolveEmployee(login);
        if (emp == null) return List.of();

        return repository.findByEmployeeOrderByCompetenciaDesc(emp).stream()
                .map(h -> new HoleriteResponseDTO(h.getId(), h.getCompetencia(), h.getTipo(), h.getOriginalFilename(), h.getCreatedAt()))
                .toList();
    }

    public Optional<HoleriteDocumento> buscar(UUID id) {
        return repository.findById(id);
    }

    /** Permite o dono do holerite ou um usuário de RH/ADMIN. */
    public boolean podeAcessar(HoleriteDocumento doc, String login, boolean isRh) {
        if (isRh) return true;
        Employee emp = resolveEmployee(login);
        return emp != null && doc.getEmployee().getId().equals(emp.getId());
    }

    public byte[] lerArquivo(HoleriteDocumento doc) throws IOException {
        return Files.readAllBytes(storage.resolve(doc.getStoragePath()));
    }

    private String ref(String nome, int pagina) {
        return nome != null && !nome.isBlank() ? nome : "Página " + (pagina + 1);
    }
}
