package com.freetime.geoweather.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object WeatherDatabaseConstructor : RoomDatabaseConstructor<WeatherDatabase>

@Database(entities = [LocationEntity::class, WeatherHistoryEntity::class], version = 7, exportSchema = false)
@ConstructedBy(WeatherDatabaseConstructor::class)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun weatherHistoryDao(): WeatherHistoryDao
}

fun getRoomDatabase(
    builder: RoomDatabase.Builder<WeatherDatabase>
): WeatherDatabase {
    return builder
        .fallbackToDestructiveMigration(true)
        .build()
}
