package com.proautokimium.api.Application.DTOs.product;

public record ProductInventoryDTO(String systemCode,
                                  String name,
                                  boolean active,
                                  int minimumStock) {}
