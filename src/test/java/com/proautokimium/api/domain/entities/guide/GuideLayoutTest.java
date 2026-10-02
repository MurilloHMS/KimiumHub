package com.proautokimium.api.domain.entities.guide;

import com.proautokimium.api.domain.enums.guide.GuideLayoutStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O ciclo rascunho → publicado → arquivado. O que foi publicado é registro:
 * se desse para alterar, "voltar para a v6" devolveria uma v6 que não é a
 * que Contratos usou.
 */
class GuideLayoutTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 2, 9, 0);

    @Test
    @DisplayName("rascunho nasce sem número e vira publicado com número, nota e autor")
    void publicaRascunho() {
        GuideLayout layout = GuideLayout.draft("{}", "designer", AGORA);
        assertThat(layout.getStatus()).isEqualTo(GuideLayoutStatus.DRAFT);
        assertThat(layout.getVersion()).isNull();

        layout.publish(8, "tirei a concentração", "designer", AGORA.plusHours(1));

        assertThat(layout.getStatus()).isEqualTo(GuideLayoutStatus.PUBLISHED);
        assertThat(layout.getVersion()).isEqualTo(8);
        assertThat(layout.getNote()).isEqualTo("tirei a concentração");
        assertThat(layout.getPublishedBy()).isEqualTo("designer");
        assertThat(layout.getPublishedAt()).isEqualTo(AGORA.plusHours(1));
    }

    @Test
    @DisplayName("o publicado não aceita mudar o conteúdo, nem ser publicado de novo")
    void publicadoEhRegistro() {
        GuideLayout layout = GuideLayout.draft("{\"a\":1}", "designer", AGORA);
        layout.publish(1, null, "designer", AGORA);

        assertThatThrownBy(() -> layout.updateDraft("{\"a\":2}", "designer", AGORA))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> layout.publish(2, null, "designer", AGORA))
                .isInstanceOf(IllegalStateException.class);
        assertThat(layout.getDocument()).isEqualTo("{\"a\":1}");
        assertThat(layout.getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("só o publicado vai para o arquivo; rascunho e arquivado recusam")
    void soPublicadoArquiva() {
        GuideLayout draft = GuideLayout.draft("{}", "designer", AGORA);
        assertThatThrownBy(draft::archive).isInstanceOf(IllegalStateException.class);
        assertThat(draft.getStatus()).isEqualTo(GuideLayoutStatus.DRAFT);

        GuideLayout published = GuideLayout.draft("{}", "designer", AGORA);
        published.publish(1, null, "designer", AGORA);
        published.archive();
        assertThat(published.getStatus()).isEqualTo(GuideLayoutStatus.ARCHIVED);
        assertThatThrownBy(published::archive).isInstanceOf(IllegalStateException.class);
    }
}
