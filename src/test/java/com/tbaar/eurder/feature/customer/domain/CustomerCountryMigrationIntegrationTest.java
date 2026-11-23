package com.tbaar.eurder.feature.customer.domain;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerCountryMigrationIntegrationTest {

    private static final String URL = "jdbc:h2:mem:country-migration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1";

    @Test
    void givenCustomersRegisteredBeforeTheCountryExisted_whenMigrating_thenTheyLiveInBelgium() throws SQLException {
        migrateTo("11");
        execute("""
                insert into credentials (id, email, password_hash, role) values
                ('5b0e8c2a-7f31-4d6e-9a42-3c8b1d0f6e27', 'jane@doe.be', 'hash', 'CUSTOMER')
                """);
        execute("""
                insert into customer (id, credentials_id, first_name, last_name, street, house_number, postal_code, city, phone_number) values
                ('1c3e5a7b-2d4f-4a6b-8c9d-0e1f2a3b4c5d', '5b0e8c2a-7f31-4d6e-9a42-3c8b1d0f6e27', 'Jane', 'Doe', 'Rue Haute', '1', '1000', 'Bruxelles', '0470123456')
                """);

        migrateTo("12");

        assertThat(countryOf("Jane")).isEqualTo("BELGIUM");
    }

    private static void migrateTo(String version) {
        Flyway.configure().dataSource(URL, "sa", "").target(version).load().migrate();
    }

    private static void execute(String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(URL, "sa", ""); Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static String countryOf(String firstName) throws SQLException {
        try (Connection connection = DriverManager.getConnection(URL, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("select country from customer where first_name = '" + firstName + "'")) {
            rows.next();
            return rows.getString(1);
        }
    }
}
