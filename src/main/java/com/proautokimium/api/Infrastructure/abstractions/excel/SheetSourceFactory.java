package com.proautokimium.api.Infrastructure.abstractions.excel;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SheetSourceFactory {

    private final List<SheetSource> sources;

    public SheetSourceFactory(List<SheetSource> sources) {
        this.sources = sources;
    }

    public SheetSource forFile(String filename){
        return sources.stream()
                .filter(source -> source.supports(filename))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("File " + filename + " not supported"));
    }
}
