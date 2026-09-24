package com.freetime.geoweather.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "forecast_snapshots")
data class ForecastSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val locationId: Long,
    val targetTime: String,
    val createdAt: Long,
    val forecastTemperature: Double,
    val actualTemperature: Double? = null,
    val evaluatedAt: Long? = null
)

data class ForecastAccuracyBucket(
    val horizonHours: Int,
    val samples: Int,
    val meanAbsoluteError: Double
)

@Dao
interface ForecastSnapshotDao {
    @Insert
    suspend fun insert(snapshot: ForecastSnapshotEntity)

    @Query("SELECT * FROM forecast_snapshots WHERE locationId = :locationId AND targetTime = :targetTime AND ABS(createdAt - :createdAt) < 1800000 LIMIT 1")
    suspend fun findNearbySnapshot(locationId: Long, targetTime: String, createdAt: Long): ForecastSnapshotEntity?

    @Query("SELECT * FROM forecast_snapshots WHERE locationId = :locationId AND actualTemperature IS NULL")
    suspend fun getPending(locationId: Long): List<ForecastSnapshotEntity>

    @Query("UPDATE forecast_snapshots SET actualTemperature = :actual, evaluatedAt = :evaluatedAt WHERE id = :id")
    suspend fun evaluate(id: Long, actual: Double, evaluatedAt: Long)

    @Query("SELECT * FROM forecast_snapshots WHERE locationId = :locationId AND actualTemperature IS NOT NULL ORDER BY evaluatedAt DESC LIMIT 500")
    suspend fun getEvaluated(locationId: Long): List<ForecastSnapshotEntity>

    @Query("DELETE FROM forecast_snapshots WHERE createdAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}
