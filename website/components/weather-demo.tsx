'use client';

import * as React from 'react';
import { Search, ThermometerSun, Droplets, Wind, Cloud, Loader2 } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

interface WeatherData {
  current: {
    temperature_2m: number;
    relative_humidity_2m: number;
    wind_speed_10m: number;
    weather_code: number;
  };
  daily: {
    temperature_2m_max: number[];
    temperature_2m_min: number[];
    time: string[];
  };
}

export function WeatherDemo() {
  const [city, setCity] = React.useState('');
  const [loading, setLoading] = React.useState(false);
  const [weather, setWeather] = React.useState<WeatherData | null>(null);
  const [error, setError] = React.useState<string | null>(null);

  const fetchWeather = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!city) return;

    setLoading(true);
    setError(null);

    try {
      // 1. Geocoding
      const geoRes = await fetch(`https://geocoding-api.open-meteo.com/v1/search?name=${encodeURIComponent(city)}&count=1&language=en&format=json`);
      const geoData = await geoRes.json();

      if (!geoData.results || geoData.results.length === 0) {
        throw new Error('City not found');
      }

      const { latitude, longitude, name } = geoData.results[0];

      // 2. Weather
      const weatherRes = await fetch(`https://api.open-meteo.com/v1/forecast?latitude=${latitude}&longitude=${longitude}&current=temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code&daily=temperature_2m_max,temperature_2m_min&timezone=auto`);
      const weatherData = await weatherRes.json();

      setWeather(weatherData);
      setCity(name);
    } catch (err: any) {
      setError(err.message || 'Failed to fetch weather');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card className="w-full max-w-2xl mx-auto overflow-hidden">
      <CardHeader className="bg-primary/5">
        <CardTitle className="flex items-center gap-2">
          <Cloud className="h-6 w-6 text-primary" />
          Live Weather Demo
        </CardTitle>
        <CardDescription>
          Search any city to see current conditions powered by Open-Meteo
        </CardDescription>
      </CardHeader>
      <CardContent className="p-6">
        <form onSubmit={fetchWeather} className="flex gap-2 mb-6">
          <Input
            placeholder="Enter city name (e.g. Zurich, Tokyo...)"
            value={city}
            onChange={(e) => setCity(e.target.value)}
            className="flex-1"
          />
          <Button type="submit" disabled={loading}>
            {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <Search className="h-4 w-4" />}
            <span className="ml-2 hidden sm:inline">Search</span>
          </Button>
        </form>

        {error && (
          <div className="bg-destructive/10 text-destructive p-3 rounded-md text-sm mb-4">
            {error}
          </div>
        )}

        {weather ? (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="flex flex-col items-center justify-center p-4 bg-accent/30 rounded-lg">
              <span className="text-5xl font-bold mb-2">
                {Math.round(weather.current.temperature_2m)}°C
              </span>
              <span className="text-muted-foreground capitalize">
                Current Temperature in {city}
              </span>
            </div>
            <div className="space-y-4">
              <div className="flex items-center gap-3">
                <ThermometerSun className="h-5 w-5 text-orange-500" />
                <div className="flex-1">
                  <div className="text-sm font-medium">Daily Range</div>
                  <div className="text-xs text-muted-foreground">
                    Low: {Math.round(weather.daily.temperature_2m_min[0])}°C / High: {Math.round(weather.daily.temperature_2m_max[0])}°C
                  </div>
                </div>
              </div>
              <div className="flex items-center gap-3">
                <Droplets className="h-5 w-5 text-blue-500" />
                <div className="flex-1">
                  <div className="text-sm font-medium">Humidity</div>
                  <div className="text-xs text-muted-foreground">
                    {weather.current.relative_humidity_2m}%
                  </div>
                </div>
              </div>
              <div className="flex items-center gap-3">
                <Wind className="h-5 w-5 text-gray-500" />
                <div className="flex-1">
                  <div className="text-sm font-medium">Wind Speed</div>
                  <div className="text-xs text-muted-foreground">
                    {weather.current.wind_speed_10m} km/h
                  </div>
                </div>
              </div>
            </div>
          </div>
        ) : !loading && (
          <div className="text-center py-12 text-muted-foreground border-2 border-dashed rounded-lg">
            Search for a city to see the weather magic
          </div>
        )}
      </CardContent>
    </Card>
  );
}
