package com.proautokimium.api.Application.DTOs.contact;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.proautokimium.api.domain.enums.ContactStatus;
import com.proautokimium.api.domain.enums.ContactType;

import java.time.LocalDateTime;

public record ContactDTO(String name, String email, ContactType contactType, String other, String message, String businessName, ContactStatus contactStatus,@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS") LocalDateTime contactDate) {}
