package com.freetime.geoweather.data
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
@Dao
interface WeatherHistoryDao {
    @Query("SELECT * FROM weather_history WHERE location = :locationName ORDER BY timestamp DESC")
    fun getHistoryForLocation(locationName: String): LiveData<List<WeatherHistoryEntity>>
    @Insert
    fun insertHistory(history: WeatherHistoryEntity)
    @Query("DELETE FROM weather_history WHERE timestamp < :cutoffTime")
    fun deleteOldHistory(cutoffTime: Long)
}
