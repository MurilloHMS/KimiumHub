package com.proautokimium.api.Infrastructure.services.fuelsupply;

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
	
	@Transactional
	public ResponseEntity<?> createFuelSupply(FuelSupplyDTO dto) {
		
		try {
			FuelSupply fs = new FuelSupply();
			fs.setActualHodometer(dto.actualHodometer());
			fs.setAverageKm(dto.averageKm());
			fs.setDepartment(dto.department());
			fs.setDiferenceHodometer(dto.diferenceHodometer());
			fs.setDriverName(dto.driverName());
			fs.setFuelSupplyDate(dto.fuelSupplyDate());
			fs.setFuelType(dto.fuelType());
			fs.setLastHodometer(dto.lastHodometer());
			fs.setPlate(dto.plate());
			fs.setPrice(dto.price());
			fs.setTotalValue(dto.totalValue());
			
			repository.save(fs);
			return ResponseEntity.ok().body("Abastecimento Criado com sucesso!");
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.internalServerError()
					.body("Ocorreu um erro ao criar o abastecimento. Error: " + e.getMessage());
		}
	}
	
	@Transactional
	public ResponseEntity<?> insertByRange(List<FuelSupply> fuelList){
		try {		
			repository.saveAll(fuelList);
	        return ResponseEntity.ok("Abastecimentos inseridos com sucesso!");
	    } catch (Exception e) {
	        return ResponseEntity.internalServerError()
	            .body("Erro ao inserir abastecimentos: " + e.getMessage());
	    }
	}
}
