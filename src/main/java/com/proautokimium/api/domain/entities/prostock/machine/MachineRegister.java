package com.proautokimium.api.domain.entities.prostock.machine;

import com.proautokimium.api.Application.DTOs.prostock.machine.CreateRegisterDTO;
import com.proautokimium.api.Application.DTOs.prostock.machine.ResponseRegisterDTO;
import com.proautokimium.api.Application.DTOs.prostock.machine.UpdateRegisterDTO;
import com.proautokimium.api.domain.enums.MachineStatus;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "machine_registers")
@EntityListeners(AuditingEntityListener.class)
public class MachineRegister extends com.proautokimium.api.domain.abstractions.Entity{
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;
    @Column(name = "nome_cliente", length = 200)
    private String nomeCliente;
    @Column(name = "tag")
    private short tag;
    @Column(name = "solicitante", length = 100)
    private String solicitante;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MachineStatus status;
    @Column(name = "observacao", length = 500)
    private String observacao;
    @Column(name = "previsao_entrega")
    private LocalDateTime previsaoEntrega;
    @Column(name = "tecnico", length = 100)
    private String tecnico;
    @Column(name = "regiao", length = 100)
    private String regiao;

    @Column(name = "consultor", length = 100)
    private String consultor;

    @CreatedBy
    @Column(name = "created_by", length = 120, updatable = false)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedBy
    @Column(name = "updated_by", length = 120)
    private String updatedBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public MachineRegister(Machine machine,
                           String nomeCliente,
                           String solicitante,
                           MachineStatus status,
                           String observacao,
                           LocalDateTime previsaoEntrega,
                           String tecnico,
                           String regiao,
                           String consultor) {
        this.machine = machine;
        this.nomeCliente = nomeCliente;
        this.solicitante = solicitante;
        this.status = status;
        this.observacao = observacao;
        this.previsaoEntrega = previsaoEntrega;
        this.tecnico = tecnico;
        this.regiao = regiao;
        this.consultor = consultor;
    }

    public MachineRegister(Machine machine){
        this.machine = machine;
    }

    protected MachineRegister() { }

    // Getters and Setters


    public Machine getMachine() {
        return machine;
    }

    public void setMachine(Machine machine) {
        this.machine = machine;
    }

    public String getNomeCliente() {
        return nomeCliente;
    }

    public void setNomeCliente(String nomeCliente) {
        this.nomeCliente = nomeCliente;
    }

    public short getTag(){ return tag; }

    public void setTag(short tag){ this.tag = tag; }

    public String getSolicitante() {
        return solicitante;
    }

    public void setSolicitante(String solicitante) {
        this.solicitante = solicitante;
    }

    public MachineStatus getStatus() {
        return status;
    }

    public void setStatus(MachineStatus status) {
        this.status = status;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public LocalDateTime getPrevisaoEntrega() {
        return previsaoEntrega;
    }

    public void setPrevisaoEntrega(LocalDateTime previsaoEntrega) {
        this.previsaoEntrega = previsaoEntrega;
    }

    public String getTecnico() {
        return tecnico;
    }

    public void setTecnico(String tecnico) {
        this.tecnico = tecnico;
    }

    public String getRegiao() { return regiao; }
    public void setRegiao(String regiao) { this.regiao = regiao; }

    public String getConsultor() { return consultor; }
    public void setConsultor(String consultor) { this.consultor = consultor; }

    public String getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getUpdatedBy() { return updatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Methods

    public void fromDto(CreateRegisterDTO dto){
        this.tag = dto.tag();
        this.nomeCliente = dto.nomeCliente();
        this.observacao = dto.Observacao();
        this.status = dto.status();
        this.previsaoEntrega = dto.previsaoEntrega();
        this.solicitante = dto.solicitante();
        this.tecnico = dto.tecnico();
        this.regiao = dto.regiao();
        this.consultor = dto.consultor();
    }

    public void fromDto(UpdateRegisterDTO dto){
        this.tag = dto.tag();
        this.nomeCliente = dto.nomeCliente();
        this.observacao = dto.Observacao();
        this.status = dto.status();
        this.previsaoEntrega = dto.previsaoEntrega();
        this.solicitante = dto.solicitante();
        this.tecnico = dto.tecnico();
        this.regiao = dto.regiao();
        this.consultor = dto.consultor();
    }

    public ResponseRegisterDTO toDto(){
        return new ResponseRegisterDTO(
                this.id,
                this.machine.getId(),
                this.nomeCliente,
                this.tag,
                this.solicitante,
                this.status,
                this.observacao,
                this.previsaoEntrega,
                this.tecnico,
                this.regiao,
                this.consultor,
                this.createdBy,
                this.createdAt,
                this.updatedBy,
                this.updatedAt
        );
    }
}
