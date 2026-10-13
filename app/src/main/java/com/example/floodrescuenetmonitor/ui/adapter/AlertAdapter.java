package com.example.floodrescuenetmonitor.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.floodrescuenetmonitor.R;
import com.example.floodrescuenetmonitor.data.AlertEvent;
import com.example.floodrescuenetmonitor.ui.StatusPill;

import java.util.ArrayList;
import java.util.List;

public class AlertAdapter extends RecyclerView.Adapter<AlertAdapter.VH> {

    private final List<AlertEvent> allItems;
    private List<AlertEvent> visible;

    public AlertAdapter(List<AlertEvent> items) {
        this.allItems = new ArrayList<>(items);
        this.visible = new ArrayList<>(items);
    }

    public void setItems(List<AlertEvent> items) {
        allItems.clear(); allItems.addAll(items);
    }

    public void filter(String category) {
        visible = new ArrayList<>();
        for (AlertEvent e : allItems) {
            if ("All".equals(category) || e.category.equals(category)) {
                visible.add(e);
            }
        }
        notifyDataSetChanged();
    }

    public int getVisibleCount() {
        return visible.size();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alert, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        AlertEvent e = visible.get(position);
        holder.title.setText(e.title);
        holder.time.setText(e.time);
        holder.description.setText(e.description);
        View.OnClickListener details = v -> new androidx.appcompat.app.AlertDialog.Builder(v.getContext())
                .setTitle(e.title).setMessage(e.time + "\n" + e.tag + "\n\n" + e.description)
                .setPositiveButton("Close", null).show();
        holder.itemView.setOnClickListener(details);
        View detailLink = holder.itemView.findViewById(R.id.tvDetails);
        if (detailLink != null) detailLink.setOnClickListener(details);

        int accent, bg, color, icon;
        android.content.Context ctx = holder.itemView.getContext();
        switch (e.tag) {
            case "DANGER":
                accent = R.color.red_danger; bg = R.color.red_danger_bg; color = R.color.red_danger;
                icon = R.drawable.ic_warning;
                break;
            case "DEPLOYED":
                accent = R.color.green_success; bg = R.color.green_success_bg; color = R.color.green_success;
                icon = R.drawable.ic_verified_user;
                break;
            case "WARNING":
                accent = R.color.amber_warning; bg = R.color.amber_warning_bg; color = R.color.amber_warning;
                icon = R.drawable.ic_info;
                break;
            default:
                accent = R.color.text_secondary; bg = R.color.card_border; color = R.color.text_secondary;
                icon = R.drawable.ic_notifications_none;
        }
        holder.accentBar.setBackgroundColor(ContextCompat.getColor(ctx, accent));
        holder.icon.setImageResource(icon);
        StatusPill.bind(holder.tag, e.tag, ContextCompat.getColor(ctx, bg), ContextCompat.getColor(ctx, color));
    }

    @Override
    public int getItemCount() {
        return visible.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        View accentBar;
        ImageView icon;
        TextView title, tag, time, description;

        VH(View itemView) {
            super(itemView);
            accentBar = itemView.findViewById(R.id.accentBar);
            icon = itemView.findViewById(R.id.ivIcon);
            title = itemView.findViewById(R.id.tvTitle);
            tag = itemView.findViewById(R.id.tvTag);
            time = itemView.findViewById(R.id.tvTime);
            description = itemView.findViewById(R.id.tvDescription);
        }
    }
}
