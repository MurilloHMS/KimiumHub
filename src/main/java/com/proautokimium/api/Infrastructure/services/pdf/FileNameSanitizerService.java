package com.proautokimium.api.Infrastructure.services.pdf;

import com.proautokimium.api.Infrastructure.interfaces.pdf.IFileNameSanitizerService;
import org.springframework.stereotype.Service;

@Service
public class FileNameSanitizerService implements IFileNameSanitizerService {
    @Override
    public String Sanitize(String fileName) {
        if(fileName == null || fileName.isBlank()){
            return "unnamed";
        }

        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}
