package com.proautokimium.api.Infrastructure.services.vcard;

import com.proautokimium.api.Application.DTOs.profile.ProfileCreateDto;
import com.proautokimium.api.Application.DTOs.profile.ProfileResponseDto;
import com.proautokimium.api.Application.DTOs.profile.ProfileUpdateDto;
import com.proautokimium.api.Infrastructure.converters.ProfileConverter;
import com.proautokimium.api.Infrastructure.repositories.ProfileRepository;
import com.proautokimium.api.domain.entities.Profile;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private ProfileRepository repository;

    @Mock
    private ProfileConverter converter;

    @InjectMocks
    private ProfileService profileService;

    private UUID profileId;
    private Profile profile;
    private ProfileResponseDto responseDto;

    @BeforeEach
    void setUp() {
        profileId = UUID.randomUUID();
        profile = mock(Profile.class);
        responseDto = new ProfileResponseDto(profileId, "João Silva", "joao-silva", "Dev", "Empresa", "joao@teste.com", null, null, List.of(), List.of(), List.of(), List.of(), true);
    }

    @Test
    @DisplayName("Deve retornar lista de todos os profiles")
    void deveRetornarTodosOsProfiles() {
        when(repository.findAll()).thenReturn(List.of(profile));
        when(converter.toDto(profile)).thenReturn(responseDto);

        List<ProfileResponseDto> result = profileService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).slug()).isEqualTo("joao-silva");
    }

    @Test
    @DisplayName("Deve retornar profile por ID com sucesso")
    void deveRetornarProfilePorId() {
        when(repository.findById(profileId)).thenReturn(Optional.of(profile));
        when(converter.toDto(profile)).thenReturn(responseDto);

        ProfileResponseDto result = profileService.getById(profileId);

        assertThat(result).isNotNull();
        assertThat(result.slug()).isEqualTo("joao-silva");
    }

    @Test
    @DisplayName("Deve lançar EntityNotFoundException ao buscar profile inexistente")
    void deveLancarExcecaoAoBuscarProfileInexistente() {
        when(repository.findById(profileId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getById(profileId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("Deve retornar profile por slug quando ativo")
    void deveRetornarProfilePorSlug() {
        when(repository.findBySlugAndAtivoTrue("joao-silva")).thenReturn(Optional.of(profile));

        Optional<Profile> result = profileService.findBySlug("joao-silva");

        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("Deve criar profile com sucesso")
    void deveCriarProfileComSucesso() {
        ProfileCreateDto createDto = new ProfileCreateDto("João Silva", "joao-silva", "Dev", "Empresa", "joao@teste.com", null, null, List.of(), List.of(), List.of(), List.of(), true);
        when(repository.existsBySlug("joao-silva")).thenReturn(false);
        when(converter.fromCreateDto(createDto)).thenReturn(profile);
        when(repository.save(profile)).thenReturn(profile);
        when(converter.toDto(profile)).thenReturn(responseDto);

        ProfileResponseDto result = profileService.create(createDto);

        assertThat(result).isNotNull();
        verify(repository).save(profile);
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar profile com slug duplicado")
    void deveLancarExcecaoAoCriarProfileComSlugDuplicado() {
        ProfileCreateDto createDto = new ProfileCreateDto("João Silva", "joao-silva", "Dev", "Empresa", "joao@teste.com", null, null, List.of(), List.of(), List.of(), List.of(), true);
        when(repository.existsBySlug("joao-silva")).thenReturn(true);

        assertThatThrownBy(() -> profileService.create(createDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Slug já em uso");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Deve deletar profile (desativar) com sucesso")
    void deveDeletarProfileComSucesso() {
        when(repository.findById(profileId)).thenReturn(Optional.of(profile));

        profileService.delete(profileId);

        verify(profile).setAtivo(false);
        verify(repository).save(profile);
    }

    @Test
    @DisplayName("Deve lançar EntityNotFoundException ao deletar profile inexistente")
    void deveLancarExcecaoAoDeletarProfileInexistente() {
        when(repository.findById(profileId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.delete(profileId))
                .isInstanceOf(EntityNotFoundException.class);

        verify(repository, never()).save(any());
    }
}
