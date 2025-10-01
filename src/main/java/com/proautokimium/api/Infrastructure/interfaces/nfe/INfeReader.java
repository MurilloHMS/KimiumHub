package com.proautokimium.api.Infrastructure.interfaces.nfe;

import com.proautokimium.api.domain.models.NfeDataInfo;
import com.proautokimium.api.domain.models.NfeIcmsInfo;

import java.io.InputStream;
import java.util.List;

public interface INfeReader {
    NfeIcmsInfo getIcmsByXml(InputStream stream) throws Exception;
    List<NfeDataInfo> getNfeDataByXml(InputStream stream) throws Exception;
}
