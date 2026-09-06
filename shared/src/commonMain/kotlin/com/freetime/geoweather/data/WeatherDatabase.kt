package com.freetime.geoweather.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(entities = [LocationEntity::class, WeatherHistoryEntity::class], version = 7)
@ConstructedBy(WeatherDatabaseConstructor::class)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun weatherHistoryDao(): WeatherHistoryDao
}

// The Room compiler generates the implementation of this constructor
expect object WeatherDatabaseConstructor : RoomDatabaseConstructor<WeatherDatabase>

fun getRoomDatabase(
    builder: RoomDatabase.Builder<WeatherDatabase>
): WeatherDatabase {
    return builder
        .fallbackToDestructiveMigration(true)
        .build()
}
