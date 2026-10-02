package com.gayatri.dentalclinic.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Locale;
import java.util.regex.Pattern;

// Format checks only: this does not verify that a postal address exists.
public class AddressValidator implements ConstraintValidator<ValidAddress, String> {
    private static final Pattern ALLOWED = Pattern.compile("[\\p{L}\\p{M}\\p{N} \\n.,'’#/()&:;°-]+");
    private static final Pattern WEBSITE = Pattern.compile("https?://|www\\.", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLACEHOLDER = Pattern.compile("n/?a|none|null|undefined|test|asdf|qwerty|unknown");
    private static final Pattern REPEATED = Pattern.compile("(.)\\1+");

    public static String normalize(String value) {
        if (value == null) return null;
        String normalized = value.replaceAll("\\r\\n?", "\n").replaceAll("[\\p{Zs}\\t]+", " ").strip();
        return normalized.isEmpty() ? null : normalized;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        String error = error(value);
        if (error == null) return true;
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(error).addConstraintViolation();
        return false;
    }

    private static String error(String value) {
        String address = normalize(value);
        if (address == null) return null;
        if (address.length() > 255) return "Address must be at most 255 characters";
        long letters = address.codePoints().filter(Character::isLetter).count();
        if (letters == 0) return "Address must include a street, area, or place name";
        if (address.length() < 5 || letters < 2) return "Enter a more complete address, including the street or area and city";
        if (!ALLOWED.matcher(address).matches()) return "Address contains unsupported characters";
        if (WEBSITE.matcher(address).find()) return "Enter a postal address, not a website link";
        String compact = address.replaceAll("[\\p{Zs}\\n]", "").toLowerCase(Locale.ROOT);
        if (PLACEHOLDER.matcher(compact).matches() || REPEATED.matcher(compact).matches()) return "Enter your address or leave this optional field blank";
        return null;
    }
}
