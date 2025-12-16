package com.proautokimium.api.domain.entities;

import java.time.LocalDate;

import com.proautokimium.api.Application.DTOs.fuelsupply.FuelSupplyDTO;
import com.proautokimium.api.domain.abstractions.Entity;
import com.proautokimium.api.domain.enums.Department;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@jakarta.persistence.Entity
@Table(name = "fuelsupply")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FuelSupply extends Entity{

	private LocalDate fuelSupplyDate;
	private String uf;
	private String plate;
	private String driverName;
	
	@Enumerated(EnumType.STRING)
	private Department department;
	private double actualHodometer;
	private double lastHodometer;
	private double diferenceHodometer;
	private double averageKm;
	private String fuelType;
	private double price;
	private double totalValue;

    public FuelSupply(FuelSupplyDTO dto){
        this.fuelSupplyDate = dto.fuelSupplyDate();
        this.uf = dto.uf();
        this.plate = dto.plate();
        this.driverName = dto.driverName();
        this.department = dto.department();
        this.actualHodometer = dto.actualHodometer();
        this.lastHodometer = dto.lastHodometer();
        this.diferenceHodometer = dto.diferenceHodometer();
        this.averageKm = dto.averageKm();
        this.fuelType = dto.fuelType();
        this.price = dto.price();
        this.totalValue = dto.totalValue();
    }
}
