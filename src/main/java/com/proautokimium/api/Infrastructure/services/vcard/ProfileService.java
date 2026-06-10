package com.proautokimium.api.Infrastructure.services.vcard;

import com.proautokimium.api.Application.DTOs.profile.ProfileResponseDto;
import com.proautokimium.api.Infrastructure.converters.ProfileConverter;
import com.proautokimium.api.Infrastructure.repositories.ProfileRepository;
import com.proautokimium.api.domain.entities.Profile;
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
        return repository.findAll().stream().map(converter::toDto).toList();
    }

    public Optional<Profile> findBySlug(String slug) {
        return repository.findBySlug(slug);
    }

    public Profile create(Profile profile) {
        if (repository.existsBySlug(profile.getSlug())) {
            throw new IllegalArgumentException("Já existe um profile com esse slug.");
        }

        return repository.save(profile);
    }

    public Profile update(UUID id, Profile profileUpdated) {
        Profile profile = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Profile não encontrado."));

        Optional<Profile> existingBySlug = repository.findBySlug(profileUpdated.getSlug());
        if (existingBySlug.isPresent() && !existingBySlug.get().getId().equals(id)) {
            throw new IllegalArgumentException("Já existe um profile com esse slug.");
        }

        profile.setNome(profileUpdated.getNome());
        profile.setSlug(profileUpdated.getSlug());
        profile.setCargo(profileUpdated.getCargo());
        profile.setEmail(profileUpdated.getEmail());
        profile.setImagem(profileUpdated.getImagem());
        profile.setDescricao(profileUpdated.getDescricao());
        profile.setTelefones(profileUpdated.getTelefones());
        profile.setRedesSociais(profileUpdated.getRedesSociais());
        profile.setRegioesAtendimento(profileUpdated.getRegioesAtendimento());
        profile.setSegmentosAtendimento(profileUpdated.getSegmentosAtendimento());
        profile.setAtivo(profileUpdated.isAtivo());

        return repository.save(profile);
    }
}