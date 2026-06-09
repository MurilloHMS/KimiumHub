package com.proautokimium.api.Infrastructure.interfaces.vcard;

import com.proautokimium.api.domain.entities.Profile;

public interface IVCardService {
    byte[] generate(Profile profile);
}