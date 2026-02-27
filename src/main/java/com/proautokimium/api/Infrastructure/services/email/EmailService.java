package com.proautokimium.api.Infrastructure.services.email;

import com.proautokimium.api.Application.DTOs.email.SmtpEmailRequestDTO;
import com.proautokimium.api.Infrastructure.repositories.SmtpEmailRepository;
import com.proautokimium.api.domain.entities.EmailEntity;
import com.proautokimium.api.domain.valueObjects.Email;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class EmailService {
    private final SmtpEmailRepository repository;

    public EmailService(SmtpEmailRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void saveEmail(SmtpEmailRequestDTO dto){
        EmailEntity email = new EmailEntity();
        email.setName(dto.nome());
        String completeEmail = dto.nome() + "@envios.proautokimium.com.br";
        email.setEmail(new Email(completeEmail));

        repository.save(email);
    }

    public EmailEntity GetByName(SmtpEmailRequestDTO dto){
        return repository.findByName(dto.nome());
    }

    public Set<EmailEntity> getAll(){
        return new HashSet<>(repository.findAll());
    }

    @Transactional
    public void updateEmail(SmtpEmailRequestDTO dto){
        EmailEntity email = repository.findByName(dto.nome());
        String completeEmail = dto.nome() + "@envios.proautokimium.com.br";
        email.setEmail(new Email(completeEmail));
        repository.save(email);
    }
    @Transactional
    public void deleteEmail(SmtpEmailRequestDTO dto){
        EmailEntity email = repository.findByName(dto.nome());
        repository.delete(email);
    }
}
