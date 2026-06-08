package com.proautokimium.api.Infrastructure.interfaces.converters;

import java.util.List;

/**
 * @param <E> Classe a ser convertida
 * @param <D> DTO response
 * @param <C> DTO de criação
 */
public interface DtoConverter<E, D, C> {
    D toDto(E entity);
    E fromCreateDto(C dto);
}
