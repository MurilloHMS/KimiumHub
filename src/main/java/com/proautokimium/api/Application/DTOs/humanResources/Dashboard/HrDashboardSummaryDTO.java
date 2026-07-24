package com.proautokimium.api.Application.DTOs.humanResources.Dashboard;

import java.math.BigDecimal;
import java.util.List;

public record HrDashboardSummaryDTO(
        List<CompanyEmployeeCountDTO> employeesByCompany,
        List<NameCountDTO> employeesByPosition,
        List<NameCountDTO> employeesByDepartment,
        BigDecimal totalSalaries,
        OrgStructureSummaryDTO orgStructure
) {
}
