package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Infrastructure.exceptions.humanResources.DocumentRequestNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.DocumentRequestFileRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.DocumentRequestRecipientRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.DocumentRequestRepository;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.humanResources.DocumentRequest;
import com.proautokimium.api.domain.entities.humanResources.DocumentRequestRecipient;
import com.proautokimium.api.domain.exceptions.partners.EmployeeNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentRequestService {

    private final DocumentRequestRepository documentRequestRepository;
    private final DocumentRequestRecipientRepository documentRequestRecipientRepository;
    private final Clock clock;
    private final EmployeeRepository employeeRepository;

    public DocumentRequestService(DocumentRequestRepository documentRequestRepository, DocumentRequestRecipientRepository documentRequestRecipientRepository, Clock clock, EmployeeRepository employeeRepository) {
        this.documentRequestRepository = documentRequestRepository;
        this.documentRequestRecipientRepository = documentRequestRecipientRepository;
        this.clock = clock;
        this.employeeRepository = employeeRepository;
    }

    @Transactional
    public DocumentRequest createDraft(String title, String login){
        DocumentRequest request = DocumentRequest.draft(title, login, LocalDateTime.now(clock));
        return documentRequestRepository.save(request);
    }

    @Transactional
    public DocumentRequest send(UUID id, List<UUID> employeeIds){

        LocalDateTime now = LocalDateTime.now(clock);

        DocumentRequest request = documentRequestRepository.findById(id)
                .orElseThrow(DocumentRequestNotFoundException::new);

        request.send(now);

        for(UUID employeeID : employeeIds) {
            Employee employee = employeeRepository.findById(employeeID)
                    .orElseThrow(EmployeeNotFoundException::new);

            DocumentRequestRecipient recipient = DocumentRequestRecipient.create(request, employee, now);
            documentRequestRecipientRepository.save(recipient);
        }
        return documentRequestRepository.save(request);
    }
}
