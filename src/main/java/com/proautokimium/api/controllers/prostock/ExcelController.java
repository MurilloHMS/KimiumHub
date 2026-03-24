package com.proautokimium.api.controllers.prostock;

import com.proautokimium.api.Infrastructure.services.excel.ExcelService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/excel")
public class ExcelController {

    private final ExcelService excelService;

    public ExcelController(ExcelService excelService) {
        this.excelService = excelService;
    }

    @PostMapping(path = "/remove-credentials", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> patchExcel(@RequestParam("files") List<MultipartFile> files) throws IOException {

        byte[] zipResult = excelService.processMultiple(files);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=excel_sem_senha.zip")
                .body(zipResult);
    }
}