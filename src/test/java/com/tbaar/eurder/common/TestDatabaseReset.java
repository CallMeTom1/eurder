package com.tbaar.eurder.common;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.concurrent.atomic.AtomicBoolean;

public class TestDatabaseReset implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final String TEST_DATABASE = "eurder_test";
    private static final AtomicBoolean ALREADY_RESET = new AtomicBoolean();

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        if (ALREADY_RESET.compareAndSet(false, true)) {
            dropTestDatabase(context.getEnvironment().getRequiredProperty("MONGODB_URI"));
        }
    }

    private static void dropTestDatabase(String uri) {
        ConnectionString connection = new ConnectionString(uri);
        if (!TEST_DATABASE.equals(connection.getDatabase())) {
            throw new IllegalStateException("The tests only run on the database " + TEST_DATABASE + ", not " + connection.getDatabase());
        }
        try (MongoClient client = MongoClients.create(connection)) {
            client.getDatabase(TEST_DATABASE).drop();
        }
    }
}
