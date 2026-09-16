package com.freetime.geoweather.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LocationEntity::class, WeatherHistoryEntity::class], version = 7, exportSchema = false)
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

fun getRoomDatabase(builder: RoomDatabase.Builder<WeatherDatabase>): WeatherDatabase =
    builder.fallbackToDestructiveMigration(true).build()
