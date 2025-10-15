package com.proautokimium.api.domain.entities;

import java.time.LocalDate;

import com.proautokimium.api.domain.abstractions.Entity;

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
	private String department;
	private double actualHodometer;
	private double lastHodometer;
	private double diferenceHodometer;
	private double averageKm;
	private String fuelType;
	private double price;
	private double totalValue;
}
