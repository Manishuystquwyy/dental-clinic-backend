package com.gayatri.dentalclinic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Uses an isolated, temporary database; never changes tables in the supplied database. */
@EnabledIfEnvironmentVariable(named = "TEST_MYSQL_URL", matches = ".+")
class MedicalRecordMigrationMySqlTest {
    @Test
    void repairsLegacyConstraintsPreservesReferencesAndCanBeRepeatedOnOldOrFreshSchemas() throws Exception {
        String schema = "medical_records_test_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(System.getenv("TEST_MYSQL_URL"),
                System.getenv("TEST_MYSQL_USERNAME"), System.getenv("TEST_MYSQL_PASSWORD"));
             var adminStatement = connection.createStatement()) {
            String originalCatalog = connection.getCatalog();
            adminStatement.execute("CREATE DATABASE `" + schema + "`");
            try {
                connection.setCatalog(schema);
                try (var statement = connection.createStatement()) {
                    statement.execute("CREATE TABLE medical_records (id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                            + "title VARCHAR(160) NOT NULL, object_key VARCHAR(255) NOT NULL, "
                            + "storage_bucket VARCHAR(255) NOT NULL, storage_namespace VARCHAR(255) NOT NULL)");
                    statement.execute("INSERT INTO medical_records (title, object_key, storage_bucket, storage_namespace) "
                            + "VALUES ('Existing report', 'original-key', 'original-bucket', 'original-namespace')");
                    SQLException failure = assertThrows(SQLException.class,
                            () -> statement.execute("INSERT INTO medical_records (title) VALUES ('New prescription')"));
                    assertEquals(1364, failure.getErrorCode());

                    var migration = new FileSystemResource("db/medical-records-legacy-storage.sql");
                    ScriptUtils.executeSqlScript(connection, migration);
                    ScriptUtils.executeSqlScript(connection, migration);
                    for (int i = 0; i < 2; i++) {
                        assertEquals(1, statement.executeUpdate("INSERT INTO medical_records (title) VALUES ('New prescription')"));
                    }
                    try (var rows = statement.executeQuery("SELECT * FROM medical_records WHERE id=1")) {
                        assertTrue(rows.next());
                        assertEquals("original-key", rows.getString("object_key"));
                        assertEquals("original-bucket", rows.getString("storage_bucket"));
                        assertEquals("original-namespace", rows.getString("storage_namespace"));
                    }
                    statement.execute("DROP TABLE medical_records");
                    statement.execute("CREATE TABLE medical_records (id BIGINT AUTO_INCREMENT PRIMARY KEY, title VARCHAR(160))");
                    ScriptUtils.executeSqlScript(connection, migration);
                    assertEquals(1, statement.executeUpdate("INSERT INTO medical_records (title) VALUES ('Fresh schema')"));
                }
            } finally {
                connection.setCatalog(originalCatalog);
                adminStatement.execute("DROP DATABASE `" + schema + "`");
            }
        }
    }
}
