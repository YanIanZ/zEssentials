package fr.maxlego08.essentials.migrations.create;

import fr.maxlego08.sarah.database.Migration;

public class CreateReportsMigration extends Migration {
    @Override
    public void up() {
        create("%prefix%reports", table -> {
            table.autoIncrement("id").primary();
            table.uuid("reporter_uuid");
            table.string("reporter_name", 255);
            table.uuid("target_uuid");
            table.string("target_name", 255);
            table.text("reason");
            table.bigInt("created_at");
            table.bool("resolved");
        });
    }
}
