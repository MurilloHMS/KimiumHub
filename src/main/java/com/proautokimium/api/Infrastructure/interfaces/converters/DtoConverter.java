package com.proautokimium.api.Infrastructure.interfaces.converters;

import java.util.List;

public interface DtoConverter<E, D, C, U> {
    D toDto(E entity); // E = entidade | D = DTO | C = create | U = update
    E fromCreateDto(C dto);
    void updateFromDto(U dto, E entity);
}
