package com.proautokimium.api.domain.entities;

import java.time.LocalDate;

import com.proautokimium.api.Application.DTOs.fuelsupply.FuelSupplyDTO;
import com.proautokimium.api.domain.abstractions.Entity;
import com.proautokimium.api.domain.enums.Department;

import jakarta.persistence.Column;
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

    @Column(name = "fuelsupplydate")
	private LocalDate fuelSupplyDate;
    @Column(name = "uf")
	private String uf;
    @Column(name = "plate")
	private String plate;
    @Column(name = "drivername")
	private String driverName;
	
	@Enumerated(EnumType.STRING)
    @Column(name = "department", nullable = false)
	private Department department = Department.SEM_DEPARTAMENTO;
    @Column(name = "actualhodometer")
	private double actualHodometer;
    @Column(name = "lasthodometer")
	private double lastHodometer;
    @Column(name = "diferencehodometer")
	private double diferenceHodometer;
    @Column(name = "averagekm")
	private double averageKm;
    @Column(name = "fueltype")
	private String fuelType;
    @Column(name = "liters")
    private double liters;
    @Column(name = "price")
	private double price;
    @Column(name = "totalvalue")
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
        this.liters = dto.liters();
        this.price = dto.price();
        this.totalValue = dto.totalValue();
    }
}
