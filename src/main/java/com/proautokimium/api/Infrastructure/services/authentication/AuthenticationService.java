package com.proautokimium.api.Infrastructure.services.authentication;

import com.proautokimium.api.Application.DTOs.authentication.NewAccessPasswordDTO;
import com.proautokimium.api.Application.DTOs.authentication.ResetPasswordDTO;
import com.proautokimium.api.Application.DTOs.user.AuthenticationDTO;
import com.proautokimium.api.Application.DTOs.user.LinkEmployeeRequest;
import com.proautokimium.api.Application.DTOs.user.RegisterDTO;
import com.proautokimium.api.Application.DTOs.user.UserResponseDTO;
import com.proautokimium.api.Infrastructure.exceptions.auth.UserBlockedException;
import com.proautokimium.api.Infrastructure.exceptions.auth.CredentialsIncorrectException;
import com.proautokimium.api.Infrastructure.exceptions.auth.UserAlreadyExistsException;
import com.proautokimium.api.Infrastructure.exceptions.auth.token.TokenExpiredException;
import com.proautokimium.api.Infrastructure.exceptions.auth.token.TokenInvalidException;
import com.proautokimium.api.Infrastructure.repositories.CustomerRepository;
import com.proautokimium.api.Infrastructure.services.permission.PermissionProvisioningService;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.PasswordResetTokenRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.email.AuthEmailService;
import com.proautokimium.api.Infrastructure.utils.UsernameSanitizer;
import com.proautokimium.api.domain.entities.Customer;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.auth.FirstAccessToken;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.enums.UserRole;
import com.proautokimium.api.domain.exceptions.auth.UserNotFoundException;
import com.proautokimium.api.domain.exceptions.customer.CustomerNotFoundException;
import com.proautokimium.api.domain.exceptions.partners.EmployeeHasAlreadyLinkedException;
import com.proautokimium.api.domain.exceptions.partners.EmployeeNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.proautokimium.api.Application.DTOs.user.LoginResponseDTO;
import com.proautokimium.api.domain.exceptions.auth.RefreshTokenInvalidoException;

@Service
public class AuthenticationService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository repository;
    private final EmployeeRepository employeeRepository;
    private final TokenAuthService accessTokenService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final TokenService tokenService;
    private final Clock clock;
    private final AuthEmailService authEmailService;
    private final CustomerRepository customerRepository;
    private final PermissionProvisioningService permissionProvisioning;
    private final RefreshTokenService refreshTokens;

    public AuthenticationService(AuthenticationManager authenticationManager, UserRepository repository, EmployeeRepository employeeRepository, TokenAuthService accessTokenService, PasswordResetTokenRepository passwordResetTokenRepository, TokenService tokenService, Clock clock, AuthEmailService authEmailService, CustomerRepository customerRepository, PermissionProvisioningService permissionProvisioning, RefreshTokenService refreshTokens) {
        this.authenticationManager = authenticationManager;
        this.repository = repository;
        this.employeeRepository = employeeRepository;
        this.accessTokenService = accessTokenService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.tokenService = tokenService;
        this.clock = clock;
        this.authEmailService = authEmailService;
        this.customerRepository = customerRepository;
        this.permissionProvisioning = permissionProvisioning;
        this.refreshTokens = refreshTokens;
    }

    public LoginResponseDTO login(AuthenticationDTO dto){
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(dto.login(), dto.password());
        Authentication authentication;
        try{
            authentication = this.authenticationManager.authenticate(usernamePasswordAuthenticationToken);
        }catch (BadCredentialsException e){
            throw new CredentialsIncorrectException(e.getMessage());
        }catch (Exception e){
            throw new IllegalArgumentException();
        }

        User user = (User) authentication.getPrincipal();
        if(!user.isActive()){
            throw new UserBlockedException();
        }

        // Conta ativa não basta: cliente inativo perde o acesso ao portal no
        // mesmo instante em que sai do cadastro, sem ninguém lembrar de
        // bloquear o usuário dele.
        if(user.getCustomer() != null && !user.getCustomer().isAtivo()){
            throw new UserBlockedException();
        }
        return new LoginResponseDTO(
                tokenService.generateToken(user),
                refreshTokens.emitir(user));
    }

    /**
     * Troca o refresh token por um par novo.
     *
     * O access token nasce das roles ATUAIS do usuário, relidas do banco — não
     * das que estavam no token velho. Sem isso, quem perdeu um acesso hoje o
     * carregaria por mais uma semana, uma renovação de cada vez.
     */
    @Transactional
    public LoginResponseDTO renovar(String refreshToken) {
        RefreshTokenService.Renovacao renovacao = refreshTokens.renovar(refreshToken);

        User user = repository.findById(renovacao.userId())
                .orElseThrow(RefreshTokenInvalidoException::new);

        // Conta bloqueada depois do login não pode renovar: é o único momento em
        // que o sistema volta a olhar para ela antes dos sete dias.
        if (!user.isActive()) {
            refreshTokens.revogarTudo(user.getId());
            throw new UserBlockedException();
        }

        return new LoginResponseDTO(tokenService.generateToken(user), renovacao.refreshToken());
    }

    public User signIn(RegisterDTO dto){
        if(repository.findByLogin(dto.login()) != null)
            throw new UserAlreadyExistsException();

        String encryptedPassword = new BCryptPasswordEncoder().encode(dto.password());
        User newUser = new User(dto.login(), dto.email(), encryptedPassword, dto.roles());

        employeeRepository.findByUsername(dto.login()).ifPresent(newUser::setEmployee);

        User salvo = this.repository.save(newUser);

        // Depois do save, nunca antes: as células levam o `user_id`, e antes do
        // save ele ainda não existe — as linhas apontariam para nada.
        permissionProvisioning.provision(salvo);
        return salvo;
    }

    @Transactional
    public User signInFirstAccess(String token, NewAccessPasswordDTO dto) {
        FirstAccessToken firstAccessToken = accessTokenService.getToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido ou expirado."));


        // O convite sabe para quem ele é. Funcionário e cliente compartilham a
        // tabela, mas não as regras: um só pode ter um usuário, o outro pode
        // ter vários; um escolhe o próprio e-mail, o outro o recebe de quem
        // convidou.

        User newUser = switch(firstAccessToken.getPartner()){
            case Employee employee -> employeeFirstAccess(employee, dto);
            case Customer customer -> clientFirstAccess(customer, firstAccessToken);
            case null, default -> throw new IllegalStateException("Convite sem parceiro válido");
        };

        newUser.setPassword(new BCryptPasswordEncoder().encode(dto.password()));
        accessTokenService.markTokenUsed(firstAccessToken);

        User salvo = repository.save(newUser);

        // Os dois primeiros acessos passam por aqui, e o serviço é quem decide
        // que cliente não recebe grade — a decisão fica num lugar só, em vez de
        // um `if` repetido em cada chamador.
        permissionProvisioning.provision(salvo);
        return salvo;
    }

    private User employeeFirstAccess(Employee employee, NewAccessPasswordDTO dto) {
        repository.findByEmployee_Id(employee.getId())
                .ifPresent(existing -> {
                    throw new UserAlreadyExistsException("Este funcionário já possui o usuário '" + existing.getLogin() + "'. Utilize a recuperação de senha.");
                });

        User user = new User();
        user.setLogin(UsernameSanitizer.generateUnique(employee.getName(), repository::existsByLogin));
        user.setEmail(dto.email());
        user.setEmployee(employee);
        user.setRoles(List.of(UserRole.USER));

        return user;
    }

    /**
     * Vários acessos por CNPJ são normais — a empresa tem mais de uma pessoa.
     * O que não pode repetir é o e-mail, que é por onde cada uma entra.
     *
     * O endereço vem do convite e não do corpo da requisição: quem escolhe
     * para onde o acesso vai é quem convidou. Aceitar outro aqui deixaria o
     * convite trocar de dono entre o envio e o clique.
     */
    private User clientFirstAccess(Customer customer, FirstAccessToken invite) {
        if (!customer.isAtivo()) {
            throw new AccessDeniedException("Cliente inativo.");
        }

        String email = invite.getEmail();
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Convite de cliente sem e-mail.");
        }

        if (repository.findByEmail(email) != null) {
            throw new UserAlreadyExistsException("Já existe um acesso com o e-mail " + email + ". Utilize a recuperação de senha.");
        }

        User user = new User();
        user.setLogin(loginFromEmail(email, customer));
        user.setEmail(email);
        user.setCustomer(customer);
        user.setRoles(List.of(UserRole.CLIENTE));

        return user;
    }

    /**
     * joao.silva@empresa.com.br → joao.silva. O cliente entra por e-mail ou por
     * CNPJ, então o login é sobretudo o que a equipe lê na lista de acessos.
     * Endereço sem letra nenhuma cai no nome da empresa: o sanitizador recusa
     * um nome que sobra vazio, e `123@empresa.com` sobraria.
     */
    private String loginFromEmail(String email, Customer customer) {
        String localPart = email.substring(0, email.indexOf('@')).replace('.', ' ');
        boolean hasLetter = localPart.chars().anyMatch(Character::isLetter);

        return UsernameSanitizer.generateUnique(
                hasLetter ? localPart : customer.getName(),
                repository::existsByLogin);
    }

    public User linkEmployee(String login, LinkEmployeeRequest employeeRequest) {
        Optional<User> user = Optional.ofNullable((User) repository.findByLogin(login));
        user.orElseThrow(UserNotFoundException::new);

        Optional<Employee> employee = Optional.ofNullable(employeeRepository.findByCodParceiro(employeeRequest.codParceiro()));
        employee.orElseThrow(EmployeeNotFoundException::new);

        Optional<User> jaVinculado = repository.findByEmployee_Id(employee.get().getId());
        if(jaVinculado.isPresent() && !jaVinculado.get().getId().equals(user.get().getId())){
            throw new EmployeeHasAlreadyLinkedException("Este funcionário já está vinculado ao usuário '" + jaVinculado.get().getLogin() + "'");
        }

        user.get().setEmployee(employee.get());
        return repository.save(user.get());
    }

    public User unlinkEmployee(String login) {
        User user = (User) repository.findByLogin(login);
        if(user == null){
            throw new UserNotFoundException("Usuário não encontrado");
        }

        user.setEmployee(null);
        return repository.save(user);
    }

    public boolean firstAccessTokenIsValid(String token){
        Optional<FirstAccessToken> valid = accessTokenService.isValid(token);
        return valid.map(t -> t.isValid(LocalDateTime.now(clock))).orElse(false);
    }

    public List<UserResponseDTO> getUsers(){
        List<User> users = repository.findAllWithEmployee();

        return users.stream().map(u -> new UserResponseDTO(
                u.getLogin(),
                u.getRoles(),
                u.getEmployee() != null
                        ? u.getEmployee().getCodParceiro()
                        : null,
                u.isActive())).toList();
    }

    @Transactional
    public String resetPassword(ResetPasswordDTO dto){
        var resetToken = passwordResetTokenRepository.findByToken(dto.token())
                .orElseThrow(() -> new TokenInvalidException("Token inválido."));

        if(resetToken.isUsed() || resetToken.getExpiration().isBefore(LocalDateTime.now())){
            throw new TokenExpiredException("Token expirado ou já utilizado.");
        }

        User user = resetToken.getUser();
        updatePassword(user, dto.newPassword());

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        return "Senha redefinida com sucesso.";
    }

    @Transactional
    public void blockUser(String login) {
        User user = (User) repository.findByLogin(login);
        if (user == null) throw new UserNotFoundException();
        if(user.getRoles().contains(UserRole.DEVELOPER)) throw new UserBlockedException("Não é possível bloquear um desenvolvedor.");
        user.setActive(false);
        repository.save(user);
    }

    @Transactional
    public void unblockUser(String login) {
        User user = (User) repository.findByLogin(login);
        if (user == null) throw new UserNotFoundException();
        user.setActive(true);
        repository.save(user);
    }

    @Transactional
    public void resetPasswordByAdmin(String login){
        User user = (User) repository.findByLogin(login);
        if(user == null) throw new UserNotFoundException();

        String token = accessTokenService.createToken(user);
        authEmailService.sendResetPasswordToken(user, token);
    }

    @Transactional
    public void linkCustomer(String login, String codParceiro) {
        User user = repository.findByLoginWithCustomer(login)
                .orElseThrow(UserNotFoundException::new);

        Customer customer = customerRepository.findByCodParceiro(codParceiro)
                .orElseThrow(CustomerNotFoundException::new);

        user.setCustomer(customer);

        // O acesso ao portal é a role, não o vínculo: sem ela o @PreAuthorize
        // de /api/client recusa e a pessoa entra numa tela vazia.
        if (!user.getRoles().contains(UserRole.CLIENTE)) {
            user.getRoles().add(UserRole.CLIENTE);
        }

        repository.save(user);
    }

    @Transactional
    public void unlinkCustomer(String login) {
        User user = repository.findByLoginWithCustomer(login)
                .orElseThrow(UserNotFoundException::new);

        user.setCustomer(null);
        user.getRoles().remove(UserRole.CLIENTE);
        repository.save(user);
    }

    // Helpers

    @Transactional
    protected void updatePassword(User user, String newPassword){
        user.setPassword(new BCryptPasswordEncoder().encode(newPassword));
    }


}
