package fr.maxlego08.essentials.storage.mongodb;

import com.google.gson.Gson;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.zutils.utils.ZUtils;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class MongoRepository extends ZUtils {

    protected final EssentialsPlugin plugin;
    private final MongoDatabase database;
    private final String collectionName;
    protected final Gson gson;

    public MongoRepository(EssentialsPlugin plugin, MongoDatabase database, String collectionName) {
        this.plugin = plugin;
        this.database = database;
        this.collectionName = collectionName;
        this.gson = new Gson();
    }

    public MongoCollection<Document> collection() {
        return database.getCollection(collectionName);
    }

    public String getCollectionName() {
        return collectionName;
    }

    protected void upsert(Document filter, Document document) {
        collection().replaceOne(filter, document, new com.mongodb.client.model.ReplaceOptions().upsert(true));
    }

    protected void insert(Document document) {
        collection().insertOne(document);
    }

    protected void insertMany(List<Document> documents) {
        collection().insertMany(documents);
    }

    protected void delete(Document filter) {
        collection().deleteOne(filter);
    }

    protected void deleteMany(Document filter) {
        collection().deleteMany(filter);
    }

    protected <T> T findOne(Document filter, Class<T> type) {
        Document doc = collection().find(filter).first();
        return doc != null ? gson.fromJson(doc.toJson(), type) : null;
    }

    protected <T> List<T> find(Document filter, Class<T> type) {
        List<T> result = new ArrayList<>();
        FindIterable<Document> docs = collection().find(filter);
        for (Document doc : docs) {
            result.add(gson.fromJson(doc.toJson(), type));
        }
        return result;
    }

    protected <T> List<T> findAll(Class<T> type) {
        return find(new Document(), type);
    }

    protected long count(Document filter) {
        return collection().countDocuments(filter);
    }

    protected long countAll() {
        return collection().countDocuments();
    }

    protected Document byUuid(UUID uuid) {
        return new Document("uuid", uuid.toString());
    }

    protected Document byField(String field, Object value) {
        return new Document(field, value);
    }

    protected Document toDocument(Object dto) {
        return Document.parse(gson.toJson(dto));
    }
}
