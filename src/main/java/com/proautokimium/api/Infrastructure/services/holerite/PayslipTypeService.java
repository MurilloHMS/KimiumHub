package com.proautokimium.api.Infrastructure.services.holerite;

import com.proautokimium.api.Application.DTOs.holerite.PayslipTypeDTOs.CreatePayslipTypeResult;
import com.proautokimium.api.Application.DTOs.holerite.PayslipTypeDTOs.PayslipTypeDTO;
import com.proautokimium.api.Infrastructure.exceptions.holerite.InvalidPayslipTypeException;
import com.proautokimium.api.Infrastructure.repositories.PayslipTypeRepository;
import com.proautokimium.api.domain.entities.PayslipType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Os tipos de holerite, cadastrados pelo RH.
 *
 * Criar é pela tela de envio, no meio do trabalho: o RH digita "PLR" e já
 * envia. Por isso o nome repetido não é erro — devolve o que existe, e a tela
 * seleciona. "PLR" e "plr" lado a lado separariam a auditoria e o filtro do
 * funcionário em dois.
 */
@Service
public class PayslipTypeService {

    private final PayslipTypeRepository repository;
    private final Clock clock;

    public PayslipTypeService(PayslipTypeRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<PayslipTypeDTO> list() {
        return repository.findByActiveTrueOrderBySortOrderAscLabelAsc().stream().map(PayslipTypeDTO::from).toList();
    }

    @Transactional
    public CreatePayslipTypeResult create(String rawLabel, String login) {
        String label = rawLabel == null ? "" : rawLabel.strip().replaceAll("\\s+", " ");
        if (label.isEmpty()) throw new InvalidPayslipTypeException("Informe o nome do tipo.");
        if (label.length() > PayslipType.LABEL_MAX) {
            throw new InvalidPayslipTypeException("O nome tem no máximo " + PayslipType.LABEL_MAX + " caracteres.");
        }

        String key = comparable(label);
        Optional<PayslipType> same = repository.findAll().stream()
                .filter(t -> comparable(t.getLabel()).equals(key))
                .findFirst();
        if (same.isPresent()) {
            return new CreatePayslipTypeResult(PayslipTypeDTO.from(same.get()), false);
        }

        String base = codeOf(label);
        if (base.isEmpty()) throw new InvalidPayslipTypeException("Use letras ou números no nome do tipo.");
        String code = base;
        for (int n = 2; repository.existsByCode(code); n++) {
            String suffix = "_" + n;
            code = base.substring(0, Math.min(base.length(), PayslipType.CODE_MAX - suffix.length())) + suffix;
        }

        PayslipType saved = repository.save(new PayslipType(code, label, repository.findMaxSortOrder() + 10,
                login, LocalDateTime.now(clock)));
        return new CreatePayslipTypeResult(PayslipTypeDTO.from(saved), true);
    }

    /** O tipo pelo código, se existir e estiver ativo — senão, 400 dizendo qual. */
    @Transactional(readOnly = true)
    public PayslipType requireActive(String rawCode) {
        String code = rawCode == null ? "" : rawCode.strip().toUpperCase(Locale.ROOT);
        return repository.findByCode(code)
                .filter(PayslipType::isActive)
                .orElseThrow(() -> new InvalidPayslipTypeException(
                        "Tipo de holerite desconhecido: " + (code.isEmpty() ? "(vazio)" : code) + "."));
    }

    /**
     * "Férias coletivas 2026" → FERIAS_COLETIVAS_2026. Só A-Z, 0-9 e `_`: o
     * código vai para o nome do arquivo no disco e para a URL.
     */
    static String codeOf(String label) {
        String code = stripAccents(label).toUpperCase(Locale.ROOT)
                .replace("º", "").replace("ª", "")
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return code.length() > PayslipType.CODE_MAX ? code.substring(0, PayslipType.CODE_MAX).replaceAll("_+$", "") : code;
    }

    private static String comparable(String label) {
        return stripAccents(label).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").strip();
    }

    private static String stripAccents(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
