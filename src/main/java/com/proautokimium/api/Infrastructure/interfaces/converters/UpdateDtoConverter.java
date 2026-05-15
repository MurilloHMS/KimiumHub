package com.proautokimium.api.Infrastructure.interfaces.converters;

public interface UpdateDtoConverter <E, U>{
    void updateFromDto(U dto, E entity);
}
