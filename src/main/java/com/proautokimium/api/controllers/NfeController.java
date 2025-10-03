package com.proautokimium.api.controllers;

import com.proautokimium.api.Infrastructure.interfaces.nfe.INfeProcessing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("api/nfe")
public class NfeController {

    @Autowired
    INfeProcessing nfeProcessing;

    @PostMapping( value = "/icms/upload", consumes = "multipart/form-data")
    public ResponseEntity<byte[]> processIcmsFiles(@RequestParam("files") List<MultipartFile> files) throws Exception{
    	
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

    @PostMapping(value = "/process/upload", consumes = "multipart/form-data")
    public ResponseEntity<byte[]> processNfeDataFiles(@RequestParam("files") List<MultipartFile> files) throws Exception{
    	
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
}
