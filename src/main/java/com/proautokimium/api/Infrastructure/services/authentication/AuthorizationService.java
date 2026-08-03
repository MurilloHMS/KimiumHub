package com.proautokimium.api.Infrastructure.services.authentication;

import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.valueObjects.Email;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class AuthorizationService implements UserDetailsService {
    @Autowired
    UserRepository repository;

    @Autowired
    EmployeeRepository employeeRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return getUserByIdentifier(username);
    }


    protected User getUserByIdentifier(String identifier){
        if(isEmail(identifier)){
            return (User) repository.findByEmail(identifier);
        }

        if(isCPF(identifier)){
            User user = null;
            String digits = identifier.replaceAll("[^0-9]", "");
            Optional<Employee> employee = employeeRepository.findByCpfDigits(digits);
            if(employee.isPresent()){
                user = repository.findByEmployee_Id(employee.get().getId()).orElse(null);
            }
            return user;
        }

        return (User) repository.findByLogin(identifier);
    }

    protected boolean isEmail(String email){
        return Email.isValid(email);
    }

    protected boolean isCPF(String cpf){
        Pattern documentPattern = Pattern.compile("[0-9.-]+");
        return documentPattern.matcher(cpf).matches();
    }
}
