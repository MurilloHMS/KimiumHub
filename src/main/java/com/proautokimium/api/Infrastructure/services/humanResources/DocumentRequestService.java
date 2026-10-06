package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Infrastructure.exceptions.humanResources.DocumentRequestNotFoundException;
import com.proautokimium.api.Infrastructure.exceptions.humanResources.DocumentRequestRecipientNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.DocumentRequestRecipientRepository;
import com.proautokimium.api.Infrastructure.repositories.humanResources.DocumentRequestRepository;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.entities.humanResources.DocumentRequest;
import com.proautokimium.api.domain.entities.humanResources.DocumentRequestRecipient;
import com.proautokimium.api.domain.exceptions.partners.EmployeeNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class DocumentRequestService {

    private final DocumentRequestRepository documentRequestRepository;
    private final DocumentRequestRecipientRepository documentRequestRecipientRepository;
    private final Clock clock;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    public DocumentRequestService(DocumentRequestRepository documentRequestRepository, DocumentRequestRecipientRepository documentRequestRecipientRepository, Clock clock, EmployeeRepository employeeRepository, UserRepository userRepository) {
        this.documentRequestRepository = documentRequestRepository;
        this.documentRequestRecipientRepository = documentRequestRecipientRepository;
        this.clock = clock;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
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

    @Transactional
    public DocumentRequestRecipient approve(UUID recipientId, String reviewerLogin){
        DocumentRequestRecipient recipient = documentRequestRecipientRepository.findById(recipientId)
                .orElseThrow(DocumentRequestRecipientNotFoundException::new);

        recipient.approve(reviewerLogin, LocalDateTime.now(clock));
        return documentRequestRecipientRepository.save(recipient);
    }

    @Transactional
    public DocumentRequestRecipient giveBack(UUID recipientId, String reviewerLogin, String reason){
        DocumentRequestRecipient recipient = documentRequestRecipientRepository.findById(recipientId)
                .orElseThrow(DocumentRequestRecipientNotFoundException::new);

        recipient.giveBack(reviewerLogin, reason, LocalDateTime.now(clock));
        return documentRequestRecipientRepository.save(recipient);
    }

    @Transactional
    public DocumentRequestRecipient submit(UUID recipientId, String login, Map<String, Object> answers){
        DocumentRequestRecipient recipient = documentRequestRecipientRepository.findById(recipientId)
                .orElseThrow(DocumentRequestRecipientNotFoundException::new);

        Employee caller = resolveEmployee(login);
        if(caller == null) throw new EmployeeNotFoundException();

        // Quem não é dono recebe o mesmo 404 de "não existe": assim não descobre quais IDs existem.
        if(!isOwner(recipient, caller)) throw new DocumentRequestRecipientNotFoundException();

        recipient.submit(answers, LocalDateTime.now(clock));
        return documentRequestRecipientRepository.save(recipient);
    }

    // Methods
    private static boolean isOwner(DocumentRequestRecipient recipient, Employee caller){
        Employee owner = recipient.getEmployee();
        return owner == caller || (caller.getId() != null && caller.getId().equals(owner.getId()));
    }

    private Employee resolveEmployee(String login){
        Employee viaLink = userRepository.findByLoginWithEmployee(login)
                .map(User::getEmployee)
                .orElse(null);
        if(viaLink != null) return viaLink;
        return employeeRepository.findByUsername(login).orElse(null);
    }
}
