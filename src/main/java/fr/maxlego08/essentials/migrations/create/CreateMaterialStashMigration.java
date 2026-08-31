package fr.maxlego08.essentials.migrations.create;

import fr.maxlego08.sarah.database.Migration;

public class CreateMaterialStashMigration extends Migration {
    @Override
    public void up() {
        create("%prefix%stash_materials", table -> {
            table.uuid("uuid").primary();
            table.text("data_json");
        });
    }
}
