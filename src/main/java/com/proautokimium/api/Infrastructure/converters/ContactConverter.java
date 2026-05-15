package com.proautokimium.api.Infrastructure.converters;

import com.proautokimium.api.Application.DTOs.contact.CreateContactDTO;
import com.proautokimium.api.Application.DTOs.contact.ContactResponseDTO;
import com.proautokimium.api.Infrastructure.interfaces.converters.DtoConverter;
import com.proautokimium.api.domain.entities.Contact;
import com.proautokimium.api.domain.valueObjects.Email;
import org.springframework.stereotype.Component;

@Component
public class ContactConverter implements DtoConverter<Contact, ContactResponseDTO, CreateContactDTO> {
    @Override
    public ContactResponseDTO toDto(Contact entity) {
        return new ContactResponseDTO(
                entity.getName(),
                entity.getEmail().getAddress(),
                entity.getContactType(),
                entity.getOther(),
                entity.getMessage(),
                entity.getBusinessName(),
                entity.getContactStatus(),
                entity.getContactDate()
        );
    }

    @Override
    public Contact fromCreateDto(CreateContactDTO dto) {
        Contact contact = new Contact();
        contact.setName(dto.name());
        contact.setEmail(new Email(dto.email()));
        contact.setContactType(dto.contactType());
        contact.setOther(dto.other());
        contact.setMessage(dto.message());
        contact.setBusinessName(dto.businessName());
        contact.setContactStatus(dto.contactStatus());
        contact.setContactDate(dto.contactDate());

        return contact;
    }
}
