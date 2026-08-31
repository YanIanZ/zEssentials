package fr.maxlego08.essentials.migrations.create;

import fr.maxlego08.sarah.database.Migration;

public class CreateNotesMigration extends Migration {
    @Override
    public void up() {
        create("%prefix%notes", table -> {
            table.autoIncrement("id").primary();
            table.uuid("player_uuid");
            table.uuid("staff_uuid");
            table.string("staff_name", 255);
            table.bigInt("created_at");
            table.text("content");
        });
    }
}
