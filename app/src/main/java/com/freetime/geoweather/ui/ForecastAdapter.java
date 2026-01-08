package com.freetime.geoweather.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.freetime.geoweather.R;
import com.freetime.geoweather.WeatherDetailActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.VH> {

    public static class DailyForecast {
        public String date;
        public double tempMax;
        public double tempMin;
        public int weatherCode;
    }

    private List<DailyForecast> items = new ArrayList<>();

    public void setItems(List<DailyForecast> list) {
        this.items = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_forecast, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        DailyForecast f = items.get(pos);

        h.date.setText(f.date);
        h.temp.setText(String.format(Locale.getDefault(), "%.1f° / %.1f°", f.tempMin, f.tempMax));
        h.desc.setText(WeatherDetailActivity.WEATHER_CODES.getOrDefault(f.weatherCode, "Unknown"));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView date, temp, desc;

        VH(View v) {
            super(v);
            date = v.findViewById(R.id.txtDate);
            temp = v.findViewById(R.id.txtTemp);
            desc = v.findViewById(R.id.txtDesc);
        }
    }
}
