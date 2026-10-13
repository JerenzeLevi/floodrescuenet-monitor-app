package com.example.floodrescuenetmonitor.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.floodrescuenetmonitor.R;
import com.example.floodrescuenetmonitor.data.DeploymentLog;
import com.example.floodrescuenetmonitor.ui.StatusPill;

import java.util.List;

/** Renders the detailed deployment cards used on the Recent Deployments screen. */
public class DeploymentLogAdapter extends RecyclerView.Adapter<DeploymentLogAdapter.VH> {

    private final List<DeploymentLog> items;

    public DeploymentLogAdapter(List<DeploymentLog> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_deployment_detail, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        DeploymentLog log = items.get(position);
        android.content.Context ctx = holder.itemView.getContext();

        holder.time.setText("🕐 " + log.time);
        holder.location.setText(log.location);
        holder.initiator.setText(log.initiator);
        holder.unitId.setText(log.unitId);

        int bg, color;
        switch (log.status) {
            case "Success":
                bg = R.color.green_success_bg; color = R.color.green_success; break;
            case "Failed":
                bg = R.color.red_danger_bg; color = R.color.red_danger; break;
            default:
                bg = R.color.card_border; color = R.color.text_secondary;
        }
        StatusPill.bind(holder.status, log.status, ContextCompat.getColor(ctx, bg), ContextCompat.getColor(ctx, color));

        if (log.cycleTime != null && !log.cycleTime.isEmpty()) {
            holder.rowCycleTime.setVisibility(View.VISIBLE);
            holder.cycleTime.setText(log.cycleTime);
        } else {
            holder.rowCycleTime.setVisibility(View.INVISIBLE);
        }

        if (log.errorMessage != null && !log.errorMessage.isEmpty()) {
            holder.rowError.setVisibility(View.VISIBLE);
            holder.error.setText(log.errorMessage);
        } else {
            holder.rowError.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView time, status, location, initiator, unitId, cycleTime, error;
        LinearLayout rowCycleTime, rowError;

        VH(View itemView) {
            super(itemView);
            time = itemView.findViewById(R.id.tvTime);
            status = itemView.findViewById(R.id.tvStatus);
            location = itemView.findViewById(R.id.tvLocation);
            initiator = itemView.findViewById(R.id.tvInitiator);
            unitId = itemView.findViewById(R.id.tvUnitId);
            cycleTime = itemView.findViewById(R.id.tvCycleTime);
            error = itemView.findViewById(R.id.tvError);
            rowCycleTime = itemView.findViewById(R.id.rowCycleTime);
            rowError = itemView.findViewById(R.id.rowError);
        }
    }
}
