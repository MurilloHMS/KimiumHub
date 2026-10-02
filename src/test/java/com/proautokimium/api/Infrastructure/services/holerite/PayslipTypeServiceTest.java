package com.proautokimium.api.Infrastructure.services.holerite;

import com.proautokimium.api.Application.DTOs.holerite.PayslipTypeDTOs.CreatePayslipTypeResult;
import com.proautokimium.api.Infrastructure.exceptions.holerite.InvalidPayslipTypeException;
import com.proautokimium.api.Infrastructure.repositories.PayslipTypeRepository;
import com.proautokimium.api.domain.entities.PayslipType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Os tipos de holerite que o RH cria pela tela. O nome repetido não é erro: a
 * tela seleciona o que já existe, e "PLR" e "plr" não viram dois tipos.
 */
class PayslipTypeServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-02T12:00:00Z"), ZoneId.of("America/Sao_Paulo"));

    private final PayslipTypeRepository repository = mock(PayslipTypeRepository.class);
    private final List<PayslipType> cadastro = new ArrayList<>();
    private PayslipTypeService service;

    @BeforeEach
    void setUp() {
        service = new PayslipTypeService(repository, CLOCK);
        cadastro.add(tipo("SALARIO", "Salário", 10));
        cadastro.add(tipo("PLR", "PLR", 50));
        cadastro.add(tipo("FERIAS_COLETIVAS", "Férias coletivas", 60));
        when(repository.findAll()).thenReturn(cadastro);
        when(repository.findMaxSortOrder()).thenReturn(60);
        when(repository.existsByCode(anyString())).thenAnswer(i -> cadastro.stream().anyMatch(t -> t.getCode().equals(i.getArgument(0))));
        when(repository.findByCode(anyString())).thenAnswer(i -> cadastro.stream().filter(t -> t.getCode().equals(i.getArgument(0))).findFirst());
        when(repository.save(any(PayslipType.class))).thenAnswer(i -> i.getArgument(0));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Férias coletivas 2026      | FERIAS_COLETIVAS_2026",
            "13º salário extra          | 13_SALARIO_EXTRA",
            "  Bônus / Prêmio (anual)   | BONUS_PREMIO_ANUAL",
            "Rescisão — complementar    | RESCISAO_COMPLEMENTAR",
    })
    @DisplayName("o código sai do nome: maiúsculas, sem acento, só letras, números e _")
    void codeFromLabel(String label, String code) {
        assertThat(PayslipTypeService.codeOf(label)).isEqualTo(code);
    }

    @Test
    @DisplayName("código longo é cortado em 40, sem terminar em _")
    void longCode() {
        String code = PayslipTypeService.codeOf("Participação nos lucros e resultados do segundo semestre");
        assertThat(code).hasSizeLessThanOrEqualTo(40).doesNotEndWith("_").matches("[A-Z0-9_]+");
    }

    @Test
    @DisplayName("tipo novo é salvo com o nome como digitado, o código gerado e no fim da ordem")
    void createsNew() {
        CreatePayslipTypeResult result = service.create("  Abono   de  férias ", "carla.rh");

        assertThat(result.created()).isTrue();
        assertThat(result.type().code()).isEqualTo("ABONO_DE_FERIAS");
        assertThat(result.type().label()).isEqualTo("Abono de férias");
        ArgumentCaptor<PayslipType> salvo = ArgumentCaptor.forClass(PayslipType.class);
        verify(repository).save(salvo.capture());
        assertThat(salvo.getValue().getSortOrder()).isEqualTo(70);
        assertThat(salvo.getValue().getCreatedBy()).isEqualTo("carla.rh");
        assertThat(salvo.getValue().getCreatedAt()).isEqualTo(LocalDateTime.now(CLOCK));
    }

    @Test
    @DisplayName("nome que já existe, com outra caixa ou sem acento, devolve o existente e não cria")
    void sameNameReturnsExisting() {
        CreatePayslipTypeResult semAcento = service.create("ferias COLETIVAS", "carla.rh");
        CreatePayslipTypeResult minusculo = service.create("plr", "carla.rh");

        assertThat(semAcento.created()).isFalse();
        assertThat(semAcento.type().code()).isEqualTo("FERIAS_COLETIVAS");
        assertThat(minusculo.type().code()).isEqualTo("PLR");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("nomes diferentes que dariam o mesmo código ganham sufixo, em vez de colidir")
    void codeCollision() {
        CreatePayslipTypeResult result = service.create("P.L.R.", "carla.rh");

        assertThat(result.created()).isTrue();
        assertThat(result.type().code()).isEqualTo("P_L_R");
        cadastro.add(tipo("P_L_R", "P.L.R.", 70));
        assertThat(service.create("P L R", "carla.rh").type().code()).isEqualTo("P_L_R_2");
    }

    @Test
    @DisplayName("nome vazio ou sem letra nem número é recusado")
    void invalidName() {
        assertThatThrownBy(() -> service.create("   ", "x")).isInstanceOf(InvalidPayslipTypeException.class);
        assertThatThrownBy(() -> service.create("— / —", "x"))
                .isInstanceOf(InvalidPayslipTypeException.class).hasMessageContaining("letras ou números");
    }

    @Test
    @DisplayName("tipo desconhecido ou desativado é 400 com o código na mensagem; caixa e espaço não importam")
    void requireActive() {
        assertThat(service.requireActive(" plr ").getCode()).isEqualTo("PLR");
        assertThatThrownBy(() -> service.requireActive("BONUS"))
                .isInstanceOf(InvalidPayslipTypeException.class).hasMessageContaining("BONUS");

        PayslipType inativo = tipo("ANTIGO", "Antigo", 5);
        org.springframework.test.util.ReflectionTestUtils.setField(inativo, "active", false);
        cadastro.add(inativo);
        assertThatThrownBy(() -> service.requireActive("ANTIGO")).isInstanceOf(InvalidPayslipTypeException.class);
    }

    private static PayslipType tipo(String code, String label, int order) {
        return new PayslipType(code, label, order, null, LocalDateTime.of(2026, 9, 1, 0, 0));
    }
}
