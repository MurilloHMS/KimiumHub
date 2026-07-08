package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.authentication.*;
import com.proautokimium.api.Application.DTOs.smtp.SmtpMail;
import com.proautokimium.api.Application.DTOs.user.*;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.PasswordResetTokenRepository;
import com.proautokimium.api.Infrastructure.services.authentication.AuthenticationService;
import com.proautokimium.api.Infrastructure.services.authentication.TokenAuthService;
import com.proautokimium.api.Infrastructure.services.email.smtp.SmtpService;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("api/auth")
@Tag(name = "Autenticação", description = "Autenticação e registro de usuários")
public class AuthenticationController {
    @Autowired
    AuthenticationManager authenticationManager;
    @Autowired
    UserRepository repository;

    @Autowired
    EmployeeRepository employeeRepository;

    @Autowired
    TokenAuthService accessTokenService;

    @Autowired
    PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    com.proautokimium.api.Infrastructure.security.TokenService tokenService;

    @Autowired
    SmtpService emailService;

    @Autowired
    AuthenticationService authService;

    @PostMapping("/login")
    @Operation(summary = "Realiza login", description = "Verifica usuário e senha e autoriza o login")
    public ResponseEntity<Object> Login(@RequestBody @Valid AuthenticationDTO data){
        return ResponseEntity.ok(new LoginResponseDTO(authService.login(data)));
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> Register(@RequestBody @Valid RegisterDTO data){
        if(this.repository.findByLogin(data.login()) != null) return ResponseEntity.status(HttpStatus.CONFLICT).body("O Usuário informado, já existe!");

        authService.signIn(data);
        return ResponseEntity.status(HttpStatus.OK).body("Usuário criado com sucesso!");
    }

    /** Vincula explicitamente um usuário a um funcionário (parceiro) pelo código do parceiro. */
    @PutMapping("/users/{login}/employee")
    @Operation(summary = "Vincular funcionário", description = "Vincula explicitamente um usuário a um funcionário (parceiro) pelo código do parceiro")
    public ResponseEntity<Object> linkEmployee(@PathVariable("login") String login,
                                               @RequestBody @Valid LinkEmployeeRequest body) {
        User user = (User) repository.findByLogin(login);
        if (user == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuário não encontrado");

        Employee employee = employeeRepository.findByCodParceiro(body.codParceiro());
        if (employee == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Funcionário não encontrado");

        authService.linkEmployee(user, employee);
        return ResponseEntity.ok("Usuário vinculado ao funcionário com sucesso!");
    }

    /** Remove o vínculo de um usuário com o funcionário. */
    @DeleteMapping("/users/{login}/employee")
    @Operation(summary = "Desvincula funcionário", description = "Realiza a exclusão do vínculo do funcionário")
    public ResponseEntity<Object> unlinkEmployee(@PathVariable("login") String login) {
        authService.unlinkEmployee(login);
        return ResponseEntity.ok("Vínculo removido com sucesso!");
    }

    @PostMapping("/app-token")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Gera token acesso", description = "Gera o token de acesso ao app ProStock")
    public ResponseEntity<Object> generateAppToken() {
        String appToken = tokenService.generateAppToken();
        return ResponseEntity.ok(new LoginResponseDTO(appToken));
    }

    @GetMapping("/users")
    @Operation(summary = "Retorna Usuários", description = "Obtém a lista de usuários")
    public ResponseEntity<Object> getUsers(){
        var users = repository.findAllWithEmployee();

        if(users == null || users.isEmpty())
        	return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Não foi encontrado Usuários válidos");

        return ResponseEntity.ok().body(users.stream().map(m -> new UserResponseDTO(
        		m.getLogin(), m.getRoles(),
        		m.getEmployee() != null ? m.getEmployee().getCodParceiro() : null)));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Recupera a senha", description = "Gera o token de recuperação e envia via email")
    public ResponseEntity<Object> forgotPassword(@RequestBody @Valid ForgotPasswordDTO dto) {
        User user = (User) repository.findByLogin(dto.login());

        if(user == null){
            return ResponseEntity.ok().build();
        }

        String token = accessTokenService.createToken(user);
        SmtpMail mail = new SmtpMail(
                List.of(user.getEmail()),
                "noreply@envios.proautokimium.com.br",
                null,
                "Token de recuperação de senha",
                "Use o seguinte token para redefinir sua senha: " + token,
                null,
                null,
                null
        );

        emailService.sendEmail(mail, null);
        return ResponseEntity.ok("Token de recuperação de senha enviado para o e-mail cadastrado.");
    }

    @PostMapping("/first-access")
    @Operation(summary = "Cria o Primeiro Acesso", description = "Gera o token de primeiro acesso e envia via email")
    public ResponseEntity<Object> forgotPassword(@RequestBody @Valid NewAccessDTO dto) {
        Optional<Employee> employee = employeeRepository.findByCpfDigits(dto.cpf());

        if(employee.isEmpty()){
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Não existe funcionário cadastrado com o CPF e email informado. Entre em contato com o RH para verificar.");
        }

        String token = accessTokenService.createTokenByEmployee(employee.get());
        SmtpMail mail = new SmtpMail(
                List.of(dto.email()),
                "noreply@envios.proautokimium.com.br",
                null,
                "Token de primeiro acesso",
                "Use o seguinte token para realizar o primeiro acesso a plataforma: " + token,
                null,
                null,
                null
        );

        emailService.sendEmail(mail, null);
        return ResponseEntity.ok("Token de recuperação de senha enviado para o e-mail cadastrado.");
    }

    @PostMapping("/first-access/{token}/is-valid")
    @Operation(summary = "Valida o token enviado", description = "Valida o token enviado por email do primeiro acesso")
    public ResponseEntity<?> firstAccessTokenIsValid(@PathVariable("token") String token){
        Boolean isValid = authService.firstAccessTokenIsValid(token);
        return isValid.equals(true) ? ResponseEntity.ok("Token Válido") : ResponseEntity.noContent().build();
    }

    @PostMapping("/first-access/{token}/sign-in")
    @Operation(summary = "Cria o novo usuário", description = "Cria o novo usuário")
    public ResponseEntity<?> createFirstUsername(@PathVariable("token") String token, @RequestBody @Valid NewAccessPasswordDTO dto){
        boolean isValid = authService.firstAccessTokenIsValid(token);
        if(isValid){
            User user = authService.signInFirstAccess(token, dto);
            return ResponseEntity.status(HttpStatus.CREATED).body("Usuário criado com sucesso!\n\n Utilize o usuário: " + user.getLogin() + " para realizar o login");
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset da senha", description = "Recebe o token e reseta a senha")
    public ResponseEntity<Object> resetPassword(@RequestBody @Valid ResetPasswordDTO dto) {
        var optionalToken = passwordResetTokenRepository.findByToken(dto.token());

        if(optionalToken.isEmpty()){
            return ResponseEntity.badRequest().body("Token inválido.");
        }

        var resetToken = optionalToken.get();
        if(resetToken.isUsed() ||
            resetToken.getExpiration().isBefore(java.time.LocalDateTime.now())){
            return ResponseEntity.badRequest().body("Token expirado ou já utilizado.");
        }

        User user = resetToken.getUser();
        user.setPassword(new BCryptPasswordEncoder().encode(dto.newPassword()));

        repository.save(user);
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
        return ResponseEntity.ok("Senha redefinida com sucesso.");
    }

    @PostMapping("/change-password")
    @Operation(summary = "Altera senha", description = "Altera a senha do usuário enviado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> changePassword(@RequestBody @Valid ChangePasswordDTO dto) {
        User user = (User) repository.findByLogin(dto.login());

        user.setPassword(new BCryptPasswordEncoder().encode(dto.newPassword()));
        repository.save(user);
        return ResponseEntity.ok("Senha alterada com sucesso.");
    }
    @PutMapping("/users/{login}/roles")
    @Operation(summary = "Retorna roles", description = "Retorna as roles de um usuário pelo login")
    public ResponseEntity<Object> getUserRoles(@PathVariable("login") String login, @RequestBody UpdateRolesRequest roles) {
        User user = (User) repository.findByLogin(login);

        if(user == null) return ResponseEntity.notFound().build();

        user.setRoles(roles.roles());
        repository.save(user);
        return ResponseEntity.ok().body("Roles Atualizadas com sucesso!");
    }
}
