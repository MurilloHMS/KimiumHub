package com.proautokimium.api.Infrastructure.services.fuelsupply;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.proautokimium.api.Application.DTOs.fuelsupply.FuelSupplyDTO;
import com.proautokimium.api.Infrastructure.repositories.FuelSupplyRepository;
import com.proautokimium.api.domain.entities.FuelSupply;

import jakarta.transaction.Transactional;

@Service
public class FuelSupplyService {

	@Autowired
	FuelSupplyRepository repository;

	@Autowired
	FuelSupplyWriterService writer;

	@Transactional
	public ResponseEntity<?> createFuelSupply(FuelSupplyDTO dto) {

		try {
			FuelSupply fs = new FuelSupply(dto);
			repository.save(fs);
			return ResponseEntity.ok().body("Abastecimento Criado com sucesso!");
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.internalServerError()
					.body("Ocorreu um erro ao criar o abastecimento. Error: " + e.getMessage());
		}
	}

	public List<FuelSupplyDTO> listByPeriod(LocalDate start, LocalDate end) {
		return repository.findByFuelSupplyDateBetween(start, end)
				.stream()
				.map(FuelSupply::toDto)
				.toList();
	}

	/**
	 * Os abastecimentos do periodo numa planilha, no mesmo formato do modelo.
	 *
	 * <p>Sai importavel de proposito: exportar, corrigir uma linha no Excel e
	 * reenviar e o caminho de conserto quando o erro so aparece depois de
	 * gravado.
	 */
	public byte[] exportByPeriod(LocalDate start, LocalDate end) throws Exception {
		return writer.write(repository.findByFuelSupplyDateBetween(start, end));
	}
}
