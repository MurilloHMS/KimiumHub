package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Application.DTOs.humanResources.ReportRecipient.HrReportRecipientDTO;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.ReportRecipientAlreadyExistsException;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.ReportRecipientNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.humanResources.HrReportRecipientRepository;
import com.proautokimium.api.domain.entities.humanResources.HrReportRecipient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** A lista de quem recebe os relatórios do RH por e-mail. */
@Service
public class HrReportRecipientService {

    private final HrReportRecipientRepository repository;
    private final Clock clock;

    public HrReportRecipientService(HrReportRecipientRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public List<HrReportRecipientDTO> list() {
        return repository.findAllByOrderByEmailAsc().stream().map(HrReportRecipientDTO::from).toList();
    }

    /** Os endereços, para o envio. */
    public List<String> emails() {
        return repository.findAllByOrderByEmailAsc().stream().map(HrReportRecipient::getEmail).toList();
    }

    @Transactional
    public HrReportRecipientDTO add(String email, String login) {
        HrReportRecipient recipient = HrReportRecipient.create(email, login, LocalDateTime.now(clock));
        // Confere antes de gravar: a constraint pegaria também, mas como 500 genérico.
        if (repository.existsByEmail(recipient.getEmail())) {
            throw new ReportRecipientAlreadyExistsException();
        }
        return HrReportRecipientDTO.from(repository.save(recipient));
    }

    @Transactional
    public void remove(UUID id) {
        if (!repository.existsById(id)) {
            throw new ReportRecipientNotFoundException();
        }
        repository.deleteById(id);
    }
}
