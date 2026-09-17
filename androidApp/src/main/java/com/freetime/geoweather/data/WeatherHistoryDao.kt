package com.freetime.geoweather.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherHistoryDao {
    @Query("SELECT * FROM weather_history WHERE location = :locationName ORDER BY timestamp DESC")
    fun getHistoryForLocation(locationName: String): Flow<List<WeatherHistoryEntity>>

    @Insert
    suspend fun insertHistory(history: WeatherHistoryEntity)

    @Query("DELETE FROM weather_history WHERE timestamp < :cutoffTime")
    suspend fun deleteOldHistory(cutoffTime: Long)
}
