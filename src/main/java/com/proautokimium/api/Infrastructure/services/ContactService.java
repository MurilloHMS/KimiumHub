package com.proautokimium.api.Infrastructure.services;

import com.proautokimium.api.Application.DTOs.contact.ContactResponseDTO;
import com.proautokimium.api.Application.DTOs.contact.CreateContactDTO;
import com.proautokimium.api.Infrastructure.converters.ContactConverter;
import com.proautokimium.api.Infrastructure.repositories.ContactRepository;
import com.proautokimium.api.domain.entities.Contact;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContactService {

    private final ContactRepository repository;
    private final ContactConverter converter;

    public ContactService(ContactRepository repository, ContactConverter converter) {
        this.repository = repository;
        this.converter = converter;
    }

    public List<ContactResponseDTO> getAllContact(){
        var contacts = repository.findAll();
        return contacts.stream().map(converter::toDto).toList();
    }

    @Transactional
    public void createContact(CreateContactDTO dto){
        Contact contact = converter.fromCreateDto(dto);
        repository.save(contact);
    }
}
