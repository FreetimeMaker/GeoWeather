package com.freetime.geoweather.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(
    tableName = "forecast_snapshots",
    indices = [Index(value = ["locationId", "targetEpochMillis", "horizonHours"])]
)
data class ForecastSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val locationId: Long,
    val issuedAt: Long,
    val targetEpochMillis: Long,
    val horizonHours: Int,
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

    @Query("SELECT * FROM forecast_snapshots WHERE locationId = :locationId AND targetEpochMillis = :targetEpochMillis AND horizonHours = :horizonHours AND ABS(issuedAt - :issuedAt) < 1800000 LIMIT 1")
    suspend fun findNearbySnapshot(locationId: Long, targetEpochMillis: Long, horizonHours: Int, issuedAt: Long): ForecastSnapshotEntity?

    @Query("SELECT * FROM forecast_snapshots WHERE locationId = :locationId AND actualTemperature IS NULL")
    suspend fun getPending(locationId: Long): List<ForecastSnapshotEntity>

    @Query("UPDATE forecast_snapshots SET actualTemperature = :actual, evaluatedAt = :evaluatedAt WHERE id = :id")
    suspend fun evaluate(id: Long, actual: Double, evaluatedAt: Long)

    @Query("SELECT * FROM forecast_snapshots WHERE locationId = :locationId AND actualTemperature IS NOT NULL ORDER BY evaluatedAt DESC LIMIT 500")
    suspend fun getEvaluated(locationId: Long): List<ForecastSnapshotEntity>

    @Query("DELETE FROM forecast_snapshots WHERE issuedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}
