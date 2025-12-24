package com.freetime.geoweather.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.freetime.geoweather.R;
import com.freetime.geoweather.data.LocationEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocationsAdapter extends RecyclerView.Adapter<LocationsAdapter.LocationViewHolder> {

    private List<LocationEntity> locations = new ArrayList<>();
    private OnItemDeleteListener onDeleteListener;
    private OnItemClickListener onItemClickListener;

    public interface OnItemDeleteListener {
        void onDeleteClick(LocationEntity location);
    }

    public interface OnItemClickListener {
        void onItemClick(LocationEntity location);
    }

    public void setOnItemDeleteListener(OnItemDeleteListener listener) {
        this.onDeleteListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setItems(List<LocationEntity> newLocations) {
        this.locations = newLocations;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_location, parent, false);
        return new LocationViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull LocationViewHolder holder, int position) {
        LocationEntity current = locations.get(position);
        holder.tvLocationName.setText(current.getName());
        holder.tvLocationCoords.setText(String.format(Locale.getDefault(), "Lat: %.4f, Lon: %.4f",
                current.getLatitude(), current.getLongitude()));

        holder.btnDeleteLocation.setOnClickListener(v -> {
            if (onDeleteListener != null) {
                onDeleteListener.onDeleteClick(current);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(current);
            }
        });
    }

    @Override
    public int getItemCount() {
        return locations.size();
    }

    static class LocationViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvLocationName;
        private final TextView tvLocationCoords;
        private final ImageButton btnDeleteLocation;

        private LocationViewHolder(View itemView) {
            super(itemView);
            tvLocationName = itemView.findViewById(R.id.tvLocationName);
            tvLocationCoords = itemView.findViewById(R.id.tvLocationCoords);
            btnDeleteLocation = itemView.findViewById(R.id.btnDeleteLocation);
        }
    }
}