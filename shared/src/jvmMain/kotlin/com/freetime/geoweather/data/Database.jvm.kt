package com.freetime.geoweather.data

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File

fun getDatabaseBuilder(): RoomDatabase.Builder<WeatherDatabase> {
    val dbFile = File(System.getProperty("java.io.tmpdir"), "location_database.db")
    return Room.databaseBuilder<WeatherDatabase>(
        name = dbFile.absolutePath,
    ).setDriver(BundledSQLiteDriver())
}
