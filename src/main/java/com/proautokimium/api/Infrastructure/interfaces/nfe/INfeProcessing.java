package com.proautokimium.api.Infrastructure.interfaces.nfe;

import java.io.InputStream;
import java.util.List;

public interface INfeProcessing {
    byte[] getData(List<InputStream> xmlFiles) throws Exception;
}
