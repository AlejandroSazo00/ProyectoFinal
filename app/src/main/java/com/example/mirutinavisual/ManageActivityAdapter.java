package com.example.mirutinavisual;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ManageActivityAdapter extends RecyclerView.Adapter<ManageActivityAdapter.ManageActivityViewHolder> {

    private List<Activity> activities;
    private OnActivityActionListener listener;

    public interface OnActivityActionListener {
        void onEditActivity(Activity activity);
        void onDeleteActivity(Activity activity);
        void onToggleActivity(Activity activity);
        void onSpeakActivity(Activity activity);
    }

    public ManageActivityAdapter(List<Activity> activities, OnActivityActionListener listener) {
        this.activities = activities;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ManageActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_manage_activity, parent, false);
        return new ManageActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ManageActivityViewHolder holder, int position) {
        Activity activity = activities.get(position);
        
        // Configurar datos básicos
        holder.activityNameText.setText(activity.getName());
        holder.activityTimeText.setText(activity.getTime());
        
        // Cargar pictograma
        Glide.with(holder.itemView.getContext())
                .load(activity.getPictogramUrl())
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_error)
                .into(holder.pictogramImageView);
        
        // Configurar switch de estado
        holder.statusSwitch.setChecked(activity.isCompleted());
        holder.statusSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onToggleActivity(activity);
            }
        });
        
        // Configurar apariencia según estado
        if (activity.isCompleted()) {
            holder.activityNameText.setPaintFlags(
                holder.activityNameText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.statusText.setText("✅ Completada");
            holder.statusText.setTextColor(
                holder.itemView.getContext().getResources().getColor(R.color.card_today));
        } else {
            holder.activityNameText.setPaintFlags(
                holder.activityNameText.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.statusText.setText("⏳ Pendiente");
            holder.statusText.setTextColor(
                holder.itemView.getContext().getResources().getColor(R.color.text_secondary));
        }
        
        // Listeners para botones
        holder.editButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditActivity(activity);
            }
        });
        
        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteActivity(activity);
            }
        });
        
        holder.speakButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSpeakActivity(activity);
            }
        });
        
        // Click en toda la tarjeta para hablar
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSpeakActivity(activity);
            }
        });
    }

    @Override
    public int getItemCount() {
        return activities.size();
    }

    public static class ManageActivityViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView pictogramImageView;
        TextView activityNameText, activityTimeText, statusText;
        Switch statusSwitch;
        ImageButton editButton, deleteButton, speakButton;

        public ManageActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.manageActivityCardView);
            pictogramImageView = itemView.findViewById(R.id.pictogramImageView);
            activityNameText = itemView.findViewById(R.id.activityNameText);
            activityTimeText = itemView.findViewById(R.id.activityTimeText);
            statusText = itemView.findViewById(R.id.statusText);
            statusSwitch = itemView.findViewById(R.id.statusSwitch);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            speakButton = itemView.findViewById(R.id.speakButton);
        }
    }
}
