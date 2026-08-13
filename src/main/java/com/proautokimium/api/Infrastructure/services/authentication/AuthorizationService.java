package com.proautokimium.api.Infrastructure.services.authentication;

import com.proautokimium.api.Infrastructure.repositories.CustomerRepository;
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

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class AuthorizationService implements UserDetailsService {
    @Autowired
    UserRepository repository;

    @Autowired
    EmployeeRepository employeeRepository;

    @Autowired
    CustomerRepository customerRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return getUserByIdentifier(username);
    }


    protected User getUserByIdentifier(String identifier){
        if(isEmail(identifier)){
            return (User) repository.findByEmail(identifier);
        }

        if(isDocument(identifier)){
            String digits = identifier.replaceAll("[^0-9]", "");

            // 11 dígitos é CPF de funcionário, 14 é CNPJ de cliente. Sem essa
            // separação o CNPJ era procurado entre os CPFs e nunca achava.
            if(digits.length() == 14) return findClientUser(digits);
            if(digits.length() == 11) return findEmployeeUser(digits);

            return null;
        }

        return (User) repository.findByLogin(identifier);
    }

    private User findEmployeeUser(String digits){
        return employeeRepository.findByCpfDigits(digits)
                .flatMap(employee -> repository.findByEmployee_Id(employee.getId()))
                .orElse(null);
    }

    private User findClientUser(String digits){
        return customerRepository.findByCnpjDigits(digits)
                .map(customer -> repository.findByCustomer_Id(customer.getId()))
                .filter(users -> users.size() == 1)
                .map(List::getFirst)
                .orElse(null);
    }

    protected boolean isDocument(String value){
        return Pattern.compile("[0-9./-]+").matcher(value).matches();
    }

    protected boolean isEmail(String email){
        return Email.isValid(email);
    }

    protected boolean isCPF(String cpf){
        Pattern documentPattern = Pattern.compile("[0-9.-]+");
        return documentPattern.matcher(cpf).matches();
    }
}
