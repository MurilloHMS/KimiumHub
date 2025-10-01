package com.proautokimium.api.Infrastructure.interfaces.nfe;

import com.proautokimium.api.domain.models.NfeDataInfo;
import com.proautokimium.api.domain.models.NfeIcmsInfo;

import java.util.List;

public interface INfeWriter {
    byte[] saveIcmsData(List<NfeIcmsInfo> icmsList) throws Exception;
    byte[] saveNfeData(List<NfeDataInfo> icmsList) throws Exception;
}
