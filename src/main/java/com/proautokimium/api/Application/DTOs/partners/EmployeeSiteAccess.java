package com.proautokimium.api.Application.DTOs.partners;

import com.proautokimium.api.domain.enums.SiteAccess;

import java.time.LocalDateTime;

public record EmployeeSiteAccess(SiteAccess status, String login, LocalDateTime firstAccessRequestedAt) {
}
