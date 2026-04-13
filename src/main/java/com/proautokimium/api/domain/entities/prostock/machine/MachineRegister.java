package com.proautokimium.api.domain.entities.prostock.machine;

import com.proautokimium.api.domain.enums.MachineStatus;
import com.proautokimium.api.domain.enums.MachineType;

import java.time.LocalDateTime;

public class MachineRegister {
    private Machine machine;
    private String nomeCliente;
    private String solicitante;
    private MachineStatus status;
    private String observacao;
    private LocalDateTime previsaoEntrega;
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
