package com.proautokimium.api.Application.DTOs.machine;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record MachineAlertConfigDTO(
        boolean active,
        List<Integer> daysBefore,
        boolean alertWhenLate,
        @JsonFormat(pattern = "HH:mm") LocalTime sendAt,
        List<UUID> recipientEmployeeIds
) { }