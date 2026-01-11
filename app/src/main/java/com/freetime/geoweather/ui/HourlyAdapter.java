package com.freetime.geoweather.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.freetime.geoweather.R;
import com.freetime.geoweather.WeatherIconMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HourlyAdapter extends RecyclerView.Adapter<HourlyAdapter.VH> {

    public static class HourlyForecast {
        public String time;
        public double temperature;
        public int weatherCode;
    }

    private List<HourlyForecast> items = new ArrayList<>();

    public void setItems(List<HourlyForecast> list) {
        this.items = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hourly, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        HourlyForecast f = items.get(pos);

        h.txtTime.setText(f.time.substring(11)); // "HH:MM"
        h.txtTemp.setText(String.format(Locale.getDefault(), "%.1f°C", f.temperature));
        h.imgIcon.setImageResource(WeatherIconMapper.getWeatherIcon(f.weatherCode));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView txtTime, txtTemp;
        ImageView imgIcon;

        VH(View v) {
            super(v);
            txtTime = v.findViewById(R.id.txtTime);
            txtTemp = v.findViewById(R.id.txtTemp);
            imgIcon = v.findViewById(R.id.imgIcon);
        }
    }
}
