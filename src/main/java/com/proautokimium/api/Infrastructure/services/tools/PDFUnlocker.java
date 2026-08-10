package com.proautokimium.api.Infrastructure.services.tools;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class PDFUnlocker {

    public static byte[] unlock(byte[] file, String password) {
        if(file == null) {
            throw new IllegalStateException("file is empty");
        }

        try(PDDocument document = Loader.loadPDF(file, password)) {
            if(document.isEncrypted()){
                document.setAllSecurityToBeRemoved(true);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                document.save(baos);
                return baos.toByteArray();
            }
        }catch(IOException e) {
            throw new IllegalStateException(e);
        }
        return null;
    }
}
