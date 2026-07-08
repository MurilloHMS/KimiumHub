package com.proautokimium.api.Infrastructure.services.authentication;

import com.proautokimium.api.Application.DTOs.authentication.NewAccessDTO;
import com.proautokimium.api.Application.DTOs.authentication.NewAccessPasswordDTO;
import com.proautokimium.api.Application.DTOs.smtp.SmtpMail;
import com.proautokimium.api.Application.DTOs.user.AuthenticationDTO;
import com.proautokimium.api.Application.DTOs.user.RegisterDTO;
import com.proautokimium.api.Infrastructure.exceptions.auth.CredentialsIncorrectException;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.PasswordResetTokenRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.email.smtp.SmtpService;
import com.proautokimium.api.Infrastructure.utils.UsernameSanitizer;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.auth.FirstAcessToken;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.enums.UserRole;
import com.proautokimium.api.domain.exceptions.auth.UserNotFoundException;
import com.proautokimium.api.domain.exceptions.partners.EmployeeHasAlreadyLinkedException;
import com.proautokimium.api.domain.exceptions.partners.EmployeeNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.file.attribute.UserPrincipalNotFoundException;
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
    private final SmtpService emailService;

    public AuthenticationService(AuthenticationManager authenticationManager, UserRepository repository, EmployeeRepository employeeRepository, TokenAuthService accessTokenService, PasswordResetTokenRepository passwordResetTokenRepository,TokenService tokenService, SmtpService emailService) {
        this.authenticationManager = authenticationManager;
        this.repository = repository;
        this.employeeRepository = employeeRepository;
        this.accessTokenService = accessTokenService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.tokenService = tokenService;
        this.emailService = emailService;
    }

    public String login(AuthenticationDTO dto){
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(dto.login(), dto.password());
        Authentication authentication;
        try{
            authentication = this.authenticationManager.authenticate(usernamePasswordAuthenticationToken);
        }catch (BadCredentialsException e){
            throw new CredentialsIncorrectException(e.getMessage());
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

    public User linkEmployee(User user, Employee employee) {
        if(user == null){
            throw new UserNotFoundException("Usuário não encontrado");
        }

        if(employee == null){
            throw new EmployeeNotFoundException();
        }

        Optional<User> jaVinculado = repository.findByEmployee_Id(employee.getId());
        if(jaVinculado.isPresent() && !jaVinculado.get().getId().equals(user.getId())){
            throw new EmployeeHasAlreadyLinkedException("Este funcionário já está vinculado ao usuário '" + jaVinculado.get().getLogin() + "'");
        }

        user.setEmployee(employee);
        return repository.save(user);
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

    // Helpers

    @Transactional
    protected User updatePassword(String login, String newPassword){
        User user = (User) repository.findByLogin(login);
        user.setPassword(new BCryptPasswordEncoder().encode(newPassword));
        return user;
    }

    private void sendEmail(String subject, String body, String email, String token){
        SmtpMail mail = new SmtpMail(
                List.of(email),
                "noreply@envios.proautokimium.com.br",
                null,
                subject,
                body + token,
                null,
                null,
                null
        );
    }
}
