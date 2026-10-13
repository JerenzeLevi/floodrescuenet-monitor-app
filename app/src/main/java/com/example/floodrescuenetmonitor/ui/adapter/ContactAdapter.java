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
import com.example.floodrescuenetmonitor.data.EmergencyContact;

import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.VH> {

    private static final int[] ICONS = {R.drawable.ic_gpp_good, R.drawable.ic_home, R.drawable.ic_monitor_heart, R.drawable.ic_water_drop};

    private final List<EmergencyContact> items;

    public ContactAdapter(List<EmergencyContact> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        EmergencyContact c = items.get(position);
        holder.name.setText(c.name);
        holder.subtitle.setText(c.subtitle);
        holder.number.setText(c.number.isEmpty() ? "Not configured" : c.number);
        holder.row.setOnClickListener(v -> {
            if (!c.number.matches("[+0-9() -]{3,25}")) return;
            android.content.Intent dial = new android.content.Intent(android.content.Intent.ACTION_DIAL,
                    android.net.Uri.fromParts("tel", c.number, null));
            try { v.getContext().startActivity(dial); }
            catch (android.content.ActivityNotFoundException e) {
                android.widget.Toast.makeText(v.getContext(), "No dialer is installed on this device.", android.widget.Toast.LENGTH_LONG).show();
            }
        });

        boolean danger = position == 0;
        boolean indigoTint = position == 1;
        android.content.Context ctx = holder.itemView.getContext();
        int bg = danger ? R.drawable.bg_banner_red : indigoTint ? R.drawable.bg_contact_indigo_tint : R.drawable.bg_contact_white;
        holder.row.setBackgroundResource(bg);
        holder.avatarBox.setBackgroundResource(danger ? R.drawable.bg_icon_box_red : indigoTint ? R.drawable.bg_icon_box_indigo : R.drawable.bg_summary_gray);
        holder.avatarIcon.setImageResource(ICONS[Math.min(position, ICONS.length - 1)]);
        holder.avatarIcon.setImageTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(ctx,
                danger || indigoTint ? R.color.white : R.color.text_secondary)));
        holder.callButton.setBackgroundResource(danger ? R.drawable.bg_call_circle_red : R.drawable.bg_call_circle_white);
        holder.callIcon.setImageTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(ctx,
                danger ? R.color.white : R.color.indigo)));
        holder.name.setTextColor(ContextCompat.getColor(ctx, danger ? R.color.red_danger : R.color.text_primary));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        LinearLayout row;
        View avatarBox, callButton;
        android.widget.ImageView avatarIcon, callIcon;
        TextView name, subtitle, number;

        VH(View itemView) {
            super(itemView);
            row = itemView.findViewById(R.id.rowContact);
            avatarBox = itemView.findViewById(R.id.boxAvatar);
            avatarIcon = itemView.findViewById(R.id.ivContactIcon);
            callButton = itemView.findViewById(R.id.btnCall);
            callIcon = itemView.findViewById(R.id.ivCall);
            name = itemView.findViewById(R.id.tvName);
            subtitle = itemView.findViewById(R.id.tvSubtitle);
            number = itemView.findViewById(R.id.tvNumber);
        }
    }
}
