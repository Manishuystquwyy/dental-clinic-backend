package com.gayatri.dentalclinic.exception;

public class SignupValidationException extends RuntimeException {
    private final String field;
    public SignupValidationException(String field, String message) {
        super(message);
        this.field = field;
    }
    public String getField() { return field; }
}
