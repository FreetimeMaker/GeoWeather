package com.freetime.geoweather.data

import androidx.room.Room
import androidx.room.RoomDatabase

// Note: For WasmJS, Room might require specific setup or might not be fully supported in alpha11 
// with the bundled sqlite driver in the same way. 
// For now, providing a placeholder that might need refinement.
actual fun getDatabaseBuilder(): RoomDatabase.Builder<LocationDatabase> {
    throw UnsupportedOperationException("Room for WasmJS is not yet fully configured")
}
