package fr.maxlego08.essentials.migrations.create;

import fr.maxlego08.sarah.database.Migration;

public class CreateEnderChestMigration extends Migration {
    @Override
    public void up() {
        create("%prefix%enderchest", table -> {
            table.uuid("uuid").primary();
            table.text("data_json");
        });
    }
}
