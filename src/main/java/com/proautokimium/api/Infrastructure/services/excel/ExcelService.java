package com.proautokimium.api.Infrastructure.services.excel;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
public class ExcelService {
    public byte[] processMultiple(List<MultipartFile> files) throws IOException {
        ByteArrayOutputStream zipOutput = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(zipOutput);

        for(MultipartFile file : files){
            String originalName = file.getOriginalFilename();
            String outputName = "mod_" + originalName;

            byte[] processed = processSingle(file);

            ZipEntry entry = new ZipEntry(outputName);
            zos.putNextEntry(entry);
            zos.write(processed);
            zos.closeEntry();
        }

        zos.finish();
        zos.close();
        return zipOutput.toByteArray();
    }

    private byte[] processSingle(MultipartFile file) throws IOException {

        Path tempDir = Files.createTempDirectory("xlsm_");

        try {
            Path input = tempDir.resolve(file.getOriginalFilename());
            Files.write(input, file.getBytes());

            Path extracted = tempDir.resolve("extracted");
            extract(input, extracted);

            Path vba = extracted.resolve("xl/vbaProject.bin");
            patchVba(vba);

            Path output = tempDir.resolve("output.xlsm");
            zip(extracted, output);

            return Files.readAllBytes(output);

        } finally {
            deleteDirectory(tempDir);
        }
    }

    private void extract(Path zipFile, Path destDir) throws IOException {
        Files.createDirectories(destDir);

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                Path newPath = destDir.resolve(entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(newPath);
                } else {
                    Files.createDirectories(newPath.getParent());
                    Files.copy(zis, newPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private void patchVba(Path vbaPath) throws IOException {

        if (!Files.exists(vbaPath)) {
            throw new RuntimeException("arquivo não encontrado");
        }

        byte[] data = Files.readAllBytes(vbaPath);

        int count = 0;

        for (int i = 0; i < data.length - 2; i++) {
            if (data[i] == 'D' && data[i + 1] == 'P' && data[i + 2] == 'B') {
                data[i + 2] = 'X';
                count++;
            }
        }

        Files.write(vbaPath, data);
    }

    private void zip(Path sourceDir, Path output) throws IOException {

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(output))) {

            Files.walk(sourceDir)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry entry = new ZipEntry(sourceDir.relativize(path).toString());

                        try {
                            zos.putNextEntry(entry);
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }

    private void deleteDirectory(Path path) throws IOException {
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
