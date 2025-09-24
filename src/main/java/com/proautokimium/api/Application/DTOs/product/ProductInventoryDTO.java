package com.proautokimium.api.Application.DTOs.product;

import java.util.UUID;

public record ProductInventoryDTO(String systemCode,
                                  String name,
                                  boolean active,
                                  int minimumStock) {}
