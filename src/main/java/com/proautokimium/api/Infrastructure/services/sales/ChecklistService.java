package com.proautokimium.api.Infrastructure.services.sales;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proautokimium.api.Application.DTOs.sales.ChecklistDetailDTO;
import com.proautokimium.api.Application.DTOs.sales.ChecklistSubmitDTO;
import com.proautokimium.api.Application.DTOs.sales.ChecklistSummaryDTO;
import com.proautokimium.api.Infrastructure.exceptions.sales.ChecklistNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.repositories.sales.ChecklistChangeRepository;
import com.proautokimium.api.Infrastructure.repositories.sales.ChecklistEventRepository;
import com.proautokimium.api.Infrastructure.repositories.sales.ChecklistRepository;
import com.proautokimium.api.Infrastructure.repositories.sales.ChecklistVersionRepository;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.sales.Checklist;
import com.proautokimium.api.domain.entities.sales.ChecklistChange;
import com.proautokimium.api.domain.entities.sales.ChecklistEvent;
import com.proautokimium.api.domain.entities.sales.ChecklistVersion;
import com.proautokimium.api.domain.enums.sales.ChecklistEventType;
import com.proautokimium.api.domain.enums.sales.ChecklistStatus;
import com.proautokimium.api.domain.exceptions.sales.ChecklistTransitionException;
import com.proautokimium.api.domain.valueObjects.BrazilianDocument;
import com.proautokimium.api.domain.valueObjects.sales.ChecklistContent;
import com.proautokimium.api.domain.valueObjects.sales.ChecklistDiff;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * O checklist de vendas: envio do celular, análise da Controladoria, pedido de
 * alteração e histórico.
 *
 * <p><b>O envio é repetível.</b> O celular manda o id do checklist (gerado lá) e
 * a versão que está criando. Se a internet caiu depois de o servidor gravar, o
 * aparelho manda de novo o mesmo par; o servidor reconhece e devolve o que já
 * tem, sem duplicar e sem gerar histórico falso.
 *
 * <p><b>Checklist de outro vendedor é 404</b>, e não 403: dizer "existe, mas
 * não é seu" confirmaria que o id é válido.
 */
@Service
public class ChecklistService {

    private final ChecklistRepository repository;
    private final ChecklistVersionRepository versionRepository;
    private final ChecklistChangeRepository changeRepository;
    private final ChecklistEventRepository eventRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final ChecklistNotifier notifier;
    private final ObjectMapper mapper;
    private final Clock clock;

    public ChecklistService(ChecklistRepository repository, ChecklistVersionRepository versionRepository,
                            ChecklistChangeRepository changeRepository, ChecklistEventRepository eventRepository,
                            UserRepository userRepository, EmployeeRepository employeeRepository,
                            ChecklistNotifier notifier, ObjectMapper mapper, Clock clock) {
        this.repository = repository;
        this.versionRepository = versionRepository;
        this.changeRepository = changeRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.notifier = notifier;
        this.mapper = mapper;
        this.clock = clock;
    }

    // ── Vendedor ─────────────────────────────────────────────────────────────

    @Transactional
    public ChecklistDetailDTO submit(UUID id, ChecklistSubmitDTO dto, String login) {
        LocalDateTime now = LocalDateTime.now(clock);
        Optional<Checklist> existing = repository.findById(id);

        if (existing.isEmpty()) {
            if (dto.revision() != 1) {
                throw new ChecklistTransitionException(
                        "Este checklist não foi encontrado no servidor. Fale com a Controladoria.");
            }
            String name = displayName(login);
            Checklist created = Checklist.submit(id, login, name, dto.content(), dto.filledOffline(),
                    dto.deviceStartedAt(), now);
            repository.saveAndFlush(created);
            versionRepository.save(ChecklistVersion.of(created, login, now));
            eventRepository.save(ChecklistEvent.of(created, ChecklistEventType.SUBMITTED, login, name,
                    created.isFilledOffline() ? "Preenchido sem internet" : null, now));
            notifier.submitted(created);
            return detail(created);
        }

        Checklist checklist = existing.get();
        if (!checklist.isOwnedBy(login)) {
            throw new ChecklistNotFoundException();
        }
        // O mesmo envio de novo (a resposta se perdeu no caminho): já está gravado.
        if (dto.revision() <= checklist.getVersion()) {
            return detail(checklist);
        }
        if (dto.revision() != checklist.getVersion() + 1) {
            throw new ChecklistTransitionException(
                    "Este checklist mudou desde que foi aberto no celular. Atualize a lista e tente de novo.");
        }

        ChecklistContent before = checklist.getContent();
        checklist.resubmit(login, dto.content(), now);
        repository.save(checklist);
        versionRepository.save(ChecklistVersion.of(checklist, login, now));
        for (ChecklistDiff.Change change : ChecklistDiff.between(before, checklist.getContent(), mapper)) {
            changeRepository.save(ChecklistChange.of(checklist.getId(), checklist.getVersion(), change.field(),
                    blankToNull(change.before()), blankToNull(change.after()), login, now));
        }
        eventRepository.save(ChecklistEvent.of(checklist, ChecklistEventType.RESUBMITTED, login,
                checklist.getSellerName(), null, now));
        notifier.submitted(checklist);
        return detail(checklist);
    }

    @Transactional(readOnly = true)
    public List<ChecklistSummaryDTO> listMine(String login) {
        return repository.findBySellerLoginOrderByLastSubmittedAtDesc(login).stream()
                .map(ChecklistSummaryDTO::from).toList();
    }

    @Transactional
    public ChecklistDetailDTO requestChange(UUID id, String reason, String login) {
        Checklist checklist = findVisible(id, login, false);
        checklist.requestChange(login, reason, LocalDateTime.now(clock));
        repository.save(checklist);
        event(checklist, ChecklistEventType.CHANGE_REQUESTED, login, checklist.getChangeReason());
        notifier.changeRequested(checklist);
        return detail(checklist);
    }

    // ── Controladoria ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ChecklistSummaryDTO> listAll(List<ChecklistStatus> statuses) {
        List<Checklist> list = statuses == null || statuses.isEmpty()
                ? repository.findAllByOrderByLastSubmittedAtDesc()
                : repository.findByStatusInOrderByLastSubmittedAtDesc(statuses);
        return list.stream().map(ChecklistSummaryDTO::from).toList();
    }

    @Transactional
    public ChecklistDetailDTO approve(UUID id, String notes, String login) {
        Checklist checklist = find(id);
        checklist.approve(login, notes, LocalDateTime.now(clock));
        return answered(checklist, ChecklistEventType.APPROVED, login, notes, "Checklist aprovado",
                "O checklist " + ref(checklist) + " foi aprovado pela Controladoria.");
    }

    @Transactional
    public ChecklistDetailDTO returnToSeller(UUID id, String notes, String login) {
        Checklist checklist = find(id);
        checklist.returnToSeller(login, notes, LocalDateTime.now(clock));
        return answered(checklist, ChecklistEventType.RETURNED, login, notes, "Checklist devolvido para correção",
                "O checklist " + ref(checklist) + " voltou para você corrigir: \"" + checklist.getReviewNotes() + "\"");
    }

    @Transactional
    public ChecklistDetailDTO grantChange(UUID id, String notes, String login) {
        Checklist checklist = find(id);
        checklist.grantChange(login, notes, LocalDateTime.now(clock));
        return answered(checklist, ChecklistEventType.CHANGE_GRANTED, login, notes, "Alteração liberada",
                "Você já pode alterar o checklist " + ref(checklist) + ". Depois, envie de novo.");
    }

    @Transactional
    public ChecklistDetailDTO denyChange(UUID id, String notes, String login) {
        Checklist checklist = find(id);
        checklist.denyChange(login, notes, LocalDateTime.now(clock));
        return answered(checklist, ChecklistEventType.CHANGE_DENIED, login, notes, "Alteração negada",
                "A alteração do checklist " + ref(checklist) + " foi negada: \"" + checklist.getReviewNotes() + "\"");
    }

    // ── Leitura ──────────────────────────────────────────────────────────────

    /**
     * O checklist aberto. {@code reviewer} é quem tem a tela da Controladoria:
     * vê qualquer um. O vendedor vê só os seus.
     */
    @Transactional(readOnly = true)
    public ChecklistDetailDTO detail(UUID id, String login, boolean reviewer) {
        return detail(findVisible(id, login, reviewer));
    }

    /** Para o PDF: a mesma regra de quem pode ver. */
    @Transactional(readOnly = true)
    public Checklist findVisible(UUID id, String login, boolean reviewer) {
        Checklist checklist = find(id);
        if (!reviewer && !checklist.isOwnedBy(login)) {
            throw new ChecklistNotFoundException();
        }
        return checklist;
    }

    private ChecklistDetailDTO detail(Checklist c) {
        List<ChecklistDetailDTO.Event> events = eventRepository.findByChecklistIdOrderByCreatedAtAsc(c.getId())
                .stream()
                .map(e -> new ChecklistDetailDTO.Event(e.getType(), e.getVersion(), e.getActorLogin(),
                        e.getActorName(), e.getNotes(), e.getCreatedAt()))
                .toList();
        List<ChecklistDetailDTO.Change> changes = changeRepository.findByChecklistIdOrderByChangedAtAsc(c.getId())
                .stream()
                .map(ch -> new ChecklistDetailDTO.Change(ch.getVersion(), ch.getField(), ch.getOldValue(),
                        ch.getNewValue(), ch.getChangedBy(), ch.getChangedAt()))
                .toList();
        return new ChecklistDetailDTO(ChecklistSummaryDTO.from(c), c.getContent(), events, changes,
                erpDifferences(c.getContent()));
    }

    // ── O que está diferente do Sankhya ──────────────────────────────────────

    /**
     * Os campos em que o checklist diz uma coisa e o Sankhya outra. O retrato do
     * ERP ({@code customer.erp}) é tirado no celular, quando o vendedor escolhe
     * o cliente; comparar com ele, e não com uma consulta nova, mostra o que o
     * vendedor mudou — e não o que alguém mudou no ERP depois.
     */
    static List<ChecklistDetailDTO.ErpDifference> erpDifferences(ChecklistContent content) {
        if (content == null || content.customer() == null || content.customer().erp() == null
                || content.customer().newCustomer()) {
            return List.of();
        }
        Map<String, String> erp = content.customer().erp();
        ChecklistContent.Customer c = content.customer();
        ChecklistContent.Address a = content.mainAddress();

        Map<String, ErpField> fields = new LinkedHashMap<>();
        fields.put("name", new ErpField("Nome", c.name(), ChecklistService::text));
        fields.put("legalName", new ErpField("Razão social", c.legalName(), ChecklistService::text));
        fields.put("document", new ErpField("CNPJ / CPF", c.document(), BrazilianDocument::digits));
        fields.put("stateRegistration", new ErpField("Inscrição estadual", c.stateRegistration(), ChecklistService::text));
        fields.put("mainPhone", new ErpField("Telefone principal", c.mainPhone(), BrazilianDocument::digits));
        fields.put("invoiceEmail", new ErpField("E-mail das notas fiscais", c.invoiceEmail(), ChecklistService::text));
        if (a != null) {
            fields.put("zipCode", new ErpField("CEP", a.zipCode(), BrazilianDocument::digits));
            fields.put("street", new ErpField("Rua", a.street(), ChecklistService::text));
            fields.put("number", new ErpField("Número", a.number(), ChecklistService::text));
            fields.put("complement", new ErpField("Complemento", a.complement(), ChecklistService::text));
            fields.put("district", new ErpField("Bairro", a.district(), ChecklistService::text));
            fields.put("city", new ErpField("Cidade", a.city(), ChecklistService::text));
            fields.put("state", new ErpField("Estado", a.state(), ChecklistService::text));
        }

        List<ChecklistDetailDTO.ErpDifference> differences = new ArrayList<>();
        fields.forEach((key, field) -> {
            if (!erp.containsKey(key)) {
                return;
            }
            String before = erp.get(key);
            if (!field.normalize().apply(nullToEmpty(before)).equals(field.normalize().apply(nullToEmpty(field.value())))) {
                differences.add(new ChecklistDetailDTO.ErpDifference(field.label(), before, field.value()));
            }
        });
        return differences;
    }

    private record ErpField(String label, String value, Function<String, String> normalize) {}

    /** Caixa, acento e espaço sobrando não contam como diferença: o ERP grava tudo em maiúsculas. */
    private static String text(String value) {
        String noAccents = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return noAccents.trim().replaceAll("\\s+", " ").toUpperCase();
    }

    // ── Apoio ────────────────────────────────────────────────────────────────

    /**
     * A mensagem chega pronta, sem {@code formatted()}: ela carrega o motivo que
     * a Controladoria digitou, e um "%" ali ("desconto de 10%") derrubaria a
     * formatação — e a ação inteira.
     */
    private ChecklistDetailDTO answered(Checklist checklist, ChecklistEventType type, String login, String notes,
                                        String title, String message) {
        repository.save(checklist);
        event(checklist, type, login, notes);
        notifier.answered(checklist, title, message);
        return detail(checklist);
    }

    private static String ref(Checklist checklist) {
        return "nº " + ChecklistNotifier.number(checklist) + " — " + checklist.getCustomerName();
    }

    private void event(Checklist checklist, ChecklistEventType type, String login, String notes) {
        eventRepository.save(ChecklistEvent.of(checklist, type, login, displayName(login), notes,
                LocalDateTime.now(clock)));
    }

    private Checklist find(UUID id) {
        return repository.findById(id).orElseThrow(ChecklistNotFoundException::new);
    }

    /** O nome do funcionário ligado ao usuário; sem vínculo, o login. */
    String displayName(String login) {
        Employee viaLink = userRepository.findByLoginWithEmployee(login).map(u -> u.getEmployee()).orElse(null);
        Employee employee = viaLink != null ? viaLink : employeeRepository.findByUsername(login).orElse(null);
        return employee != null && employee.getName() != null ? employee.getName() : login;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
