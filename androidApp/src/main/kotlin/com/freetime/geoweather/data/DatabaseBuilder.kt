package com.freetime.geoweather.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

lateinit var appContext: Context

fun initContext(context: Context) {
    appContext = context
}

fun getDatabaseBuilder(): RoomDatabase.Builder<LocationDatabase> {
    val dbFile = appContext.getDatabasePath(dbFileName)
    return Room.databaseBuilder<LocationDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
}
