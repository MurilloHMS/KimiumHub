package com.proautokimium.api.Infrastructure.interfaces.nfe;

import java.io.InputStream;
import java.util.List;

public interface INfeProcessing {
    byte[] getIcmsData(List<InputStream> xmlFiles) throws Exception;
    byte[] getNfeData(List<InputStream> xmlFiles) throws Exception;
}
