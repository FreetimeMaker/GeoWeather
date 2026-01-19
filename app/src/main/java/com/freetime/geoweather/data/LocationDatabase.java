package com.freetime.geoweather.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {LocationEntity.class}, version = 2, exportSchema = false)
public abstract class LocationDatabase extends RoomDatabase {

    public abstract LocationDao locationDao();

    private static volatile LocationDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Create the new table
            database.execSQL(
                "CREATE TABLE `locations_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `weatherData` TEXT, `lastUpdated` INTEGER NOT NULL DEFAULT 0)");

            // Copy the data
            database.execSQL(
                "INSERT INTO `locations_new` (`id`, `name`, `latitude`, `longitude`) SELECT `id`, `name`, `latitude`, `longitude` FROM `locations`");

            // Remove the old table
            database.execSQL("DROP TABLE `locations`");

            // Rename the new table to the old table's name
            database.execSQL("ALTER TABLE `locations_new` RENAME TO `locations`");
        }
    };

    public static LocationDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (LocationDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            LocationDatabase.class, "location_database")
                            .addMigrations(MIGRATION_1_2)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}