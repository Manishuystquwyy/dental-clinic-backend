package com.gayatri.dentalclinic.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void onlyActualDuplicateKeysReturnConflict() {
        for (SQLException sql : new SQLException[]{
                new SQLException("Duplicate private value", "23000", 1062),
                new SQLException("Unique constraint", "23505", 23505)}) {
            var response = handler.handleDataIntegrityViolation(new DataIntegrityViolationException("save", sql));
            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertEquals("A record with the same unique value already exists", response.getBody());
        }
    }

    @Test
    void missingLegacyColumnsAndOtherIntegrityErrorsAreNotReportedAsDuplicates() {
        for (SQLException sql : new SQLException[]{
                new SQLException("Field 'object_key' doesn't have a default value", "HY000", 1364),
                new SQLException("Column cannot be null", "23000", 1048),
                new SQLException("Foreign key failure", "23000", 1452),
                new SQLException("Data too long", "22001", 1406)}) {
            var response = handler.handleDataIntegrityViolation(new DataIntegrityViolationException("save", sql));
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertFalse(response.getBody().contains("unique"));
            assertFalse(response.getBody().contains(sql.getMessage()));
        }
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("No SQL cause")).getStatusCode());
    }
}
