package fr.maxlego08.essentials.storage.mongodb.repos;

import com.mongodb.client.MongoDatabase;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.dto.StepDTO;
import fr.maxlego08.essentials.api.steps.Step;
import fr.maxlego08.essentials.storage.mongodb.MongoRepository;
import org.bson.Document;

import java.util.Date;
import java.util.UUID;

public class MongoUserStepRepository extends MongoRepository {
    public MongoUserStepRepository(EssentialsPlugin plugin, MongoDatabase database) {
        super(plugin, database, "steps");
    }

    public void createStep(UUID uniqueId, Step step, long playTime) {
        Document doc = new Document("unique_id", uniqueId.toString())
                .append("step_name", step.name())
                .append("play_time_start", playTime);
        insert(doc);
    }

    public void finishStep(UUID uniqueId, Step step, String data, long playTimeBetween, long playTimeEnd) {
        Document filter = new Document("unique_id", uniqueId.toString()).append("step_name", step.name());
        Document set = new Document("data", data)
                .append("play_time_end", playTimeEnd)
                .append("play_time_between", playTimeBetween)
                .append("finished_at", new Date());
        collection().updateOne(filter, new Document("$set", set));
    }

    public StepDTO selectStep(UUID uniqueId, Step step) {
        return findOne(new Document("unique_id", uniqueId.toString()).append("step_name", step.name()), StepDTO.class);
    }
}
