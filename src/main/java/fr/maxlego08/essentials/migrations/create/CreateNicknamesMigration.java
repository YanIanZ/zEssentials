package fr.maxlego08.essentials.migrations.create;

import fr.maxlego08.sarah.database.Migration;

public class CreateNicknamesMigration extends Migration {
    @Override
    public void up() {
        create("%prefix%nicknames", table -> {
            table.uuid("uuid").primary();
            table.string("nickname", 255);
        });
    }
}
