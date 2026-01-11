package com.freetime.geoweather.data;

import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "locations")
public class LocationEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String name;
    private double latitude;
    private double longitude;

    @Nullable
    private Double temperature;

    @Nullable
    private Integer weatherCode;

    @Nullable
    private String weatherDescription;

    public LocationEntity(String name, double latitude, double longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    @Nullable
    public Double getTemperature() { return temperature; }
    public void setTemperature(@Nullable Double temperature) { this.temperature = temperature; }

    @Nullable
    public Integer getWeatherCode() { return weatherCode; }
    public void setWeatherCode(@Nullable Integer weatherCode) { this.weatherCode = weatherCode; }

    @Nullable
    public String getWeatherDescription() { return weatherDescription; }
    public void setWeatherDescription(@Nullable String weatherDescription) { this.weatherDescription = weatherDescription; }
}
