package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.authentication.*;
import com.proautokimium.api.Application.DTOs.user.*;
import com.proautokimium.api.Infrastructure.exceptions.auth.UserAlreadyExistsException;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.PasswordResetTokenRepository;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.authentication.AuthenticationService;
import com.proautokimium.api.Infrastructure.services.authentication.AuthorizationService;
import com.proautokimium.api.Infrastructure.services.authentication.TokenAuthService;
import com.proautokimium.api.Infrastructure.services.email.AuthEmailService;
import com.proautokimium.api.Infrastructure.services.email.EmailQueueService;
import com.proautokimium.api.Infrastructure.services.notification.NotificationService;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.Infrastructure.exceptions.auth.CredentialsIncorrectException;
import com.proautokimium.api.domain.exceptions.permission.DeveloperPermissionsAreLockedException;
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
import org.springframework.security.core.Authentication;
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
    private final AuthorizationService authorizationService;

    public AuthenticationController(
            UserRepository repository,
            EmployeeRepository employeeRepository,
            TokenAuthService accessTokenService,
            PasswordResetTokenRepository passwordResetTokenRepository,
            TokenService tokenService,
            EmailQueueService emailQueueService,
            AuthEmailService authEmailService,
            AuthenticationService authService,
            NotificationService notificationService, AuthorizationService authorizationService
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
        this.authorizationService = authorizationService;
    }


    @PostMapping("/login")
    @Operation(summary = "Realiza login", description = "Verifica usuário e senha e autoriza o login")
    public ResponseEntity<Object> Login(@RequestBody @Valid AuthenticationDTO data){
        return ResponseEntity.ok(new LoginResponseDTO(authService.login(data)));
    }

    @PostMapping("/register")
    @PreAuthorize("hasAuthority('settings/admin:INCLUIR')")
    public ResponseEntity<Object> Register(@RequestBody @Valid RegisterDTO data){
        return authService.signIn(data) != null ?
                ResponseEntity.status(HttpStatus.OK).body("Usuário criado com sucesso!")
                : ResponseEntity.noContent().build();
    }

    /**
     * Vincula um usuário a um funcionário (parceiro) pelo código do parceiro.
     *
     * Estava sem trava nenhuma até 2026-08-27.
     */
    @PreAuthorize("hasAuthority('settings/admin:CONFIGURAR')")
    @PutMapping("/users/{login}/employee")
    @Operation(summary = "Vincular funcionário", description = "Vincula explicitamente um usuário a um funcionário (parceiro) pelo código do parceiro")
    public ResponseEntity<Object> linkEmployee(@PathVariable String login,
                                               @RequestBody @Valid LinkEmployeeRequest body) {
        return authService.linkEmployee(login, body) != null ?
                ResponseEntity.ok("Usuário vinculado ao funcionário com sucesso!")
                : ResponseEntity.noContent().build();
    }

    /** Remove o vínculo de um usuário com o funcionário. */
    /** Desvincular. Mesmo caso, e mais perigoso: tira o acesso de alguém. */
    @PreAuthorize("hasAuthority('settings/admin:CONFIGURAR')")
    @DeleteMapping("/users/{login}/employee")
    @Operation(summary = "Desvincula funcionário", description = "Realiza a exclusão do vínculo do funcionário")
    public ResponseEntity<Object> unlinkEmployee(@PathVariable String login) {
        return authService.unlinkEmployee(login) != null ?
                ResponseEntity.ok("Vínculo removido com sucesso!")
                : ResponseEntity.noContent().build();
    }

    @GetMapping("/users")
    @Operation(summary = "Retorna Usuários", description = "Obtém a lista de usuários")
    @PreAuthorize("hasAuthority('settings/admin:CONSULTAR')")
    public ResponseEntity<Object> getUsers(){
        return ResponseEntity.ok(authService.getUsers());
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Recupera a senha", description = "Gera o token de recuperação e envia via email")
    public ResponseEntity<Object> forgotPassword(@RequestBody @Valid ForgotPasswordDTO dto) {
        authorizationService.forgotPassword(dto.login());
        return ResponseEntity.ok("Token de recuperação de senha enviado para o e-mail cadastrado.");
    }

    @PostMapping("/users/{login}/reset-password")
    @Operation(summary = "Reset de senha pelo Admin/RH", description = "Gera o token de recuperação e envia via email para o usuário")
    @PreAuthorize("hasAuthority('settings/admin:CONFIGURAR')")
    public ResponseEntity<Object> resetPasswordByAdmin(@PathVariable String login) {
        authService.resetPasswordByAdmin(login);
        return ResponseEntity.ok("Token de redefinição enviado para o e-mail do usuário.");
    }

    @PreAuthorize("hasAuthority('settings/admin:ENVIAR')")
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
        if(user.getEmployee() == null) return;
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

    /**
     * Trocar a **própria** senha.
     *
     * Duas coisas mudaram aqui em 2026-08-27, e a segunda é o motivo da
     * primeira ser segura.
     *
     * Ele exigia `hasRole('ADMIN')`, o que quer dizer que ninguém trocava a
     * própria senha. Só que o corpo também trazia o `login` de quem trocar, e
     * o `currentPassword` **era ignorado** — abrir isso para todo mundo do
     * jeito que estava daria a qualquer funcionário logado o poder de trocar a
     * senha de qualquer pessoa, o administrador incluído.
     *
     * Agora o alvo é quem está autenticado, e a senha atual é conferida. É o
     * que "todo mundo pode trocar a própria senha" precisa significar.
     */
    @PostMapping("/change-password")
    @Operation(summary = "Altera a própria senha",
               description = "Confere a senha atual e troca a senha de quem está autenticado")
    public ResponseEntity<Object> changePassword(@RequestBody @Valid ChangePasswordDTO dto,
                                                 Authentication authentication) {
        // O login vem da autenticação, e não do corpo: aceitar o corpo aqui é
        // deixar quem chama escolher a vítima.
        User user = (User) repository.findByLogin(authentication.getName());
        if (user == null) {
            throw new CredentialsIncorrectException("Não foi possível trocar a senha.");
        }

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (!encoder.matches(dto.currentPassword(), user.getPassword())) {
            throw new CredentialsIncorrectException("A senha atual não confere.");
        }

        user.setPassword(encoder.encode(dto.newPassword()));
        repository.save(user);
        return ResponseEntity.ok("Senha alterada com sucesso.");
    }
    /**
     * Trocar as roles de alguém.
     *
     * **Estava sem trava nenhuma até 2026-08-27**: qualquer funcionário logado
     * mudava a role de qualquer pessoa, inclusive se dando ADMIN. Não era
     * decisão — ninguém tinha reparado.
     */
    @PreAuthorize("hasAuthority('settings/admin:CONFIGURAR')")
    @PutMapping("/users/{login}/roles")
    @Operation(summary = "Retorna roles", description = "Retorna as roles de um usuário pelo login")
    public ResponseEntity<Object> getUserRoles(@PathVariable String login, @RequestBody UpdateRolesRequest roles) {
        User user = (User) repository.findByLogin(login);

        if(user == null) return ResponseEntity.notFound().build();

        // A conta de desenvolvedor não perde o papel por aqui.
        //
        // Ela é a saída de emergência do controle de acesso: tem todas as
        // permissões por resolução, e é o que garante que sempre exista alguém
        // capaz de reabrir o sistema. Deixar a role cair por uma requisição
        // seria fechar essa saída sem ninguém perceber — e a volta é `INSERT`
        // no banco. O mesmo motivo pelo qual o bloqueio já a recusa.
        if (user.getRoles().contains(UserRole.DEVELOPER)
                && !roles.roles().contains(UserRole.DEVELOPER)) {
            throw new DeveloperPermissionsAreLockedException();
        }

        user.setRoles(roles.roles());
        repository.save(user);
        return ResponseEntity.ok().body("Roles Atualizadas com sucesso!");
    }

    @PutMapping("/users/{login}/block")
    @Operation(summary = "Bloqueia Usuário", description = "Bloqueia o acesso ao sistema pelo login")
    @PreAuthorize("hasAuthority('settings/admin:CONFIGURAR')")
    public ResponseEntity<Object> blockAccess(@PathVariable String login){
        authService.blockUser(login);
        return ResponseEntity.ok().body("Acesso do usuário foi bloqueado");
    }

    @PutMapping("/users/{login}/unblock")
    @Operation(summary = "Libera Usuário", description = "Libera o acesso ao sistema pelo login")
    @PreAuthorize("hasAuthority('settings/admin:CONFIGURAR')")
    public ResponseEntity<Object> unblockAccess(@PathVariable String login){
        authService.unblockUser(login);
        return ResponseEntity.ok().body("Acesso do usuário foi liberado");
    }

    @PutMapping("/users/{login}/customer")
    @PreAuthorize("hasAuthority('company/customers:CONFIGURAR')")
    @Operation(summary = "Vincula o usuário a um cliente")
    public ResponseEntity<Object> linkCustomer(@PathVariable String login,
                                               @RequestParam String codParceiro) {
        authService.linkCustomer(login, codParceiro);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/users/{login}/customer")
    @PreAuthorize("hasAuthority('company/customers:CONFIGURAR')")
    @Operation(summary = "Remove o acesso do usuário ao cliente")
    public ResponseEntity<Object> unlinkCustomer(@PathVariable String login) {
        authService.unlinkCustomer(login);
        return ResponseEntity.ok().build();
    }
}
