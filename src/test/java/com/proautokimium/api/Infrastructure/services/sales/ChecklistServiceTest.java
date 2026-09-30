package com.proautokimium.api.Infrastructure.services.sales;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.proautokimium.api.Application.DTOs.sales.ChecklistDetailDTO;
import com.proautokimium.api.Application.DTOs.sales.ChecklistSubmitDTO;
import com.proautokimium.api.Infrastructure.exceptions.sales.ChecklistNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.repositories.sales.ChecklistChangeRepository;
import com.proautokimium.api.Infrastructure.repositories.sales.ChecklistEventRepository;
import com.proautokimium.api.Infrastructure.repositories.sales.ChecklistRepository;
import com.proautokimium.api.Infrastructure.repositories.sales.ChecklistVersionRepository;
import com.proautokimium.api.domain.entities.sales.Checklist;
import com.proautokimium.api.domain.entities.sales.ChecklistChange;
import com.proautokimium.api.domain.enums.sales.ChecklistStatus;
import com.proautokimium.api.domain.exceptions.sales.ChecklistTransitionException;
import com.proautokimium.api.domain.valueObjects.sales.ChecklistContent;
import com.proautokimium.api.domain.valueObjects.sales.ChecklistFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * O envio do celular. O que se protege: repetir o mesmo envio não duplica nem
 * gera histórico; o checklist de outro vendedor é 404; o reenvio grava o que
 * mudou com nome de gente.
 */
class ChecklistServiceTest {

    final ChecklistRepository repository = mock(ChecklistRepository.class);
    final ChecklistVersionRepository versions = mock(ChecklistVersionRepository.class);
    final ChecklistChangeRepository changes = mock(ChecklistChangeRepository.class);
    final ChecklistEventRepository events = mock(ChecklistEventRepository.class);
    final UserRepository users = mock(UserRepository.class);
    final EmployeeRepository employees = mock(EmployeeRepository.class);
    final ChecklistNotifier notifier = mock(ChecklistNotifier.class);
    final Clock clock = Clock.fixed(Instant.parse("2026-09-30T13:00:00Z"), ZoneId.of("America/Sao_Paulo"));

    ChecklistService service;

    static final UUID ID = UUID.fromString("7d7f5a0c-1c2b-4c8e-9a51-2f7b3f7e0a11");

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        service = new ChecklistService(repository, versions, changes, events, users, employees, notifier, mapper, clock);
        when(users.findByLoginWithEmployee(anyString())).thenReturn(Optional.empty());
        when(employees.findByUsername(anyString())).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    static ChecklistSubmitDTO dto(int revision, ChecklistContent content) {
        return new ChecklistSubmitDTO(revision, content, true, LocalDateTime.of(2026, 9, 30, 8, 0));
    }

    @Test
    @DisplayName("primeiro envio: grava o checklist, a versão 1 e o evento, e avisa a Controladoria")
    void firstSubmission() {
        when(repository.findById(ID)).thenReturn(Optional.empty());

        ChecklistDetailDTO result = service.submit(ID, dto(1, ChecklistFixtures.valid()), "diego");

        assertThat(result.summary().status()).isEqualTo(ChecklistStatus.SUBMITTED);
        assertThat(result.summary().version()).isEqualTo(1);
        verify(repository).saveAndFlush(any());
        verify(versions).save(any());
        verify(events).save(any());
        verify(notifier).submitted(any());
    }

    @Test
    @DisplayName("o mesmo envio de novo (a resposta se perdeu): devolve o que tem, sem gravar nem avisar")
    void replayIsIdempotent() {
        Checklist existing = Checklist.submit(ID, "diego", "Diego", ChecklistFixtures.valid(), false, null,
                LocalDateTime.now(clock));
        when(repository.findById(ID)).thenReturn(Optional.of(existing));

        ChecklistDetailDTO result = service.submit(ID, dto(1, ChecklistFixtures.valid()), "diego");

        assertThat(result.summary().version()).isEqualTo(1);
        verify(repository, never()).save(any());
        verify(repository, never()).saveAndFlush(any());
        verify(versions, never()).save(any());
        verify(notifier, never()).submitted(any());
    }

    @Test
    @DisplayName("id de checklist de outro vendedor é 404, e não 'não é seu'")
    void otherSellersChecklist() {
        Checklist existing = Checklist.submit(ID, "ana", "Ana", ChecklistFixtures.valid(), false, null,
                LocalDateTime.now(clock));
        when(repository.findById(ID)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.submit(ID, dto(1, ChecklistFixtures.valid()), "diego"))
                .isInstanceOf(ChecklistNotFoundException.class);
        assertThatThrownBy(() -> service.detail(ID, "diego", false)).isInstanceOf(ChecklistNotFoundException.class);
        // A Controladoria vê qualquer um.
        assertThat(service.detail(ID, "fernanda", true).summary().sellerLogin()).isEqualTo("ana");
    }

    @Test
    @DisplayName("reenvio depois da devolução: versão 2, e o que mudou vai para o histórico")
    void resubmitRecordsChanges() {
        Checklist existing = Checklist.submit(ID, "diego", "Diego", ChecklistFixtures.valid(), false, null,
                LocalDateTime.now(clock));
        existing.returnToSeller("fernanda", "CPF errado", LocalDateTime.now(clock));
        when(repository.findById(ID)).thenReturn(Optional.of(existing));

        var c = ChecklistFixtures.customer();
        var fixed = ChecklistFixtures.withCustomer(ChecklistFixtures.valid(), new ChecklistContent.Customer(
                c.code(), false, c.name(), c.legalName(), c.document(), c.stateRegistration(), c.mainPhone(),
                c.mobile(), "João da Silva", c.signatoryCpf(), c.invoiceEmail(), c.contractEmail(), c.priceTable(), c.erp()));

        ChecklistDetailDTO result = service.submit(ID, dto(2, fixed), "diego");

        assertThat(result.summary().version()).isEqualTo(2);
        assertThat(result.summary().status()).isEqualTo(ChecklistStatus.SUBMITTED);
        ArgumentCaptor<ChecklistChange> saved = ArgumentCaptor.forClass(ChecklistChange.class);
        verify(changes).save(saved.capture());
        assertThat(saved.getValue().getField()).isEqualTo("Cliente e contrato › Quem assina o contrato");
        assertThat(saved.getValue().getOldValue()).isEqualTo("Maria Aparecida Souza");
        assertThat(saved.getValue().getNewValue()).isEqualTo("João da Silva");
        assertThat(saved.getValue().getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("versão pulada ou checklist desconhecido com versão > 1: 409, nada gravado")
    void revisionMismatch() {
        when(repository.findById(ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.submit(ID, dto(2, ChecklistFixtures.valid()), "diego"))
                .isInstanceOf(ChecklistTransitionException.class);

        Checklist existing = Checklist.submit(ID, "diego", "Diego", ChecklistFixtures.valid(), false, null,
                LocalDateTime.now(clock));
        when(repository.findById(ID)).thenReturn(Optional.of(existing));
        assertThatThrownBy(() -> service.submit(ID, dto(3, ChecklistFixtures.valid()), "diego"))
                .isInstanceOf(ChecklistTransitionException.class);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("diferença do Sankhya: bairro mudou aparece; caixa e acento não contam")
    void erpDifferences() {
        // O retrato do ERP do fixture: nome e cidade em maiúsculas (iguais), bairro VILA INDUSTRIAL.
        var differences = ChecklistService.erpDifferences(ChecklistFixtures.valid());

        assertThat(differences).containsExactly(
                new ChecklistDetailDTO.ErpDifference("Bairro", "VILA INDUSTRIAL", "Centro"));
    }

    @Test
    @DisplayName("cliente novo não tem diferença do Sankhya")
    void newCustomerHasNoErp() {
        var c = ChecklistFixtures.customer();
        var fresh = ChecklistFixtures.withCustomer(ChecklistFixtures.valid(), new ChecklistContent.Customer(
                null, true, c.name(), null, c.document(), c.stateRegistration(), c.mainPhone(), c.mobile(),
                c.signatory(), c.signatoryCpf(), c.invoiceEmail(), c.contractEmail(), null, Map.of("district", "X")));

        assertThat(ChecklistService.erpDifferences(fresh)).isEmpty();
    }

    @Test
    @DisplayName("responder à Controladoria avisa o vendedor, com o motivo mesmo que tenha %")
    void answerNotifiesSeller() {
        Checklist existing = Checklist.submit(ID, "diego", "Diego", ChecklistFixtures.valid(), false, null,
                LocalDateTime.now(clock));
        when(repository.findById(ID)).thenReturn(Optional.of(existing));

        service.returnToSeller(ID, "Desconto de 10% não autorizado", "fernanda");

        verify(notifier).answered(eq(existing), eq("Checklist devolvido para correção"),
                contains("Desconto de 10% não autorizado"));
        verify(events).save(any());
        assertThat(existing.getStatus()).isEqualTo(ChecklistStatus.RETURNED);
    }

    @Test
    @DisplayName("lista da Controladoria filtra por situação")
    void listAllByStatus() {
        when(repository.findByStatusInOrderByLastSubmittedAtDesc(List.of(ChecklistStatus.SUBMITTED))).thenReturn(List.of());
        service.listAll(List.of(ChecklistStatus.SUBMITTED));
        verify(repository).findByStatusInOrderByLastSubmittedAtDesc(List.of(ChecklistStatus.SUBMITTED));
        verify(repository, never()).findAllByOrderByLastSubmittedAtDesc();
    }
}
