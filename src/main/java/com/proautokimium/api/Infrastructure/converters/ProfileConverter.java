package com.proautokimium.api.Infrastructure.converters;

import com.proautokimium.api.Application.DTOs.profile.*;
import com.proautokimium.api.Infrastructure.interfaces.converters.DtoConverter;
import com.proautokimium.api.domain.entities.Profile;
import com.proautokimium.api.domain.models.RedeSocial;
import com.proautokimium.api.domain.models.TelefoneContato;
import com.proautokimium.api.domain.valueObjects.Email;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProfileConverter implements DtoConverter<Profile, ProfileResponseDto, ProfileCreateDto> {

    @Override
    public ProfileResponseDto toDto(Profile entity) {
        return new ProfileResponseDto(
                entity.getId(),
                entity.getNome(),
                entity.getSlug(),
                entity.getCargo(),
                entity.getEmpresa(),
                entity.getEmail() != null ? entity.getEmail().getAddress() : null,
                entity.getImagem(),
                entity.getDescricao(),
                toTelefoneDtoList(entity.getTelefones()),
                toRedeSocialDtoList(entity.getRedesSociais()),
                entity.getRegioesAtendimento(),
                entity.getSegmentosAtendimento(),
                entity.isAtivo()
        );
    }

    @Override
    public Profile fromCreateDto(ProfileCreateDto dto) {
        Profile profile = new Profile();
        profile.setNome(dto.nome());
        profile.setSlug(dto.slug());
        profile.setCargo(dto.cargo());
        profile.setEmpresa(dto.empresa());
        profile.setEmail(new Email(dto.email()));
        profile.setImagem(dto.imagem());
        profile.setDescricao(dto.descricao());
        profile.setTelefones(toTelefoneList(dto.telefones()));
        profile.setRedesSociais(toRedeSocialList(dto.redesSociais()));
        profile.setRegioesAtendimento(dto.regioesAtendimento());
        profile.setSegmentosAtendimento(dto.segmentosAtendimento());
        if (dto.ativo() != null) profile.setAtivo(dto.ativo());
        return profile;
    }

    public void updateFromDto(ProfileUpdateDto dto, Profile profile) {
        profile.setNome(dto.nome());
        profile.setSlug(dto.slug());
        profile.setCargo(dto.cargo());
        profile.setEmpresa(dto.empresa());
        profile.setEmail(new Email(dto.email()));
        profile.setImagem(dto.imagem());
        profile.setDescricao(dto.descricao());
        profile.setTelefones(toTelefoneList(dto.telefones()));
        profile.setRedesSociais(toRedeSocialList(dto.redesSociais()));
        profile.setRegioesAtendimento(dto.regioesAtendimento());
        profile.setSegmentosAtendimento(dto.segmentosAtendimento());
        if (dto.ativo() != null) profile.setAtivo(dto.ativo());
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private List<TelefoneContato> toTelefoneList(List<TelefoneContatoDto> dtos) {
        if (dtos == null) return List.of();
        return dtos.stream().map(d -> {
            TelefoneContato t = new TelefoneContato();
            t.setTipo(d.tipo());
            t.setNumero(d.numero());
            return t;
        }).toList();
    }

    private List<RedeSocial> toRedeSocialList(List<RedeSocialDto> dtos) {
        if (dtos == null) return List.of();
        return dtos.stream().map(d -> {
            RedeSocial r = new RedeSocial();
            r.setTipo(d.tipo());
            r.setUrl(d.url());
            return r;
        }).toList();
    }

    private List<TelefoneContatoDto> toTelefoneDtoList(List<TelefoneContato> entities) {
        if (entities == null) return List.of();
        return entities.stream()
                .map(t -> new TelefoneContatoDto(t.getTipo(), t.getNumero()))
                .toList();
    }

    private List<RedeSocialDto> toRedeSocialDtoList(List<RedeSocial> entities) {
        if (entities == null) return List.of();
        return entities.stream()
                .map(r -> new RedeSocialDto(r.getTipo(), r.getUrl()))
                .toList();
    }
}