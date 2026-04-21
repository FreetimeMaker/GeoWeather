package com.freetime.geoweather.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.io.IOException

@Database(entities = [LocationEntity::class], version = 6, exportSchema = false)
@ConstructedBy(LocationDatabaseConstructor::class)
abstract class LocationDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
}

// The following is needed for Room KMP to work
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object LocationDatabaseConstructor : RoomDatabaseConstructor<LocationDatabase> {
    override fun initialize(): LocationDatabase
}

internal const val dbFileName = "location_database.db"

fun getDatabase(builder: RoomDatabase.Builder<LocationDatabase>): LocationDatabase {
    return builder
        .fallbackToDestructiveMigration(dropAllTables = true)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
