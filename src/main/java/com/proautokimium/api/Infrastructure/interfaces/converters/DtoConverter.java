package com.proautokimium.api.Infrastructure.interfaces.converters;

import java.util.List;

public interface DtoConverter<E, D, C> {
    D toDto(E entity); // E = entidade | D = DTO | C = create | U = update
    E fromCreateDto(C dto);
}
