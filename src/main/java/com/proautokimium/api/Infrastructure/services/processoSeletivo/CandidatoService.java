package com.proautokimium.api.Infrastructure.services.processoSeletivo;

import com.proautokimium.api.Application.DTOs.processoSeletivo.candidato.CreateCandidatoDTO;
import com.proautokimium.api.Application.DTOs.processoSeletivo.candidato.ResponseCandidatoDTO;
import com.proautokimium.api.Infrastructure.converters.processoSeletivo.CandidatoConverter;
import com.proautokimium.api.Infrastructure.exceptions.processoSeletivo.CandidatoAlreadyExistsException;
import com.proautokimium.api.Infrastructure.repositories.processoSeletivo.CandidatoRepository;
import com.proautokimium.api.domain.entities.processoSeletivo.Candidato;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CandidatoService {

    private final CandidatoRepository candidatoRepository;
    private final CandidatoConverter converter;

    public  CandidatoService(CandidatoRepository candidatoRepository, CandidatoConverter converter) {
        this.candidatoRepository = candidatoRepository;
        this.converter = converter;
    }

    /**
     * Cadastro manual, e a checagem de duplicado tem duas camadas de propósito.
     *
     * <p>A busca antes do save resolve o caso comum e devolve 409 com mensagem.
     * Mas ela é um {@code find}-depois-{@code save} <b>sem lock</b>: dois
     * cadastros simultâneos do mesmo e-mail passam os dois pela checagem. Antes
     * da V103 isso criava duas linhas em silêncio; com o índice único, a
     * segunda bate no banco — e sem o {@code catch} viraria <b>500</b> para o
     * que é, de novo, um e-mail repetido.
     *
     * <p>A busca é insensível a caixa porque o índice é sobre
     * {@code lower(email)}: sem isso, {@code Joao@x.com} passaria pela
     * checagem e morreria no índice.
     */
    @Transactional
    public Candidato create(CreateCandidatoDTO dto){
        String email = dto.email() == null ? null : dto.email().trim().toLowerCase();

        if(candidatoRepository.findByEmail_AddressIgnoreCase(email).isPresent())
            throw new CandidatoAlreadyExistsException();

        Candidato candidato = converter.fromCreateDto(dto);

        try {
            return candidatoRepository.saveAndFlush(candidato);
        } catch (DataIntegrityViolationException e) {
            throw new CandidatoAlreadyExistsException();
        }
    }

    public List<ResponseCandidatoDTO> listarCandidatos(){
        return candidatoRepository.findAll().stream().map(converter::toDto).toList();
    }
}
