package com.proautokimium.api.Infrastructure.services.client;

import com.proautokimium.api.Infrastructure.repositories.CustomerRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.domain.entities.Customer;
import com.proautokimium.api.domain.entities.auth.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ClientAccessService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    public ClientAccessService(UserRepository userRepository, CustomerRepository customerRepository) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
    }

    public Customer currentCustomer() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof User principal))
            throw new AccessDeniedException("Sessão inválida.");

        Customer customer = userRepository.findByLoginWithCustomer(principal.getLogin())
                .map(User::getCustomer)
                .orElse(null);

        if (customer == null)
            throw new AccessDeniedException("Este usuário não é de cliente.");

        if (!customer.isAtivo())
            throw new AccessDeniedException("Cliente inativo.");

        return customer;
    }

    /** As unidades que este login enxerga: a própria, e as do grupo se for matriz. */
    public List<Customer> visibleUnits() {
        Customer customer = currentCustomer();

        if (!customer.isMatriz()) return List.of(customer);

        List<Customer> units = new ArrayList<>();
        units.add(customer);
        units.addAll(customerRepository.findByCodigoMatriz(customer.getCodParceiro())
                .stream()
                .filter(unit -> !unit.getId().equals(customer.getId()))
                .toList());

        return units;
    }

    /**
     * Filtra o que o cliente pediu pelo que ele pode ver.
     *
     * Pedido vazio significa "tudo que eu enxergo". Código que não é dele é
     * ignorado em silêncio, não recusado: dizer "esse código não é seu"
     * confirma que ele existe.
     */
    public List<String> allowedCodes(Collection<String> requested) {
        Set<String> visible = visibleUnits().stream()
                .map(Customer::getCodParceiro)
                .collect(Collectors.toSet());

        if (requested == null || requested.isEmpty()) return List.copyOf(visible);

        return requested.stream().filter(visible::contains).toList();
    }
}
