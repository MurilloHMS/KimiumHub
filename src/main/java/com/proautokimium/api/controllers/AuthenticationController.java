package com.proautokimium.api.controllers;

import com.proautokimium.api.Application.DTOs.authentication.ChangePasswordDTO;
import com.proautokimium.api.Application.DTOs.authentication.ForgotPasswordDTO;
import com.proautokimium.api.Application.DTOs.authentication.ResetPasswordDTO;
import com.proautokimium.api.Application.DTOs.partners.EmployeeDTO;
import com.proautokimium.api.Application.DTOs.smtp.SmtpMail;
import com.proautokimium.api.Application.DTOs.user.*;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.PasswordResetTokenRepository;
import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Infrastructure.services.authentication.PasswordResetService;
import com.proautokimium.api.Infrastructure.services.email.smtp.SmtpService;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.User;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/auth")
public class AuthenticationController {
    @Autowired
    AuthenticationManager authenticationManager;
    @Autowired
    UserRepository repository;

    @Autowired
    EmployeeRepository employeeRepository;

    @Autowired
    PasswordResetService passwordResetService;

    @Autowired
    PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    TokenService tokenService;

    @Autowired
    SmtpService emailService;

    @PostMapping("/login")
    public ResponseEntity<Object> Login(@RequestBody @Valid AuthenticationDTO data){
        var usernamepassword = new UsernamePasswordAuthenticationToken(data.login(), data.password());
        var auth = this.authenticationManager.authenticate(usernamepassword);

        var token = tokenService.generateToken((User) auth.getPrincipal());
        return ResponseEntity.ok(new LoginResponseDTO(token));
    }

    @PostMapping("/login/android")
    public ResponseEntity<Object> LoginAndoid(@RequestBody @Valid AuthenticationDTO data){
        try {
            var usernamepassword = new UsernamePasswordAuthenticationToken(data.login(), data.password());
            var auth = this.authenticationManager.authenticate(usernamepassword);

            var token = tokenService.generateToken((User) auth.getPrincipal());

            Employee employee = employeeRepository.findByUsername(data.login()).orElse(null);

            if (employee == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Funcionário não encontrado para este usuário");
            }

            EmployeeDTO employeeDTO = new EmployeeDTO(
                    employee.getCodParceiro(),
                    employee.getDocumento(),
                    employee.getName(),
                    employee.getEmail().getAddress(),
                    employee.isAtivo(),
                    employee.getCodigoGerente(),
                    employee.getHierarquia(),
                    employee.getBirthday(),
                    employee.getDepartment()
            );

            return ResponseEntity.ok(new LoginAndroidResponseDTO(token, employeeDTO));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Credenciais inválidas");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Object> Register(@RequestBody @Valid RegisterDTO data){
        if(this.repository.findByLogin(data.login()) != null) return ResponseEntity.status(HttpStatus.CONFLICT).body("O Usuário informado, já existe!");

        String encryptedPassword = new BCryptPasswordEncoder().encode(data.password());
        User newUser = new User(data.login(), data.login(), encryptedPassword, data.roles());

        this.repository.save(newUser);
        return ResponseEntity.status(HttpStatus.OK).body("Usuário criado com sucesso!");
    }

    @PostMapping("/app-token")
    public ResponseEntity<Object> generateAppToken() {
        String appToken = tokenService.generateAppToken();
        return ResponseEntity.ok(new LoginResponseDTO(appToken));
    }

    @GetMapping("/users")
    public ResponseEntity<Object> getUsers(){
        var users = repository.findAll();
        
        if(users == null || users.isEmpty())
        	return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Não foi encontrado Usuários válidos");

        return ResponseEntity.ok().body(users.stream().map(m -> new UserResponseDTO(
        		m.getLogin(), m.getRoles())));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Object> forgotPassword(@RequestBody @Valid ForgotPasswordDTO dto) {
        User user = (User) repository.findByLogin(dto.login());

        if(user == null){
            return ResponseEntity.ok().build();
        }

        String token = passwordResetService.createToken(user);
        SmtpMail mail = new SmtpMail(
                List.of(user.getEmail()),
                "noreply@envios.proautokimium.com.br",
                "Token de recuperação de senha",
                "Use o seguinte token para redefinir sua senha: " + token,
                null,
                null,
                null,
                null
        );

        emailService.sendEmail(mail);
        return ResponseEntity.ok("Token de recuperação de senha enviado para o e-mail cadastrado.");
    }

    @PostMapping("/reset-password")
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
    public ResponseEntity<Object> changePassword(@RequestBody @Valid ChangePasswordDTO dto) {
        User user = (User) repository.findByLogin(dto.login());

        user.setPassword(new BCryptPasswordEncoder().encode(dto.newPassword()));
        repository.save(user);
        return ResponseEntity.ok("Senha alterada com sucesso.");
    }

}
