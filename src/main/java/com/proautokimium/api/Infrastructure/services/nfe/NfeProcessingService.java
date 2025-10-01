package com.proautokimium.api.Infrastructure.services.nfe;

import com.proautokimium.api.Infrastructure.interfaces.nfe.INfeProcessing;
import com.proautokimium.api.Infrastructure.interfaces.nfe.INfeReader;
import com.proautokimium.api.Infrastructure.interfaces.nfe.INfeWriter;
import com.proautokimium.api.domain.models.NfeIcmsInfo;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class NfeProcessingService implements INfeProcessing {

    private final INfeReader reader;
    private final INfeWriter writer;

    public NfeProcessingService(INfeReader reader, INfeWriter writer) {
        this.reader = reader;
        this.writer = writer;
    }

    @Override
    public byte[] getData(List<InputStream> xmlFiles) throws Exception {
        List<NfeIcmsInfo> icmsData = new ArrayList<>();

        for(InputStream stream : xmlFiles){
            NfeIcmsInfo infos = reader.getIcmsByXml(stream);
            icmsData.add(infos);
        }
        return writer.saveIcmsData(icmsData);
    }
}
