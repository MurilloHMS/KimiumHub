package com.proautokimium.api.Infrastructure.services.reports.guide;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.proautokimium.api.Application.DTOs.guide.GuideLayoutDTO;
import com.proautokimium.api.Infrastructure.exceptions.guide.GuideLayoutNotFoundException;
import com.proautokimium.api.Infrastructure.exceptions.guide.InvalidGuideLayoutException;
import com.proautokimium.api.Infrastructure.repositories.guide.GuideLayoutImageRepository;
import com.proautokimium.api.Infrastructure.repositories.guide.GuideLayoutRepository;
import com.proautokimium.api.domain.entities.guide.GuideLayout;
import com.proautokimium.api.domain.enums.guide.GuideLayoutStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A regra que sustenta o editor: Contratos só vê o que foi publicado.
 */
class GuideLayoutServiceTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-02T12:00:00Z"), SP);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    private final GuideLayoutRepository repository = mock(GuideLayoutRepository.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private GuideLayoutService service;

    @BeforeEach
    void setUp() {
        service = new GuideLayoutService(repository, mock(GuideLayoutImageRepository.class),
                new GuideLayoutValidator(), mapper, CLOCK);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("salvar sem rascunho cria um rascunho — o publicado não é tocado")
    void salvarCriaRascunho() throws Exception {
        when(repository.findFirstByStatus(GuideLayoutStatus.DRAFT)).thenReturn(Optional.empty());

        GuideLayoutDTO saved = service.saveDraft(seed(), "designer");

        assertThat(saved.status()).isEqualTo(GuideLayoutStatus.DRAFT);
        assertThat(saved.version()).isNull();
        assertThat(saved.updatedBy()).isEqualTo("designer");
        verify(repository, never()).findFirstByStatus(GuideLayoutStatus.PUBLISHED);
    }

    @Test
    @DisplayName("layout inválido não é salvo, e a mensagem diz onde está o erro")
    void invalidoNaoSalva() throws Exception {
        ObjectNode broken = (ObjectNode) seed();
        ((ObjectNode) broken.get("table").get("columns").get(0)).put("width", 400);

        assertThatThrownBy(() -> service.saveDraft(broken, "designer"))
                .isInstanceOf(InvalidGuideLayoutException.class)
                .hasMessageContaining("As colunas somam");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("publicar arquiva a versão em uso ANTES de publicar o rascunho, e numera a seguinte")
    void publicaNaOrdem() throws Exception {
        GuideLayout current = published(3);
        GuideLayout draft = GuideLayout.draft(mapper.writeValueAsString(seed()), "designer", NOW);
        when(repository.findFirstByStatus(GuideLayoutStatus.DRAFT)).thenReturn(Optional.of(draft));
        when(repository.findFirstByStatus(GuideLayoutStatus.PUBLISHED)).thenReturn(Optional.of(current));
        when(repository.findMaxVersion()).thenReturn(5);

        GuideLayoutDTO result = service.publish("  tirei a concentração  ", "designer");

        // O índice parcial aceita um PUBLISHED só: o antigo precisa sair
        // (com flush) antes de o novo entrar.
        InOrder order = inOrder(repository);
        order.verify(repository).saveAndFlush(current);
        order.verify(repository).save(draft);
        assertThat(current.getStatus()).isEqualTo(GuideLayoutStatus.ARCHIVED);
        assertThat(result.status()).isEqualTo(GuideLayoutStatus.PUBLISHED);
        assertThat(result.version()).as("o maior número já usado + 1, não o da versão em uso + 1").isEqualTo(6);
        assertThat(result.note()).isEqualTo("tirei a concentração");
    }

    @Test
    @DisplayName("publicar sem rascunho é 404, e nada muda")
    void publicarSemRascunho() {
        when(repository.findFirstByStatus(GuideLayoutStatus.DRAFT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publish(null, "designer"))
                .isInstanceOf(GuideLayoutNotFoundException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("restaurar uma versão antiga cria um RASCUNHO com o documento dela — não publica")
    void restaurarNaoPublica() {
        GuideLayout old = published(2);
        old.archive();
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(old));
        when(repository.findFirstByStatus(GuideLayoutStatus.DRAFT)).thenReturn(Optional.empty());

        GuideLayoutDTO restored = service.restore(id, "designer");

        assertThat(restored.status()).isEqualTo(GuideLayoutStatus.DRAFT);
        assertThat(restored.document()).isEqualTo(old.getDocument());
        assertThat(old.getStatus()).isEqualTo(GuideLayoutStatus.ARCHIVED);
        verify(repository, never()).saveAndFlush(any());
    }

    private JsonNode seed() throws Exception {
        return mapper.readTree(GuideLayoutSeed.text());
    }

    private GuideLayout published(int version) {
        GuideLayout layout = GuideLayout.draft(GuideLayoutSeed.text(), "designer", NOW);
        layout.publish(version, null, "designer", NOW);
        return layout;
    }
}
