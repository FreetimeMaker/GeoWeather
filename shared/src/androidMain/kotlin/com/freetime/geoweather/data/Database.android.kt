package com.freetime.geoweather.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

fun getDatabaseBuilder(ctx: Context): RoomDatabase.Builder<WeatherDatabase> {
    val appContext = ctx.applicationContext
    val dbFile = appContext.getDatabasePath("location_database")
    return Room.databaseBuilder<WeatherDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    ).setDriver(BundledSQLiteDriver())
}
