package com.example.floodrescuenetmonitor.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.floodrescuenetmonitor.R;
import com.example.floodrescuenetmonitor.data.ActivityLogEntry;
import com.example.floodrescuenetmonitor.ui.StatusPill;

import java.util.List;

public class ActivityLogAdapter extends RecyclerView.Adapter<ActivityLogAdapter.VH> {

    private final List<ActivityLogEntry> items;

    public ActivityLogAdapter(List<ActivityLogEntry> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_activity_log, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ActivityLogEntry e = items.get(position);
        holder.time.setText(e.time);
        holder.level.setText(e.level);

        int bg, color;
        switch (e.trend) {
            case "RISING":
                bg = ContextCompat.getColor(holder.itemView.getContext(), R.color.red_danger_bg);
                color = ContextCompat.getColor(holder.itemView.getContext(), R.color.red_danger);
                break;
            case "RECEDING":
                bg = ContextCompat.getColor(holder.itemView.getContext(), R.color.green_success_bg);
                color = ContextCompat.getColor(holder.itemView.getContext(), R.color.green_success);
                break;
            default:
                bg = ContextCompat.getColor(holder.itemView.getContext(), R.color.card_border);
                color = ContextCompat.getColor(holder.itemView.getContext(), R.color.text_secondary);
        }
        StatusPill.bind(holder.trend, e.trend, bg, color);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView time, level, trend;

        VH(View itemView) {
            super(itemView);
            time = itemView.findViewById(R.id.tvTime);
            level = itemView.findViewById(R.id.tvLevel);
            trend = itemView.findViewById(R.id.tvTrend);
        }
    }
}
