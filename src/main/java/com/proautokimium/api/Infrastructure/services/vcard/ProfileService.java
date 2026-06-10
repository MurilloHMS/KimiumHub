package com.proautokimium.api.Infrastructure.services.vcard;

import com.proautokimium.api.Application.DTOs.profile.ProfileCreateDto;
import com.proautokimium.api.Application.DTOs.profile.ProfileResponseDto;
import com.proautokimium.api.Application.DTOs.profile.ProfileUpdateDto;
import com.proautokimium.api.Infrastructure.converters.ProfileConverter;
import com.proautokimium.api.Infrastructure.repositories.ProfileRepository;
import com.proautokimium.api.domain.entities.Profile;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository repository;
    private final ProfileConverter converter;

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
}