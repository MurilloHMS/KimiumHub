package com.proautokimium.api.Infrastructure.services.partner;

import com.proautokimium.api.Application.DTOs.partners.EmployeeSiteAccess;
import com.proautokimium.api.Infrastructure.repositories.FirstAccessTokenRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.services.authentication.TokenAuthService;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.auth.FirstAccessToken;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.enums.SiteAccess;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class SiteAccessResolver {

    private final UserRepository users;
    private final FirstAccessTokenRepository firstAccessTokens;

    public SiteAccessResolver(UserRepository users, FirstAccessTokenRepository firstAccessTokens) {
        this.users = users;
        this.firstAccessTokens = firstAccessTokens;
    }

    public Map<UUID, EmployeeSiteAccess> resolve(List<Employee> employees){
        Map<UUID, User> accountByEmployee = new HashMap<>();
        for(User user : users.findAllWithEmployee()){
            if(user.getEmployee() != null){
                accountByEmployee.put(user.getEmployee().getId(), user);
            }
        }

        Map<UUID, LocalDateTime> requestByEmployee = new HashMap<>();
        for(FirstAccessToken token : firstAccessTokens.findByUsedFalse()){
            if(token.getPartner() == null || token.getExpiration() == null) continue;

            LocalDateTime requestedAt = token.getExpiration().minusMinutes(TokenAuthService.TOKEN_TTL_MINUTES);
            requestByEmployee.merge(token.getPartner().getId(), requestedAt, (first, second) -> first.isAfter(second) ? first : second);
        }

        Map<UUID, EmployeeSiteAccess> result = new HashMap<>();
        for (Employee employee : employees) {
            User account = accountByEmployee.get(employee.getId());
            if (account != null) {
                SiteAccess status = account.isActive() ? SiteAccess.ACTIVE : SiteAccess.BLOCKED;
                result.put(employee.getId(), new EmployeeSiteAccess(status, account.getLogin(), null));
            } else {
                result.put(employee.getId(),
                        new EmployeeSiteAccess(SiteAccess.PENDING, null, requestByEmployee.get(employee.getId())));
            }
        }
        return result;
    }
}
