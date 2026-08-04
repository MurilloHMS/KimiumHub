package com.proautokimium.api.Infrastructure.services.nfe;

import com.proautokimium.api.Infrastructure.exceptions.file.FileNotFoundException;
import com.proautokimium.api.Infrastructure.services.pdf.FileNameSanitizerService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class NfseRenameService {

    private final FileNameSanitizerService sanitizerService;

    public NfseRenameService(FileNameSanitizerService sanitizerService) {
        this.sanitizerService = sanitizerService;
    }

    public byte[] renameFiles(List<MultipartFile> pdfFiles) throws IOException {
        if(pdfFiles.isEmpty())
            throw new FileNotFoundException("Nenhum arquivo enviado.");

        Map<String, byte[]> renamedFiles = new LinkedHashMap<>();

        for(MultipartFile file : pdfFiles){
            try(PDDocument document = Loader.loadPDF(file.getBytes())){
                PDFTextStripper stripper = new PDFTextStripper();
                String textPage = stripper.getText(document);

                String nfseNumber = sanitizerService.Sanitize(extractNfseNumber(textPage));
                String supplyerName = sanitizerService.Sanitize(extractSupplyerName(textPage));

                String pageName = nfseNumber + " - " + supplyerName;
                renamedFiles.put(pageName + ".pdf", file.getBytes());
            }catch (IOException e){
                throw new FileNotFoundException();
            }
        }

        return createZipFile(renamedFiles);
    }

    private String extractNfseNumber(String text){
        String key = "NÚMERO DA NFS-e";
        int indexNumber = text.toLowerCase().indexOf(key.toLowerCase());

        if(indexNumber != -1){
            String remaining = text.substring(indexNumber + key.length()).trim();
            String[] parts = remaining.split("\n");
            return parts[0].trim();
        }
        return null;
    }

    private String extractSupplyerName(String text){
        String key = "Nome / Nome Empresarial";
        int indexName = text.toLowerCase().indexOf(key.toLowerCase());

        if(indexName != -1){
            String remaining = text.substring(indexName + key.length()).trim();
            String[] parts = remaining.split("\n");
            return parts[0].trim();
        }
        return null;
    }

    private byte[] createZipFile(Map<String, byte[]> files) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try(ZipOutputStream zos = new ZipOutputStream(baos)){
            for(Map.Entry<String, byte[]> entry : files.entrySet()){
                ZipEntry zipEntry = new ZipEntry(entry.getKey());
                zos.putNextEntry(zipEntry);
                zos.write(entry.getValue());
                zos.closeEntry();
            }
            zos.finish();
        }
        return baos.toByteArray();
    }
}
