package com.proautokimium.api.domain.entities;

import java.time.LocalDate;

import com.proautokimium.api.Application.DTOs.fuelsupply.FuelSupplyDTO;
import com.proautokimium.api.domain.abstractions.Entity;
import com.proautokimium.api.domain.entities.humanResources.Department;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
	
    /**
     * O departamento do abastecimento, agora ligado ao cadastro.
     *
     * Era enum numa coluna de texto. O funcionario deixou de ter departamento
     * proprio — quem decide e o SETOR dele, que pertence a um departamento —,
     * entao o abastecimento passa a apontar para a mesma linha que todo o
     * resto do sistema enxerga.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
	private Department department;
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
        // `department` NAO sai do DTO: la viaja o nome, e resolver nome para
        // linha do cadastro precisa de repositorio, que entidade nao tem.
        // Quem monta um FuelSupply por este caminho preenche depois.
        this.actualHodometer = dto.actualHodometer();
        this.lastHodometer = dto.lastHodometer();
        this.diferenceHodometer = dto.diferenceHodometer();
        this.averageKm = dto.averageKm();
        this.fuelType = dto.fuelType();
        this.liters = dto.liters();
        this.price = dto.price();
        this.totalValue = dto.totalValue();
    }

    public String getDepartmentName() {
        return department != null ? department.getName() : "";
    }

    public FuelSupplyDTO toDto(){
        return new FuelSupplyDTO(
                this.fuelSupplyDate,
                this.uf,
                this.plate,
                this.driverName,
                getDepartmentName(),
                this.actualHodometer,
                this.lastHodometer,
                this.diferenceHodometer,
                this.averageKm,
                this.fuelType,
                this.liters,
                this.price,
                this.totalValue
        );
    }
}
