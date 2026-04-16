package com.freetime.geoweather.data

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

actual fun getDatabaseBuilder(): RoomDatabase.Builder<LocationDatabase> {
    // Testweise In-Memory Datenbank, um Datei-System-Probleme (SIGABRT) auszuschließen
    return Room.inMemoryDatabaseBuilder<LocationDatabase>()
}
