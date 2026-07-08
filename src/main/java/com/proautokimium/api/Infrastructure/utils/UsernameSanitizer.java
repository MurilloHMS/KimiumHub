package com.proautokimium.api.Infrastructure.utils;

import java.text.Normalizer;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class UsernameSanitizer {

    private static final Set<String> STOPWORDS = Set.of(
            "de", "da", "do", "das", "dos", "e"
    );

    private static final Pattern NON_ALFA = Pattern.compile("[^a-z]");

    private UsernameSanitizer(){}

    public static String generate(String completeName){
        if(completeName == null || completeName.isBlank()){
            throw new IllegalArgumentException("Nome completo não pode ser vazio.");
        }

        List<String> parts = normalizeParts(completeName);

        if(parts.isEmpty()){
            throw new IllegalArgumentException("Nome inválido após sanitização");
        }

        String first = parts.getFirst();

        if(parts.size() == 1){
            return first;
        }

        String second = findSecondName(parts);

        return second != null ? first + "." + second : first;
    }

    private static List<String> normalizeParts(String completeName){
        String withoutAccentMark = Normalizer.normalize(completeName, Normalizer.Form.NFD).replaceAll("\\p{M}", "");

        return List.of(withoutAccentMark.trim().toLowerCase().split("\\s+"))
                .stream()
                .map(p -> NON_ALFA.matcher(p).replaceAll(""))
                .filter(p -> !p.isBlank())
                .toList();
    }

    private static String findSecondName(List<String> parts){
        for(int i = 1; i< parts.size(); i++){
            String part = parts.get(i);
            if(!STOPWORDS.contains(part)){
                return part;
            }
        }
        return null;
    }

    public static String generateUnique(String completeName, Predicate<String> usernameExist){
        String base = generate(completeName);
        String username = base;
        int sufix = 1;

        while(usernameExist.test(username)){
            username = base + sufix++;
        }

        return username;
    }

}
