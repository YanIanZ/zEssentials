package fr.maxlego08.essentials.api.server;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MongoConfigTest {

    @Test
    @DisplayName("useUri returns true when uri is non-blank")
    void testUseUriTrue() {
        MongoConfiguration config = new MongoConfiguration(
            "mongodb://localhost:27017", "localhost", 27017, "", "", "test");
        assertTrue(config.useUri());
    }

    @Test
    @DisplayName("useUri returns false when uri is blank")
    void testUseUriFalse() {
        MongoConfiguration config = new MongoConfiguration(
            "", "localhost", 27017, "", "", "test");
        assertFalse(config.useUri());
    }

    @Test
    @DisplayName("useUri returns false when uri is null")
    void testUseUriNull() {
        MongoConfiguration config = new MongoConfiguration(
            null, "localhost", 27017, "", "", "test");
        assertFalse(config.useUri());
    }

    @Test
    @DisplayName("hasAuth returns true when user is non-blank")
    void testHasAuthTrue() {
        MongoConfiguration config = new MongoConfiguration(
            "", "localhost", 27017, "admin", "secret", "test");
        assertTrue(config.hasAuth());
    }

    @Test
    @DisplayName("hasAuth returns false when user is blank")
    void testHasAuthFalse() {
        MongoConfiguration config = new MongoConfiguration(
            "", "localhost", 27017, "", "", "test");
        assertFalse(config.hasAuth());
    }

    @Test
    @DisplayName("Individual fields are accessible")
    void testFieldAccess() {
        MongoConfiguration config = new MongoConfiguration(
            "", "mongo.example.com", 27018, "user", "pass", "zessentials");
        assertEquals("mongo.example.com", config.host());
        assertEquals(27018, config.port());
        assertEquals("user", config.user());
        assertEquals("pass", config.password());
        assertEquals("zessentials", config.database());
    }

    @Test
    @DisplayName("URI field is accessible")
    void testUriAccess() {
        MongoConfiguration config = new MongoConfiguration(
            "mongodb://user:pass@host:27017", "", 0, "", "", "db");
        assertEquals("mongodb://user:pass@host:27017", config.uri());
    }
}
