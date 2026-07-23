package com.proautokimium.api.Infrastructure.services.humanResources;

import com.proautokimium.api.Infrastructure.exceptions.humanResources.PositionLevelNotFoundException;
import com.proautokimium.api.Infrastructure.repositories.humanResources.PositionLevelRepository;
import com.proautokimium.api.domain.entities.humanResources.Position;
import com.proautokimium.api.domain.entities.humanResources.PositionLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PositionLevelSalaryResolverTest {

    @Mock
    private PositionLevelRepository positionLevelRepository;

    private PositionLevelSalaryResolver resolver;
    private Position position;

    @BeforeEach
    void setUp() {
        resolver = new PositionLevelSalaryResolver(
                positionLevelRepository,
                new FixedSalaryStrategy(),
                new PercentageSalaryStrategy()
        );
        position = new Position();
        position.setName("Desenvolvedor Java");
    }

    @Test
    @DisplayName("Nível FIXED resolve direto para o próprio valor fixo")
    void deveResolverNivelFixo() {
        PositionLevel junior = PositionLevel.fixed("Júnior", 1, position, new BigDecimal("1690.00"));

        BigDecimal resolved = resolver.resolve(junior);

        assertThat(resolved).isEqualByComparingTo("1690.00");
    }

    @Test
    @DisplayName("Nível PERCENTAGE resolve compondo sobre o nível anterior")
    void deveResolverNivelPercentualSobreAnterior() {
        PositionLevel junior = PositionLevel.fixed("Júnior", 1, position, new BigDecimal("1690.00"));
        PositionLevel pleno = PositionLevel.percentage("Pleno", 2, position, new BigDecimal("5"));

        when(positionLevelRepository.findByPositionAndLevelOrder(any(Position.class), eq(1)))
                .thenReturn(Optional.of(junior));

        BigDecimal resolved = resolver.resolve(pleno);

        assertThat(resolved).isEqualByComparingTo("1774.50");
    }

    @Test
    @DisplayName("Percentuais em cadeia compõem sobre o nível imediatamente anterior, não sobre a base")
    void deveComporPercentuaisEmCadeia() {
        PositionLevel junior = PositionLevel.fixed("Júnior", 1, position, new BigDecimal("1000.00"));
        PositionLevel pleno = PositionLevel.percentage("Pleno", 2, position, new BigDecimal("10"));
        PositionLevel senior = PositionLevel.percentage("Sênior", 3, position, new BigDecimal("10"));

        when(positionLevelRepository.findByPositionAndLevelOrder(any(Position.class), eq(1)))
                .thenReturn(Optional.of(junior));
        when(positionLevelRepository.findByPositionAndLevelOrder(any(Position.class), eq(2)))
                .thenReturn(Optional.of(pleno));

        BigDecimal resolved = resolver.resolve(senior);

        // 1000 * 1.10 * 1.10 = 1210.00 — se fosse sobre a base, seria 1000 * 1.20 = 1200.00 (errado)
        assertThat(resolved).isEqualByComparingTo("1210.00");
    }

    @Test
    @DisplayName("Nível PERCENTAGE sem nível anterior cadastrado lança PositionLevelNotFoundException")
    void deveLancarExcecaoQuandoNivelAnteriorNaoExiste() {
        PositionLevel pleno = PositionLevel.percentage("Pleno", 2, position, new BigDecimal("5"));

        when(positionLevelRepository.findByPositionAndLevelOrder(any(Position.class), eq(1)))
                .thenReturn(Optional.empty());

        assertThrows(PositionLevelNotFoundException.class, () -> resolver.resolve(pleno));
    }
}
