package com.freetime.geoweather.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.freetime.geoweather.R;
import com.freetime.geoweather.WeatherCodes;
import com.freetime.geoweather.WeatherIconMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DailyAdapter extends RecyclerView.Adapter<DailyAdapter.VH> {

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
                .inflate(R.layout.item_daily, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        DailyForecast f = items.get(pos);

        h.txtDate.setText(f.date.substring(5)); // "MM‑DD"
        h.txtTemp.setText(String.format(Locale.getDefault(), "%.0f° / %.0f°", f.tempMin, f.tempMax));
        h.txtDesc.setText(WeatherCodes.getDescription(f.weatherCode));
        h.imgIcon.setImageResource(WeatherIconMapper.getWeatherIcon(f.weatherCode));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView txtDate, txtTemp, txtDesc;
        ImageView imgIcon;

        VH(View v) {
            super(v);
            txtDate = v.findViewById(R.id.txtDate);
            txtTemp = v.findViewById(R.id.txtTemp);
            txtDesc = v.findViewById(R.id.txtDesc);
            imgIcon = v.findViewById(R.id.imgIcon);
        }
    }
}
