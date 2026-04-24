package io.github.freetimemaker.geoweather.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
data class LocationRoomEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val selected: Boolean = false,
    val isDefault: Boolean = false
) {
    fun toLocationEntity() = LocationEntity(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        selected = selected,
        isDefault = isDefault
    )
}
