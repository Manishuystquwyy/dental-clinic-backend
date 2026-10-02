package com.gayatri.dentalclinic;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("prod")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:prod-startup;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.admin.email=", "app.admin.password="
})
class ProductionProfileStartupTest {
    @Autowired HikariDataSource dataSource;

    @Test
    void productionProfileStartsWithConfiguredConnectionPool() throws Exception {
        assertEquals(10, dataSource.getMaximumPoolSize());
        assertEquals(2, dataSource.getMinimumIdle());
        assertEquals(5000, dataSource.getConnectionTimeout());
        try (var connection = dataSource.getConnection()) {
            assertTrue(connection.isValid(2));
        }
    }
}
