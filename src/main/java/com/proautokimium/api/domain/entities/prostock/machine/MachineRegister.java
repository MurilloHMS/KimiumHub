package com.proautokimium.api.domain.entities.prostock.machine;

import com.proautokimium.api.domain.enums.MachineStatus;
import com.proautokimium.api.domain.enums.MachineType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "machine_registers")
public class MachineRegister extends com.proautokimium.api.domain.abstractions.Entity{
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;
    @Column(name = "nome_cliente", length = 200, nullable = false)
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

    public MachineRegister(Machine machine, String nomeCliente, String solicitante, MachineStatus status, String observacao, LocalDateTime previsaoEntrega, String tecnico) {
        this.machine = machine;
        this.nomeCliente = nomeCliente;
        this.solicitante = solicitante;
        this.status = status;
        this.observacao = observacao;
        this.previsaoEntrega = previsaoEntrega;
        this.tecnico = tecnico;
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
}
