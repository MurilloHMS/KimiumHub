package com.proautokimium.api.Infrastructure.services.pdf.holerith;

import com.proautokimium.api.Application.DTOs.pdf.PdfPageInfoExtractorDTO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HolerithExtractorService {

    public List<PdfPageInfoExtractorDTO> extract(String inputPdfPath){

        File file = new File(inputPdfPath);
        if(!file.exists()){
            throw new IllegalArgumentException("Pdf file does not exist");
        }

        List<PdfPageInfoExtractorDTO> result = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(file)) {

            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = document.getNumberOfPages();

            for (int i = 1; i <= totalPages; i++) {

                stripper.setStartPage(i);
                stripper.setEndPage(i);

                String text = stripper.getText(document);

                String nome = extract(text, "Nome do Funcionário\\s+.*\\n.*?\\s([A-Z ]+)\\s\\d");
                String cpf = extract(text, "CPF:\\s([0-9.\\-]+)");
                String cargo = extract(text, "([A-Z][A-Z ()A-ZÀ-Ú]+?)\\s{2,}Data Admissão");
                String empresa = extract(text, "(PIOFEX REPRESENTAÇÃO COMERCIAL E SERVIÇOS)");
                String departamento = extract(text, "\\s(\\d{3}\\.\\d{3})\\s\\d{2}");

                Map<Integer, Double> events = parseEvents(text);

                Double inss = events.get(1950);
                Double inssFerias = events.get(1952);
                Double emprestimo = events.get(4004);
                Double fgts = extractFgts(text);
                Double irrf = events.get(1920);

                PdfPageInfoExtractorDTO dto = new PdfPageInfoExtractorDTO(
                        nome,
                        cargo,
                        cpf,
                        empresa,
                        departamento,
                        inss,
                        irrf,
                        inssFerias,
                        null,
                        null,
                        null,
                        fgts,
                        emprestimo
                );

                result.add(dto);
            }

        } catch (IOException e) {
            throw new RuntimeException("Error reading PDF", e);
        }

        return result;
    }

    private Map<Integer, Double> parseEvents(String text){

        Map<Integer, Double> events = new HashMap<>();

        String[] lines = text.split("\\n");

        Pattern pattern = Pattern.compile("^(\\d{3,4})\\s+.*?\\s+([0-9.,]+)$");

        for(String line : lines){

            Matcher matcher = pattern.matcher(line.trim());

            if(matcher.find()){

                int codigo = Integer.parseInt(matcher.group(1));

                String valorStr = matcher.group(2);

                double valor = Double.parseDouble(
                        valorStr.replace(".", "").replace(",", ".")
                );

                events.put(codigo, valor);
            }
        }

        return events;
    }

    private String extract(String text, String regex){
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private Double extractFgts(String text){

        String[] lines = text.split("\\n");

        for(int i = 0; i < lines.length; i++){

            if(lines[i].contains("FGTS Mês")){

                if(i + 1 < lines.length){

                    String[] values = lines[i + 1].trim().split("\\s+");

                    if(values.length >= 4){
                        String fgts = values[3];

                        fgts = fgts.replace(".", "").replace(",", ".");

                        return Double.parseDouble(fgts);
                    }
                }
            }
        }

        return null;
    }
}