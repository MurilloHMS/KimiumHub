package com.proautokimium.api.Infrastructure.services.authentication;

import com.proautokimium.api.Application.DTOs.authentication.NewAccessPasswordDTO;
import com.proautokimium.api.Application.DTOs.authentication.ResetPasswordDTO;
import com.proautokimium.api.Application.DTOs.user.AuthenticationDTO;
import com.proautokimium.api.Application.DTOs.user.LinkEmployeeRequest;
import com.proautokimium.api.Application.DTOs.user.RegisterDTO;
import com.proautokimium.api.Application.DTOs.user.UserResponseDTO;
import com.proautokimium.api.Infrastructure.exceptions.auth.CredentialsIncorrectException;
import com.proautokimium.api.Infrastructure.exceptions.auth.token.TokenExpiredException;
import com.proautokimium.api.Infrastructure.exceptions.auth.token.TokenInvalidException;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.PasswordResetTokenRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.utils.UsernameSanitizer;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.auth.FirstAcessToken;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.exceptions.auth.UserNotFoundException;
import com.proautokimium.api.domain.exceptions.partners.EmployeeHasAlreadyLinkedException;
import com.proautokimium.api.domain.exceptions.partners.EmployeeNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AuthenticationService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository repository;
    private final EmployeeRepository employeeRepository;
    private final TokenAuthService accessTokenService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final TokenService tokenService;

    public AuthenticationService(AuthenticationManager authenticationManager, UserRepository repository, EmployeeRepository employeeRepository, TokenAuthService accessTokenService, PasswordResetTokenRepository passwordResetTokenRepository,TokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.repository = repository;
        this.employeeRepository = employeeRepository;
        this.accessTokenService = accessTokenService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.tokenService = tokenService;
    }

    public String login(AuthenticationDTO dto){
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(dto.login(), dto.password());
        Authentication authentication;
        try{
            authentication = this.authenticationManager.authenticate(usernamePasswordAuthenticationToken);
        }catch (BadCredentialsException e){
            throw new CredentialsIncorrectException(e.getMessage());
        }catch (Exception e){
            throw new IllegalArgumentException();
        }

        Object principal = authentication.getPrincipal();
        return tokenService.generateToken((User) principal);
    }

    public User signIn(RegisterDTO dto){
        String encryptedPassword = new BCryptPasswordEncoder().encode(dto.password());
        User newUser = new User(dto.login(), dto.login(), encryptedPassword, dto.roles());

        employeeRepository.findByUsername(dto.login()).ifPresent(newUser::setEmployee);

        return this.repository.save(newUser);
    }

    public User signInFirstAccess(String token, NewAccessPasswordDTO dto) {
        String encryptedPassword = new BCryptPasswordEncoder().encode(dto.password());

        FirstAcessToken firstAccessToken = accessTokenService.getToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido ou expirado."));

        String username = UsernameSanitizer.generateUnique(
                firstAccessToken.getEmployee().getName(),
                repository::existsByLogin
        );

        User newUser = new User();
        newUser.setLogin(username);
        newUser.setEmail(dto.email());
        newUser.setPassword(encryptedPassword);
        newUser.setEmployee(firstAccessToken.getEmployee());

        return repository.save(newUser);
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
        Optional<FirstAcessToken> valid = accessTokenService.isValid(token);
        return valid.map(firstAcessToken -> firstAcessToken.getExpiration().isAfter(LocalDateTime.now())).orElse(false);
    }

    public List<UserResponseDTO> getUsers(){
        List<User> users = repository.findAllWithEmployee();

        return users.stream().map(u -> new UserResponseDTO(
                u.getLogin(),
                u.getRoles(),
                u.getEmployee() != null
                        ? u.getEmployee().getCodParceiro()
                        : null)).toList();
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

    // Helpers

    @Transactional
    protected void updatePassword(User user, String newPassword){
        user.setPassword(new BCryptPasswordEncoder().encode(newPassword));
    }
}
