package com.freetime.geoweather.data

import androidx.room.Database
import androidx.room.RoomDatabase
import kotlinx.coroutines.Dispatchers

@Database(entities = [LocationRoomEntity::class], version = 6, exportSchema = false)
abstract class LocationDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationRoomDao
}

internal const val dbFileName = "location_database.db"

fun getDatabase(builder: RoomDatabase.Builder<LocationDatabase>): LocationDatabase {
    return builder
        .fallbackToDestructiveMigration(dropAllTables = true)
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
