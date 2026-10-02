package com.gayatri.dentalclinic.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AddressValidator.class)
public @interface ValidAddress {
    String message() default "Enter a valid postal address";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
