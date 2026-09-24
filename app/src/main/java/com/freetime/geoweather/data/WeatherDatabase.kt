package com.freetime.geoweather.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [LocationEntity::class, WeatherHistoryEntity::class, ForecastSnapshotEntity::class], version = 10, exportSchema = false)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun weatherHistoryDao(): WeatherHistoryDao
    abstract fun forecastSnapshotDao(): ForecastSnapshotDao
}

fun getDatabaseBuilder(ctx: Context): RoomDatabase.Builder<WeatherDatabase> {
    val appContext = ctx.applicationContext
    return Room.databaseBuilder(
        appContext,
        WeatherDatabase::class.java,
        "weather_database.db"
    )
}

private val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE locations ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
    }
}

private val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE locations ADD COLUMN offlinePackEnabled INTEGER NOT NULL DEFAULT 0")
    }
}

private val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS forecast_snapshots (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                locationId INTEGER NOT NULL,
                issuedAt INTEGER NOT NULL,
                targetEpochMillis INTEGER NOT NULL,
                horizonHours INTEGER NOT NULL,
                forecastTemperature REAL NOT NULL,
                actualTemperature REAL,
                evaluatedAt INTEGER
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_forecast_snapshots_locationId_targetEpochMillis_horizonHours ON forecast_snapshots(locationId, targetEpochMillis, horizonHours)")
    }
}

fun getRoomDatabase(builder: RoomDatabase.Builder<WeatherDatabase>): WeatherDatabase =
    builder.addMigrations(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10).fallbackToDestructiveMigration(true).build()
