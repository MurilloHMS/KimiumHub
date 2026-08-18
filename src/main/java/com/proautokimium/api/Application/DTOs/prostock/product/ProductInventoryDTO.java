package com.proautokimium.api.Application.DTOs.prostock.product;

import com.proautokimium.api.domain.enums.MachineStatus;
import com.proautokimium.api.domain.enums.MachineType;

/**
 * Cadastro de produto — e de máquina, que é um produto marcado.
 *
 * Os três últimos campos só valem quando `isMachine` é verdadeiro; nos demais
 * produtos chegam nulos e são gravados assim.
 */
public record ProductInventoryDTO(String systemCode,
                                  String name,
                                  boolean active,
                                  int minimumStock,
                                  boolean isMachine,
                                  String brand,
                                  MachineType machineType,
                                  MachineStatus machineStatus) {}
