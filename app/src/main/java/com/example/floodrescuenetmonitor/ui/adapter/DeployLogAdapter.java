package com.example.floodrescuenetmonitor.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.floodrescuenetmonitor.R;
import com.example.floodrescuenetmonitor.data.DeploymentLog;
import com.example.floodrescuenetmonitor.ui.StatusPill;

import java.util.List;

/** Renders the simple deploy-log rows used on the Manual Deploy screen. */
public class DeployLogAdapter extends RecyclerView.Adapter<DeployLogAdapter.VH> {

    private final List<DeploymentLog> items;

    public DeployLogAdapter(List<DeploymentLog> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_deploy_log, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        DeploymentLog log = items.get(position);
        android.content.Context ctx = holder.itemView.getContext();
        holder.location.setText(log.location);
        holder.meta.setText("🕐 " + log.time + "   👤 " + log.initiator);

        boolean success = "Success".equals(log.status);
        boolean failed = "Failed".equals(log.status);
        int bg = ContextCompat.getColor(ctx, success ? R.color.green_success_bg : failed ? R.color.red_danger_bg : R.color.card_border);
        int color = ContextCompat.getColor(ctx, success ? R.color.green_success : failed ? R.color.red_danger : R.color.text_secondary);
        StatusPill.bind(holder.status, log.status, bg, color);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView location, meta, status;

        VH(View itemView) {
            super(itemView);
            location = itemView.findViewById(R.id.tvLocation);
            meta = itemView.findViewById(R.id.tvMeta);
            status = itemView.findViewById(R.id.tvStatus);
        }
    }
}
