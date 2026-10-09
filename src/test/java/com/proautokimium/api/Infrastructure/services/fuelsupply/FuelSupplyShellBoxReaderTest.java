package com.proautokimium.api.Infrastructure.services.fuelsupply;

import com.proautokimium.api.domain.entities.FuelSupply;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * O arquivo real do Shell Box (agosto, nomes trocados) passando pelo leitor.
 *
 * <p>Rede de segurança da F0: os passos 4 a 6 trocam os tipos da entidade
 * ({@code double} → {@code BigDecimal}) e a leitura da data-hora. Se alguma
 * dessas mudanças fizer o leitor perder linha ou trocar campo, este teste
 * fica vermelho.
 */
class FuelSupplyShellBoxReaderTest {

    private final FuelSupplyReaderService reader = new FuelSupplyReaderService();

    private List<FuelSupply> readAugust() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/fuelsupply/shellbox-agosto-anonimizado.xlsx")) {
            // getResourceAsStream devolve null em silêncio quando o arquivo não existe;
            // sem esta linha, o erro aparece como um NullPointerException dentro do leitor.
            assertThat(stream).as("fixture não encontrada em src/test/resources").isNotNull();
            return reader.getDataByExcel(stream);
        }
    }

    @Test
    @DisplayName("lê as 246 linhas de agosto, sem contar o cabeçalho")
    void readsEveryRow() throws Exception {
        List<FuelSupply> rows = readAugust();

        assertThat(rows).hasSize(246);
    }

    @Test
    @DisplayName("a primeira linha chega no campo certo da entidade")
    void firstRowLandsInTheRightFields() throws Exception {
        FuelSupply first = readAugust().getFirst();

        assertThat(first.getDriverName()).isEqualTo("Condutor 01");
        assertThat(first.getFuelSupplyDate())
                .as("a data vem em texto com hora (\"28/08/2026 15:42:56\"); hoje só o dia é guardado")
                .isEqualTo(LocalDate.of(2026, 8, 28));
        assertThat(first.getUf()).isEqualTo("PR");
        assertThat(first.getPlate()).isEqualTo("TEB1B85");
        assertThat(first.getActualHodometer()).isEqualTo(56592);
        assertThat(first.getFuelType()).isEqualTo("Gasolina Comum");
        assertThat(first.getLiters()).isEqualTo(27.39);
        assertThat(first.getTotalValue()).isEqualTo(191.46);
        assertThat(first.getPrice()).isEqualTo(6.99);
        assertThat(first.getDiferenceHodometer())
                .as("KM rodado: o Shell Box calcula, nós só guardamos para conferência")
                .isEqualTo(422);
        assertThat(first.getAverageKm())
                .as("KM/Litro: o Shell Box manda com 13 casas; três bastam para provar a coluna")
                .isCloseTo(15.407, within(0.001));
    }

    @Test
    @DisplayName("a última linha também é lida, com os campos dela")
    void lastRowIsAlsoRead() throws Exception {
        FuelSupply last = readAugust().getLast();

        // Se o leitor parasse antes do fim, a primeira linha continuaria certa
        // e só este teste perceberia qual linha sumiu.
        assertThat(last.getDriverName()).isEqualTo("Condutor 34");
        assertThat(last.getFuelSupplyDate()).isEqualTo(LocalDate.of(2026, 8, 13));
        assertThat(last.getUf()).isEqualTo("RJ");
        assertThat(last.getPlate()).isEqualTo("GGB4A54");
        assertThat(last.getActualHodometer()).isEqualTo(90269);
        assertThat(last.getFuelType()).isEqualTo("Diesel S-10 Comum");
        assertThat(last.getLiters()).isEqualTo(47.37);
        assertThat(last.getTotalValue()).isEqualTo(350.06);
    }

    /**
     * "Total do abastecimento" e "Valor bomba" são iguais em 245 das 246 linhas.
     * Esta é a única diferente (linha 32 do Excel: total 235,61, bomba 249,49):
     * se o leitor pegar a coluna errada, só ela mostra.
     */
    @Test
    @DisplayName("o total vem de \"Total do abastecimento\", não de \"Valor bomba\"")
    void totalComesFromTheRightColumn() throws Exception {
        FuelSupply row = readAugust().get(30);   // linha 32 do Excel: menos o cabeçalho, menos 1 do índice

        assertThat(row.getPlate()).as("conferindo que é a linha certa").isEqualTo("CUQ5I50");
        assertThat(row.getTotalValue()).isEqualTo(235.61);
    }
}
