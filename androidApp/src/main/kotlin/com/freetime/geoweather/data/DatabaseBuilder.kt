package com.freetime.geoweather.data

import androidx.room.RoomDatabase

expect fun getDatabaseBuilder(): RoomDatabase.Builder<LocationDatabase>
