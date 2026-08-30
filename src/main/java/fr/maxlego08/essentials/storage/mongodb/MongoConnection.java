package fr.maxlego08.essentials.storage.mongodb;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.server.MongoConfiguration;

import java.util.Collections;

public class MongoConnection {

    private final MongoClient client;
    private final MongoDatabase database;

    public MongoConnection(MongoConfiguration config) {
        MongoClientSettings settings;

        if (config.useUri()) {
            settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(config.uri()))
                    .build();
        } else if (config.hasAuth()) {
            settings = MongoClientSettings.builder()
                    .applyToClusterSettings(builder -> builder.hosts(
                            Collections.singletonList(new ServerAddress(config.host(), config.port()))))
                    .credential(MongoCredential.createCredential(
                            config.user(), config.database(), config.password().toCharArray()))
                    .build();
        } else {
            settings = MongoClientSettings.builder()
                    .applyToClusterSettings(builder -> builder.hosts(
                            Collections.singletonList(new ServerAddress(config.host(), config.port()))))
                    .build();
        }

        this.client = MongoClients.create(settings);
        this.database = client.getDatabase(config.database());
    }

    public MongoDatabase getDatabase() {
        return database;
    }

    public boolean isValid() {
        try {
            this.client.listDatabaseNames().first();
            return true;
        } catch (MongoException e) {
            return false;
        }
    }

    public void close() {
        if (client != null) {
            client.close();
        }
    }
}
