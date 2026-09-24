package com.proautokimium.api.Infrastructure.abstractions.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalInt;

/**
 * O cabeçalho da planilha, para achar a coluna <b>pelo nome</b>.
 *
 * <p><b>Por que isto existe.</b> Os leitores casavam coluna por posição
 * ({@code getString(row, 3)}), e a posição é do arquivo, não nossa. Em
 * 2026-09-24 a planilha da fornecedora chegou sem a coluna <i>Cidade</i>, que o
 * modelo tem: da terceira coluna em diante tudo andou uma casa, a UF passou a
 * ler a placa, a placa passou a ler o hodômetro, e o erro que apareceu na tela
 * foi <i>For input string: "Gasolina Comum"</i> — que não diz nem qual coluna
 * nem qual linha.
 *
 * <p>E esse foi o caso <b>sortudo</b>: o desalinhamento estourou porque caiu
 * texto onde se esperava número. Uma coluna a mais entre duas numéricas teria
 * importado o mês inteiro trocado, em silêncio.
 *
 * <p>Nome tem variação — a mesma coluna é "UF" no nosso modelo e "Estado do
 * posto" no arquivo da fornecedora —, então quem procura passa os apelidos que
 * conhece, na ordem de preferência.
 */
public final class SheetHeader {

    private final Map<String, Integer> indices;

    private SheetHeader(Map<String, Integer> indices) {
        this.indices = indices;
    }

    /** O cabeçalho de uma planilha que não tem linha de cabeçalho. */
    public static SheetHeader vazio() {
        return new SheetHeader(Map.of());
    }

    public static SheetHeader de(Row header) {
        if (header == null) {
            return vazio();
        }

        Map<String, Integer> indices = new LinkedHashMap<>();

        for (int i = 0; i < header.getLastCellNum(); i++) {
            Cell cell = header.getCell(i);
            if (cell == null) {
                continue;
            }

            String nome = normalizar(cell.toString());

            // A primeira ocorrência ganha: planilha com duas colunas de mesmo
            // nome é erro de quem montou, e apontar para a primeira é menos
            // surpreendente do que apontar para a última.
            if (!nome.isEmpty()) {
                indices.putIfAbsent(nome, i);
            }
        }

        return new SheetHeader(new HashMap<>(indices));
    }

    public boolean vazioDeColunas() {
        return indices.isEmpty();
    }

    /** O índice do primeiro apelido que existir na planilha. */
    public OptionalInt procurar(String... apelidos) {
        for (String apelido : apelidos) {
            Integer indice = indices.get(normalizar(apelido));
            if (indice != null) {
                return OptionalInt.of(indice);
            }
        }
        return OptionalInt.empty();
    }

    /**
     * O índice da coluna, ou uma recusa que diz o que faltou.
     *
     * <p>A mensagem sai para a tela inteira: ela nomeia a coluna que o sistema
     * procurou e lista o que a planilha tem, porque quem está olhando o arquivo
     * precisa saber qual das duas coisas corrigir.
     */
    public int exigir(String campo, String... apelidos) {
        return procurar(apelidos).orElseThrow(() -> new IllegalArgumentException(
                "A planilha não tem a coluna de " + campo + ". "
                        + "Esperava uma com o título \"" + apelidos[0] + "\". "
                        + "As colunas do arquivo são: " + String.join(", ", indices.keySet()) + "."));
    }

    /**
     * Minúsculas, sem acento e com um espaço só entre palavras.
     *
     * <p>"Tipo de Combustível" e "tipo de  combustivel" são a mesma coluna, e
     * planilha que passou por três pessoas tem as duas grafias.
     */
    private static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }

        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        return semAcento.toLowerCase().trim().replaceAll("\\s+", " ");
    }
}
