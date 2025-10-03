package com.proautokimium.api.controllers;

import com.proautokimium.api.Infrastructure.security.TokenService;
import com.proautokimium.api.Application.DTOs.user.AuthenticationDTO;
import com.proautokimium.api.Application.DTOs.user.LoginResponseDTO;
import com.proautokimium.api.Application.DTOs.user.RegisterDTO;
import com.proautokimium.api.Application.DTOs.user.UserResponseDTO;
import com.proautokimium.api.domain.entities.User;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/auth")
public class AuthenticationController {
    @Autowired
    AuthenticationManager authenticationManager;
    @Autowired
    UserRepository repository;

    @Autowired
    TokenService tokenService;

    @PostMapping("/login")
    public ResponseEntity<Object> Login(@RequestBody @Valid AuthenticationDTO data){
        var usernamepassword = new UsernamePasswordAuthenticationToken(data.login(), data.password());
        var auth = this.authenticationManager.authenticate(usernamepassword);

        var token = tokenService.generateToken((User) auth.getPrincipal());
        return ResponseEntity.ok(new LoginResponseDTO(token));
    }

    @PostMapping("/register")
    public ResponseEntity<Object> Register(@RequestBody @Valid RegisterDTO data){
        if(this.repository.findByLogin(data.login()) != null) return ResponseEntity.badRequest().build();

        String encryptedPassword = new BCryptPasswordEncoder().encode(data.password());
        User newUser = new User(data.login(), encryptedPassword, data.roles());

        this.repository.save(newUser);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/app-token")
    public ResponseEntity<Object> generateAppToken() {
        String appToken = tokenService.generateAppToken();
        return ResponseEntity.ok(new LoginResponseDTO(appToken));
    }

    @GetMapping("/users")
    public ResponseEntity<Object> getUsers(){
        var users = repository.findAll();

        return ResponseEntity.ok().body(users.stream().map(m -> new UserResponseDTO(
        		m.getLogin(), m.getRoles())));
    }
}
