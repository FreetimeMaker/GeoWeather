package com.freetime.geoweather.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun getDatabaseBuilder(ctx: Context): RoomDatabase.Builder<WeatherDatabase> {
    val appContext = ctx.applicationContext
    val dbFile = appContext.getDatabasePath("weather_database.db")
    return Room.databaseBuilder<WeatherDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
}
