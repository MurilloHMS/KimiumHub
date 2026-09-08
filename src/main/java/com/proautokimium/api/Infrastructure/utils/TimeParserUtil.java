package com.proautokimium.api.Infrastructure.utils;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.regex.Pattern;

public final class TimeParserUtil {

    private static final Pattern ACCEPT = Pattern.compile("\\d{1,2}([:]\\d{1,2})?");

    public static Optional<LocalTime> interpret(String text){
        if(text == null || text.isBlank()){
            return Optional.empty();
        }

        String clean = text.trim()
                .toLowerCase()
                .replace("h", ":")
                .trim();

        if(!ACCEPT.matcher(clean).matches()){
            return  Optional.empty();
        }

        try{
            return Optional.of(LocalTime.parse(clean));
        }catch (DateTimeParseException e){
            return Optional.empty();
        }
    }
}
