package com.freetime.geoweather.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [LocationEntity::class, WeatherHistoryEntity::class], version = 9, exportSchema = false)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun weatherHistoryDao(): WeatherHistoryDao
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

fun getRoomDatabase(builder: RoomDatabase.Builder<WeatherDatabase>): WeatherDatabase =
    builder.addMigrations(MIGRATION_7_8, MIGRATION_8_9).fallbackToDestructiveMigration(true).build()
