package com.proautokimium.api.Infrastructure.services.pdf;

import com.proautokimium.api.Infrastructure.interfaces.pdf.INameExtractor;
import org.springframework.stereotype.Service;

@Service
public class NameExtractorService  implements INameExtractor {

    @Override
    public String extractName(String text){
        String key = "FL";
        int indexName = text.toLowerCase().indexOf(key.toLowerCase());

        if(indexName != -1){
            String remaining = text.substring(indexName + key.length())
                    .trim();
            String[] parts = remaining.split("\\s+");

            StringBuilder nameBuilder = new StringBuilder();
            boolean foundName = false;

            for(String part : parts){
                if(!foundName && part.matches("\\d+")){
                    continue;
                }

                foundName = true;

                if(part.matches("\\d+")){
                    break;
                }

                nameBuilder.append(part).append(" ");
            }
            return nameBuilder.toString().trim();
        }
        return null;
    }
}
