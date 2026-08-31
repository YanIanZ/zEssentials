package fr.maxlego08.essentials.migrations.create;

import fr.maxlego08.sarah.database.Migration;

public class CreateChatPreferencesMigration extends Migration {
    @Override
    public void up() {
        create("%prefix%chat_preferences", table -> {
            table.uuid("uuid").primary();
            table.text("data_json");
        });
    }
}
