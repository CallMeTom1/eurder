package com.tbaar.eurder.feature.item.domain;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class ItemStockMigrationIntegrationTest {

    private static final String URL = "jdbc:h2:mem:stock-migration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1";

    @Test
    void givenItemsWithStockBeforeItemUnitsExisted_whenMigrating_thenEachUnitOfStockBecomesAnAvailablePristineUnit() throws SQLException {
        migrateTo("7");
        execute("""
                insert into item (id, name, description, price, stock) values
                ('9d92d67e-6ef3-450d-85ac-11180c8826d2', 'Chair', 'Wooden chair', 49.99, 3),
                ('1c3e5a7b-2d4f-4a6b-8c9d-0e1f2a3b4c5d', 'Lamp', 'Desk lamp', 9.99, 0)
                """);

        migrateTo("8");

        assertThat(availablePristineUnitsByItemName()).containsExactly(entry("Chair", 3L), entry("Lamp", 0L));
    }

    private static void migrateTo(String version) {
        Flyway.configure().dataSource(URL, "sa", "").target(version).load().migrate();
    }

    private static void execute(String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(URL, "sa", ""); Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static Map<String, Long> availablePristineUnitsByItemName() throws SQLException {
        Map<String, Long> units = new LinkedHashMap<>();
        try (Connection connection = DriverManager.getConnection(URL, "sa", "");
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("""
                     select i.name, count(u.id) from item i
                     left join item_unit u on u.item_id = i.id and u.state = 'PRISTINE' and u.item_group_id is null
                     group by i.name order by i.name
                     """)) {
            while (rows.next()) {
                units.put(rows.getString(1), rows.getLong(2));
            }
        }
        return units;
    }
}
