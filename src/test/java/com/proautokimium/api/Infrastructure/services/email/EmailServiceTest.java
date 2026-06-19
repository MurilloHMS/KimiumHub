package com.proautokimium.api.Infrastructure.services.email;

import com.proautokimium.api.Application.DTOs.email.SmtpEmailRequestDTO;
import com.proautokimium.api.Infrastructure.repositories.SmtpEmailRepository;
import com.proautokimium.api.domain.entities.EmailEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private SmtpEmailRepository repository;

    @InjectMocks
    private EmailService emailService;

    private SmtpEmailRequestDTO dto;
    private EmailEntity emailEntity;

    @BeforeEach
    void setUp() {
        dto = new SmtpEmailRequestDTO("remetente-teste");
        emailEntity = mock(EmailEntity.class);
    }

    @Test
    @DisplayName("Deve salvar email com endereço gerado automaticamente")
    void deveSalvarEmailComSucesso() {
        emailService.saveEmail(dto);

        verify(repository).save(any(EmailEntity.class));
    }

    @Test
    @DisplayName("Deve retornar todos os emails cadastrados")
    void deveRetornarTodosOsEmails() {
        when(repository.findAll()).thenReturn(List.of(emailEntity));

        Set<EmailEntity> result = emailService.getAll();

        assertThat(result).hasSize(1);
        verify(repository).findAll();
    }

    @Test
    @DisplayName("Deve retornar conjunto vazio quando não há emails")
    void deveRetornarConjuntoVazioQuandoNaoHaEmails() {
        when(repository.findAll()).thenReturn(List.of());

        Set<EmailEntity> result = emailService.getAll();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar email por nome")
    void deveRetornarEmailPorNome() {
        when(emailEntity.getName()).thenReturn("remetente-teste");
        when(repository.findByName("remetente-teste")).thenReturn(emailEntity);

        EmailEntity result = emailService.GetByName(dto);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("remetente-teste");
    }

    @Test
    @DisplayName("Deve atualizar email com sucesso")
    void deveAtualizarEmailComSucesso() {
        when(repository.findByName("remetente-teste")).thenReturn(emailEntity);

        emailService.updateEmail(dto);

        verify(repository).save(emailEntity);
    }

    @Test
    @DisplayName("Deve deletar email com sucesso")
    void deveDeletarEmailComSucesso() {
        when(repository.findByName("remetente-teste")).thenReturn(emailEntity);

        emailService.deleteEmail(dto);

        verify(repository).delete(emailEntity);
    }
}
