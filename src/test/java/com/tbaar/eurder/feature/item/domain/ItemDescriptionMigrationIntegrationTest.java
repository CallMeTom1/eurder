package com.tbaar.eurder.feature.item.domain;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ItemDescriptionMigrationIntegrationTest {

    private static final String URL = "jdbc:h2:mem:item-description-migration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1";

    @Test
    void givenAnItemWithADescriptionOf255Characters_whenMigrating_thenItIsKeptAndALongerOneIsRefused() throws SQLException {
        migrateTo("12");
        execute(insertItem("7a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d", "a".repeat(255)));

        migrateTo("13");
        Throwable thrown = catchThrowable(() -> execute(insertItem("8b2c3d4e-5f6a-4b7c-9d8e-0f1a2b3c4d5e", "a".repeat(256))));

        assertThat(thrown).isInstanceOf(SQLException.class);
    }

    private static String insertItem(String id, String description) {
        return "insert into item (id, name, description, price) values ('%s', 'Chair', '%s', 49.99)".formatted(id, description);
    }

    private static void migrateTo(String version) {
        Flyway.configure().dataSource(URL, "sa", "").target(version).load().migrate();
    }

    private static void execute(String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(URL, "sa", ""); Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }
}
