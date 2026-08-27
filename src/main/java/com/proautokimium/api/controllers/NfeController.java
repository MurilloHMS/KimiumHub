package com.proautokimium.api.controllers;

import com.proautokimium.api.Infrastructure.interfaces.nfe.INfeProcessing;
import com.proautokimium.api.Infrastructure.services.nfe.NfseRenameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("api/nfe")
public class NfeController {

    @Autowired
    INfeProcessing nfeProcessing;

    @Autowired
    NfseRenameService nfseRenameService;

    @PreAuthorize("hasAuthority('company/nfe-collector:INCLUIR')")
    @PostMapping( value = "/icms/upload", consumes = "multipart/form-data")
    public ResponseEntity<byte[]> processIcmsFiles(@RequestParam List<MultipartFile> files) throws Exception{
    	
    	if(files.size() > 500) {
    		return ResponseEntity.badRequest().body(("Máximo permitido 500 arquivos. Você enviou " + files.size()).getBytes());
    	}
    	
        List<InputStream> xmlStreams = new ArrayList<>();

        for(MultipartFile file : files){
            if(!file.getOriginalFilename().toLowerCase().endsWith(".xml")) continue;
            xmlStreams.add(file.getInputStream());
        }

        byte[] excelFile = nfeProcessing.getIcmsData(xmlStreams);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=icms.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelFile);
    }

    @PreAuthorize("hasAuthority('company/nfe-collector:INCLUIR')")
    @PostMapping(value = "/process/upload", consumes = "multipart/form-data")
    public ResponseEntity<byte[]> processNfeDataFiles(@RequestParam List<MultipartFile> files) throws Exception{
    	
    	if(files.size() > 500) {
    		return ResponseEntity.badRequest().body(("Máximo permitido 500 arquivos. Você enviou " + files.size()).getBytes());
    	}
    	
        List<InputStream> xmlStreams = new ArrayList<>();

        for (MultipartFile file: files){
            if(!file.getOriginalFilename().toLowerCase().endsWith(".xml")) continue;
            xmlStreams.add(file.getInputStream());
        }

        byte[] excelFile = nfeProcessing.getNfeData(xmlStreams);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=nfe_data.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelFile);
    }

    @PreAuthorize("hasAnyAuthority('company/nfe-collector:INCLUIR', 'tools/pdf/nfse-rename:INCLUIR')")
    @PostMapping(value = "/nfse/upload", consumes = "multipart/form-data")
    public ResponseEntity<byte[]> renameNfseFileNames(@RequestParam List<MultipartFile> files) throws IOException {
        byte[] zipFile = nfseRenameService.renameFiles(files);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=nfse_renomeadas.zip")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zipFile);
    }
}
