package com.proautokimium.api.Infrastructure.services.fuelsupply;

import com.proautokimium.api.Infrastructure.abstractions.excel.ExcelReader;
import com.proautokimium.api.Infrastructure.abstractions.excel.SheetHeader;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Service;

import com.proautokimium.api.domain.entities.FuelSupply;

import java.util.OptionalInt;

/**
 * A planilha de abastecimentos virando entidade.
 *
 * <p><b>As colunas são achadas pelo nome do cabeçalho, não pela posição.</b>
 * Até 2026-09-24 era por posição, e a planilha da fornecedora chegou sem a
 * coluna <i>Cidade</i>: da terceira em diante tudo andou uma casa, a UF passou
 * a ler a placa, a placa passou a ler o hodômetro, e o combustível caiu onde se
 * esperava número — <i>For input string: "Gasolina Comum"</i>.
 *
 * <p>A mesma coluna tem nomes diferentes nos dois arquivos que circulam aqui —
 * o nosso modelo diz "UF", o da fornecedora diz "Estado do posto" —, então cada
 * campo lista os apelidos que conhece, <b>na ordem de preferência</b>. O
 * primeiro que existir na planilha ganha.
 */
@Service
public class FuelSupplyReaderService extends ExcelReader<FuelSupply> {

	@Override
	protected int getFirstDataRow(){
		return 1;
	}

	@Override
	protected FuelSupply mapRow(Row row, SheetHeader header) {
		FuelSupply fuel = new FuelSupply();

		fuel.setDriverName(getString(row, header.exigir(
				"motorista", "Nome do Motorista", "Nome do condutor", "Motorista", "Condutor")));

		fuel.setFuelSupplyDate(getDate(row, header.exigir(
				"data do abastecimento", "Data de Abastecimento",
				"Data/Hora transação (fim do abastecimento)", "Data/Hora transação", "Data")));

		fuel.setUf(getString(row, header.exigir("UF", "UF", "Estado do posto", "Estado")));

		fuel.setPlate(getString(row, header.exigir("placa", "Placa")));

		fuel.setActualHodometer(numero(row, header.exigir(
				"hodômetro", "Hodometro Atual", "Hodômetro", "Hodometro")));

		fuel.setFuelType(getString(row, header.exigir(
				"tipo de combustível", "Tipo de Combustível", "Combustível")));

		fuel.setLiters(numero(row, header.exigir(
				"litros", "Litros abastecidos", "Litros")));

		fuel.setTotalValue(numero(row, header.exigir(
				"valor total", "Valor Total", "Total do abastecimento")));

		fuel.setPrice(numero(row, header.exigir(
				"valor por litro", "Valor por Litro", "R$/Litro", "Valor unitário")));

		// Estas duas a fornecedora manda calculadas, e nem toda exportação as
		// traz. Faltando, ficam zero: são informativas, e recusar a planilha
		// inteira por causa delas seria desproporcional.
		fuel.setDiferenceHodometer(opcional(row, header,
				"Diferença Hodometro", "KM rodado", "Km rodado"));

		fuel.setAverageKm(opcional(row, header,
				"Média Km/L", "KM/Litro", "Km/Litro"));

		return fuel;
	}

	private double opcional(Row row, SheetHeader header, String... apelidos) {
		OptionalInt coluna = header.procurar(apelidos);

		return coluna.isEmpty() ? 0 : numero(row, coluna.getAsInt());
	}

	/**
	 * Célula numérica vazia vale zero, e não estoura.
	 *
	 * <p>{@code getDouble} devolve {@code null}, e os campos da entidade são
	 * {@code double} primitivo: a conversão automática vira
	 * {@code NullPointerException} e derruba a planilha inteira por uma célula
	 * em branco. Zero aparece na conferência, e quem está olhando decide.
	 */
	private double numero(Row row, int coluna) {
		Double valor = getDouble(row, coluna);
		return valor == null ? 0 : valor;
	}
}
