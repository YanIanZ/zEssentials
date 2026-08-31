package fr.maxlego08.essentials.migrations.create;

import fr.maxlego08.sarah.database.Migration;

public class CreateReputationsMigration extends Migration {
    @Override
    public void up() {
        create("%prefix%reputations", table -> {
            table.uuid("uuid").primary();
            table.text("data_json");
        });
    }
}
