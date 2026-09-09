package com.proautokimium.api.domain.valueObjects;

import com.proautokimium.api.domain.exceptions.email.EmailInvalidException;
import jakarta.persistence.Embeddable;

import java.util.Objects;
import java.util.regex.Pattern;

@Embeddable
public final class Email {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@" +
                    "(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$"
    );

    private String address;

    protected Email() { }

    public Email(String value) {
        if (value == null || value.isBlank()) {
            throw new EmailInvalidException("Email address cannot be null or empty : " + value);
        }

        if (!isValid(value)) {
            throw new EmailInvalidException("Email address isn't valid: " + value);
        }

        this.address = value;
    }

    public String getAddress() {
        return address;
    }

    public static boolean isValid(String value) {
        return value != null && !value.isBlank() && isValidEmail(value);
    }

    private static boolean isValidEmail(String value) {
        return EMAIL_PATTERN.matcher(value).matches();
    }

    @Override
    public String toString() {
        return address;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Email other)) return false;
        return Objects.equals(address, other.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }
}