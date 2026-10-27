package com.example.floodrescuenetmonitor.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.floodrescuenetmonitor.R;
import com.example.floodrescuenetmonitor.data.HistoryEntry;
import com.example.floodrescuenetmonitor.ui.StatusPill;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {

    private final List<HistoryEntry> items;

    public HistoryAdapter(List<HistoryEntry> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        HistoryEntry e = items.get(position);
        holder.title.setText(e.title);
        holder.time.setText(e.time);
        if (e.status.isEmpty()) { holder.status.setVisibility(View.GONE); return; }
        holder.status.setVisibility(View.VISIBLE);
        android.content.Context ctx = holder.itemView.getContext();
        int bg, color;
        switch (e.status.toUpperCase(java.util.Locale.ROOT)) {
            case "DANGER": bg = R.color.red_danger_bg; color = R.color.red_danger; break;
            case "WARNING": bg = R.color.amber_warning_bg; color = R.color.amber_warning; break;
            case "DEPLOYED": case "SUCCESS": bg = R.color.green_success_bg; color = R.color.green_success; break;
            default: bg = R.color.card_border; color = R.color.text_secondary;
        }
        StatusPill.bind(holder.status, e.status, ContextCompat.getColor(ctx, bg), ContextCompat.getColor(ctx, color));
        int h = (int) (6 * ctx.getResources().getDisplayMetrics().density), v = (int) (2 * ctx.getResources().getDisplayMetrics().density);
        holder.status.setPadding(h, v, h, v);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, time, status;

        VH(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvTitle);
            time = itemView.findViewById(R.id.tvTime);
            status = itemView.findViewById(R.id.tvStatus);
        }
    }
}
