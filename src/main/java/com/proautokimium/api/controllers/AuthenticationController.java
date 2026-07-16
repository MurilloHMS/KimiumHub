package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.authentication.*;
import com.proautokimium.api.Application.DTOs.user.*;
import com.proautokimium.api.Infrastructure.exceptions.auth.UserAlreadyExistsException;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.PasswordResetTokenRepository;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.authentication.AuthenticationService;
import com.proautokimium.api.Infrastructure.services.authentication.TokenAuthService;
import com.proautokimium.api.Infrastructure.services.email.AuthEmailService;
import com.proautokimium.api.Infrastructure.services.email.EmailQueueService;
import com.proautokimium.api.Infrastructure.services.notification.NotificationService;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.enums.NotificationType;
import com.proautokimium.api.domain.enums.UserRole;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("api/auth")
@Tag(name = "Autenticação", description = "Autenticação e registro de usuários")
public class AuthenticationController {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationController.class);

    private final UserRepository repository;
    private final EmployeeRepository employeeRepository;
    private final TokenAuthService accessTokenService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final TokenService tokenService;
    private final EmailQueueService emailService;
    private final AuthEmailService authEmailService;
    private final AuthenticationService authService;
    private final NotificationService notificationService;

    public AuthenticationController(
            UserRepository repository,
            EmployeeRepository employeeRepository,
            TokenAuthService accessTokenService,
            PasswordResetTokenRepository passwordResetTokenRepository,
            TokenService tokenService,
            EmailQueueService emailQueueService,
            AuthEmailService authEmailService,
            AuthenticationService authService,
            NotificationService notificationService
    ){
        this.repository = repository;
        this.employeeRepository = employeeRepository;
        this.accessTokenService = accessTokenService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.tokenService = tokenService;
        this.emailService = emailQueueService;
        this.authEmailService = authEmailService;
        this.authService = authService;
        this.notificationService = notificationService;
    }


    @PostMapping("/login")
    @Operation(summary = "Realiza login", description = "Verifica usuário e senha e autoriza o login")
    public ResponseEntity<Object> Login(@RequestBody @Valid AuthenticationDTO data){
        return ResponseEntity.ok(new LoginResponseDTO(authService.login(data)));
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> Register(@RequestBody @Valid RegisterDTO data){
        if(this.repository.findByLogin(data.login()) != null) return ResponseEntity.status(HttpStatus.CONFLICT).body("O Usuário informado, já existe!");

        return authService.signIn(data) != null ?
                ResponseEntity.status(HttpStatus.OK).body("Usuário criado com sucesso!")
                : ResponseEntity.noContent().build();
    }

    /** Vincula explicitamente um usuário a um funcionário (parceiro) pelo código do parceiro. */
    @PutMapping("/users/{login}/employee")
    @Operation(summary = "Vincular funcionário", description = "Vincula explicitamente um usuário a um funcionário (parceiro) pelo código do parceiro")
    public ResponseEntity<Object> linkEmployee(@PathVariable String login,
                                               @RequestBody @Valid LinkEmployeeRequest body) {
        return authService.linkEmployee(login, body) != null ?
                ResponseEntity.ok("Usuário vinculado ao funcionário com sucesso!")
                : ResponseEntity.noContent().build();
    }

    /** Remove o vínculo de um usuário com o funcionário. */
    @DeleteMapping("/users/{login}/employee")
    @Operation(summary = "Desvincula funcionário", description = "Realiza a exclusão do vínculo do funcionário")
    public ResponseEntity<Object> unlinkEmployee(@PathVariable String login) {
        return authService.unlinkEmployee(login) != null ?
                ResponseEntity.ok("Vínculo removido com sucesso!")
                : ResponseEntity.noContent().build();
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
        return ResponseEntity.ok(authService.getUsers());
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Recupera a senha", description = "Gera o token de recuperação e envia via email")
    public ResponseEntity<Object> forgotPassword(@RequestBody @Valid ForgotPasswordDTO dto) {
        User user = (User) repository.findByLogin(dto.login());

        if(user == null){
            return ResponseEntity.ok().build();
        }

        String token = accessTokenService.createToken(user);
        emailService.sendNow(
                user.getEmail(),
                "noreply@envios.proautokimium.com.br",
                "Token de recuperação de senha",
                "Use o seguinte token para redefinir sua senha: " + token);

        return ResponseEntity.ok("Token de recuperação de senha enviado para o e-mail cadastrado.");
    }

    @PostMapping("/first-access")
    @Operation(summary = "Cria o Primeiro Acesso", description = "Gera o token de primeiro acesso e envia via email")
    public ResponseEntity<Object> firstAccess(@RequestBody @Valid NewAccessDTO dto) {
        Optional<Employee> employee = employeeRepository.findByCpfDigits(dto.cpf());

        if(employee.isEmpty()){
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Não existe funcionário cadastrado com o CPF informado. Entre em contato com o RH para verificar.");
        }

        if(repository.findByEmployee_Id(employee.get().getId()).isPresent()){
            throw new UserAlreadyExistsException("Já existe um usuário cadastrado para o CPF informado. Utilize a recuperação de senha ou contate o RH.");
        }

        String token = accessTokenService.createTokenByEmployee(employee.get());
        authEmailService.sendFirstAccessToken(dto.email(), token);
        return ResponseEntity.ok("Token de primeiro acesso enviado para o e-mail informado.");
    }

    @PostMapping("/first-access/{token}/is-valid")
    @Operation(summary = "Valida o token enviado", description = "Valida o token enviado por email do primeiro acesso")
    public ResponseEntity<?> firstAccessTokenIsValid(@PathVariable String token){
        boolean isValid = authService.firstAccessTokenIsValid(token);
        return isValid ? ResponseEntity.ok("Token Válido") : ResponseEntity.badRequest().body("Token inválido ou expirado.");
    }

    @PostMapping("/first-access/{token}/sign-in")
    @Operation(summary = "Cria o novo usuário", description = "Cria o novo usuário")
    public ResponseEntity<?> createFirstUsername(@PathVariable String token, @RequestBody @Valid NewAccessPasswordDTO dto){
        boolean isValid = authService.firstAccessTokenIsValid(token);
        if(isValid){
            User user = authService.signInFirstAccess(token, dto);
            notifyStaffAboutFirstAccess(user);
            return ResponseEntity.status(HttpStatus.CREATED).body("Usuário criado com sucesso!\n\n Utilize o usuário: " + user.getLogin() + " para realizar o login");
        }
        return ResponseEntity.badRequest().body("Erro ao criar usuário, Token inválido ou expirado");
    }

    /** Aviso a RH/Desenvolvedores é melhor esforço: falha na entrega não pode desfazer nem esconder a criação do usuário. */
    private void notifyStaffAboutFirstAccess(User user){
        try{
            List<User> recipients = repository.findByRolesIn(List.of(UserRole.RH, UserRole.DEVELOPER));
            for(User recipient : recipients){
                notificationService.notify(
                        recipient.getLogin(),
                        NotificationType.GERAL,
                        "Novo usuário criado via primeiro acesso",
                        "O funcionário " + user.getEmployee().getName() + " criou o usuário '" + user.getLogin() + "' pelo fluxo de primeiro acesso.",
                        null);
            }
        }catch (Exception e){
            log.warn("Falha ao notificar RH/Desenvolvedores sobre o primeiro acesso do usuário {}", user.getLogin(), e);
        }
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset da senha", description = "Recebe o token e reseta a senha")
    public ResponseEntity<Object> resetPassword(@RequestBody @Valid ResetPasswordDTO dto) {
        String response = authService.resetPassword(dto);
        return ResponseEntity.ok(response);
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
    public ResponseEntity<Object> getUserRoles(@PathVariable String login, @RequestBody UpdateRolesRequest roles) {
        User user = (User) repository.findByLogin(login);

        if(user == null) return ResponseEntity.notFound().build();

        user.setRoles(roles.roles());
        repository.save(user);
        return ResponseEntity.ok().body("Roles Atualizadas com sucesso!");
    }
}
