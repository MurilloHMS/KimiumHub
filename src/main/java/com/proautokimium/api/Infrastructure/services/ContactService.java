package com.proautokimium.api.Infrastructure.services;

import com.proautokimium.api.Application.DTOs.contact.ContactDTO;
import com.proautokimium.api.Infrastructure.repositories.ContactRepository;
import com.proautokimium.api.domain.entities.Contact;
import com.proautokimium.api.domain.valueObjects.Email;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContactService {

    private final ContactRepository repository;

    public ContactService(ContactRepository repository) {
        this.repository = repository;
    }

    public List<ContactDTO> getAllContact(){
        var contacts = repository.findAll();
        return contacts.stream().map(c -> new ContactDTO(
                c.getName(),
                c.getEmail().getAddress(),
                c.getContactType(),
                c.getOther(),
                c.getMessage(),
                c.getBusinessName(),
                c.getContactStatus(),
                c.getContactDate()
        )).toList();
    }

    @Transactional
    public void createContact(ContactDTO dto){
        Contact contact = new Contact();

        contact.setBusinessName(dto.businessName());
        contact.setContactDate(dto.contactDate());
        contact.setContactStatus(dto.contactStatus());
        contact.setContactType(dto.contactType());
        contact.setEmail(new Email(dto.email()));
        contact.setMessage(dto.message());
        contact.setName(dto.name());
        contact.setOther(dto.other());

        repository.save(contact);
    }
}
