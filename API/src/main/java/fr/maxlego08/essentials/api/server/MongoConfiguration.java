package fr.maxlego08.essentials.api.server;

public record MongoConfiguration(
        String uri,
        String host,
        int port,
        String user,
        String password,
        String database
) {
    public boolean useUri() {
        return uri != null && !uri.isBlank();
    }

    public boolean hasAuth() {
        return user != null && !user.isBlank();
    }
}
