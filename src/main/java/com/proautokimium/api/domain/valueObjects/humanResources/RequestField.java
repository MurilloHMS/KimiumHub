package com.proautokimium.api.domain.valueObjects.humanResources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RequestField(
        String key,
        String label,
        String help,
        String type,
        boolean required,
        List<String> options,
        UUID documentTypeId
) {
    /** Os tipos de campo. É texto, e não enum, porque mora no jsonb: um tipo novo não quebra o que já foi gravado. */
    public static final String FILE = "FILE";
    public static final String CHOICE = "CHOICE";
    public static final Set<String> TYPES = Set.of(FILE, CHOICE, "SHORT_TEXT", "LONG_TEXT", "NUMBER", "DATE", "YES_NO");

    public boolean isFile(){
        return FILE.equals(type);
    }
}
