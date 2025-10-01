package com.proautokimium.api.Infrastructure.interfaces.nfe;

import com.proautokimium.api.domain.models.NfeIcmsInfo;

import java.io.InputStream;

public interface INfeReader {
    NfeIcmsInfo getIcmsByXml(InputStream stream) throws Exception;
}
