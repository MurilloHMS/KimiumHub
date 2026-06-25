package com.proautokimium.api.Infrastructure.services.holerite;

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
import jakarta.transaction.Transactional;
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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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
    @Transactional
    public VincularHoleriteResultDTO vincular(MultipartFile file, LocalDate competencia, HoleriteTipo tipo) throws IOException {
        File temp = File.createTempFile("holerite_", ".pdf");
        file.transferTo(temp);

        try {
            List<PdfPageInfoExtractorDTO> infos = extractor.extract(temp.getAbsolutePath());
            int vinculados = 0;
            List<String> naoEncontrados = new ArrayList<>();
            Set<Employee> afetados = new LinkedHashSet<>();   // dedupe por id (Entity.equals)

            try (PDDocument doc = Loader.loadPDF(temp)) {
                int total = doc.getNumberOfPages();

                for (int i = 0; i < total; i++) {
                    String cpfRaw = i < infos.size() ? infos.get(i).cpf() : null;
                    String nome = i < infos.size() ? infos.get(i).nome() : null;
                    String cpfDigits = cpfRaw == null ? "" : cpfRaw.replaceAll("\\D", "");

                    Employee emp = cpfDigits.length() >= 11
                            ? employeeRepository.findByCpfDigits(cpfDigits).orElse(null)
                            : null;

                    if (emp == null) {
                        String ref = nome != null && !nome.isBlank() ? nome : "Página " + (i + 1);
                        naoEncontrados.add(cpfRaw != null ? ref + " (" + cpfRaw + ")" : ref);
                        continue;
                    }

                    byte[] pageBytes = extractPage(doc, i);
                    String storedPath = storage.save(pageBytes, emp.getCodParceiro(), competencia, tipo);
                    repository.save(new HoleriteDocumento(emp, competencia, tipo, file.getOriginalFilename(), storedPath));
                    afetados.add(emp);
                    vinculados++;
                }

                notificarFuncionarios(afetados, competencia, tipo);
                return new VincularHoleriteResultDTO(total, vinculados, naoEncontrados);
            }
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
}
