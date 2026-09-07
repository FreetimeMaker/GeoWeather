package com.freetime.geoweather.data

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

fun getDatabaseBuilder(): RoomDatabase.Builder<WeatherDatabase> {
    val dbFile = File(System.getProperty("java.io.tmpdir"), "weather_database.db")
    return Room.databaseBuilder<WeatherDatabase>(
        name = dbFile.absolutePath,
    )
}
