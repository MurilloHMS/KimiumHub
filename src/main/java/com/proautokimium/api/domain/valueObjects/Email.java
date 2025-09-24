package com.proautokimium.api.domain.valueObjects;

import jakarta.persistence.Embeddable;

import java.util.Objects;
import java.util.regex.Pattern;

@Embeddable
public final class Email {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private String address;

    protected Email() { }

    public Email(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Email address cannot be null or empty");
        }

        if (!isValidEmail(value)) {
            throw new IllegalArgumentException("Invalid email address");
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
        if (!(obj instanceof Email)) return false;
        Email other = (Email) obj;
        return Objects.equals(address, other.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }
}