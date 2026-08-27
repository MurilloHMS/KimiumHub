package com.proautokimium.api.Infrastructure.services.vcard;

import com.proautokimium.api.Application.DTOs.profile.*;
import com.proautokimium.api.Infrastructure.converters.ProfileConverter;
import com.proautokimium.api.Infrastructure.repositories.EmployeeRepository;
import com.proautokimium.api.Infrastructure.repositories.ProfileRepository;
import com.proautokimium.api.Infrastructure.repositories.UserRepository;
import com.proautokimium.api.Infrastructure.services.storage.ProfileImageStorageService;
import com.proautokimium.api.domain.entities.Employee;
import com.proautokimium.api.domain.entities.Profile;
import com.proautokimium.api.domain.entities.auth.User;
import com.proautokimium.api.domain.enums.UserRole;
import com.proautokimium.api.domain.exceptions.partners.AccountNotLinkedToEmployeeException;
import com.proautokimium.api.domain.exceptions.partners.EmployeeNotFoundException;
import com.proautokimium.api.domain.exceptions.profile.ProfileAlreadyExistsException;
import com.proautokimium.api.domain.exceptions.profile.ProfileNotFoundException;
import com.proautokimium.api.domain.valueObjects.Email;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.Normalizer;
import com.proautokimium.api.Infrastructure.services.permission.PermissionService;
import com.proautokimium.api.domain.enums.Permission;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository repository;
    private final ProfileConverter converter;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final ProfileImageStorageService profileImageStorage;
    private final PermissionService permissionService;

    public List<ProfileResponseDto> getAll() {
        return repository.findAll()
                .stream()
                .map(converter::toDto)
                .toList();
    }

    public ProfileResponseDto getById(UUID id) {
        Profile profile = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Profile não encontrado: " + id));
        return converter.toDto(profile);
    }

    public Optional<Profile> findBySlug(String slug) {
        return repository.findBySlugAndAtivoTrue(slug);
    }

    @Transactional
    public ProfileResponseDto create(ProfileCreateDto dto) {
        if (repository.existsBySlug(dto.slug())) {
            throw new IllegalArgumentException("Slug já em uso: " + dto.slug());
        }
        Profile profile = converter.fromCreateDto(dto);
        return converter.toDto(repository.save(profile));
    }

    @Transactional
    public ProfileResponseDto update(UUID id, ProfileUpdateDto dto) {
        Profile profile = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Profile não encontrado: " + id));

        if (!profile.getSlug().equals(dto.slug()) && repository.existsBySlug(dto.slug())) {
            throw new IllegalArgumentException("Slug já em uso: " + dto.slug());
        }

        converter.updateFromDto(dto, profile);
        return converter.toDto(repository.save(profile));
    }

    @Transactional
    public void delete(UUID id) {
        Profile profile = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Profile não encontrado: " + id));
        profile.setAtivo(false);
        repository.save(profile);
    }

    // ── Self-service endpoints ────────────────────────────────────────────────

    public MyProfileResponseDto getMyProfile(String login) {
        User user = userRepository.findByLoginWithEmployee(login)
                .orElseThrow(() -> new EmployeeNotFoundException("Usuário não encontrado"));

        Employee employee = resolveEmployee(login);
        if (employee == null) {
            throw new AccountNotLinkedToEmployeeException();
        }

        ProfileResponseDto profileDto = repository.findByEmployee_Id(employee.getId())
                .map(converter::toDto)
                .orElse(null);

        // Criar o cartão digital é `perfil:INCLUIR`, e não mais a role VENDEDOR.
        //
        // A tela é de todo mundo — cada um vê o próprio perfil. O que era só do
        // vendedor é criar o cartão, e cravar isso aqui como role obrigava a
        // mexer em código toda vez que alguém de fora de vendas precisasse.
        //
        // É por isso que a checagem é do domínio e não um `@PreAuthorize`:
        // recusar o endpoint inteiro tiraria de quem não pode criar até o
        // direito de ler o próprio perfil.
        boolean canCreate = permissionService.can(user.getId(), "perfil", Permission.INCLUIR);

        String empresa = employee.getCompany() != null ? employee.getCompany().getName() : null;
        String email = user.getEmail() != null ? user.getEmail() :
                (employee.getEmail() != null ? employee.getEmail().getAddress() : null);

        return new MyProfileResponseDto(
                profileDto,
                employee.getName(),
                email,
                null,
                empresa,
                canCreate
        );
    }

    @Transactional
    public ProfileResponseDto createMyProfile(String login, ProfileCreateDto dto) {
        Employee employee = resolveEmployee(login);
        if (employee == null) {
            throw new AccountNotLinkedToEmployeeException();
        }

        if (repository.existsByEmployee_Id(employee.getId())) {
            throw new ProfileAlreadyExistsException();
        }

        Profile profile = converter.fromCreateDto(dto);
        profile.setEmployee(employee);

        if (dto.slug() == null || dto.slug().isBlank()) {
            profile.setSlug(generateUniqueSlug(employee.getName()));
        } else if (repository.existsBySlug(dto.slug())) {
            throw new IllegalArgumentException("Slug já em uso: " + dto.slug());
        }

        if (dto.email() == null || dto.email().isBlank()) {
            User user = userRepository.findByLoginWithEmployee(login).orElse(null);
            String email = user != null && user.getEmail() != null ? user.getEmail() :
                    (employee.getEmail() != null ? employee.getEmail().getAddress() : null);
            if (email != null) {
                profile.setEmail(new Email(email));
            }
        }

        return converter.toDto(repository.save(profile));
    }

    @Transactional
    public ProfileResponseDto updateMyProfile(String login, ProfileUpdateDto dto) {
        Employee employee = resolveEmployee(login);
        if (employee == null) {
            throw new AccountNotLinkedToEmployeeException();
        }

        Profile profile = repository.findByEmployee_Id(employee.getId())
                .orElseThrow(ProfileNotFoundException::new);

        if (dto.slug() != null && !profile.getSlug().equals(dto.slug()) && repository.existsBySlug(dto.slug())) {
            throw new IllegalArgumentException("Slug já em uso: " + dto.slug());
        }

        converter.updateFromDto(dto, profile);
        return converter.toDto(repository.save(profile));
    }

    @Transactional
    public String uploadMyProfileImage(String login, MultipartFile file) throws IOException {
        Employee employee = resolveEmployee(login);
        if (employee == null) {
            throw new AccountNotLinkedToEmployeeException();
        }

        Profile profile = repository.findByEmployee_Id(employee.getId())
                .orElseThrow(ProfileNotFoundException::new);

        String imageUrl = profileImageStorage.save(file, "profile");
        profile.setImagem(imageUrl);
        repository.save(profile);

        return imageUrl;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Employee resolveEmployee(String login) {
        Employee viaLink = userRepository.findByLoginWithEmployee(login)
                .map(User::getEmployee)
                .orElse(null);
        if (viaLink != null) return viaLink;
        return employeeRepository.findByUsername(login).orElse(null);
    }

    private String generateUniqueSlug(String name) {
        String base = slugify(name);
        if (!repository.existsBySlug(base)) {
            return base;
        }
        int counter = 2;
        while (repository.existsBySlug(base + "-" + counter)) {
            counter++;
        }
        return base + "-" + counter;
    }

    private String slugify(String text) {
        if (text == null) return "profile";
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        return normalized
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("[\\s]+", "-");
    }
}