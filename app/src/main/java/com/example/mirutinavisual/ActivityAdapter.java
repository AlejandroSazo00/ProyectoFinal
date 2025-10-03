package com.example.mirutinavisual;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder> {

    private List<Activity> activities;
    private OnActivityClickListener listener;

    public interface OnActivityClickListener {
        void onActivityClick(Activity activity);
        void onActivityComplete(Activity activity);
        void onActivitySpeak(Activity activity);
    }

    public ActivityAdapter(List<Activity> activities, OnActivityClickListener listener) {
        this.activities = activities;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_activity, parent, false);
        return new ActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
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
        
        // Configurar estado completado
        if (activity.isCompleted()) {
            holder.cardView.setCardBackgroundColor(
                holder.itemView.getContext().getResources().getColor(R.color.card_today));
            holder.activityNameText.setPaintFlags(
                holder.activityNameText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.statusText.setText("✅ Completada");
            holder.statusText.setTextColor(
                holder.itemView.getContext().getResources().getColor(android.R.color.white));
            holder.completeButton.setVisibility(View.GONE);
        } else {
            holder.cardView.setCardBackgroundColor(
                holder.itemView.getContext().getResources().getColor(R.color.background_white));
            holder.activityNameText.setPaintFlags(
                holder.activityNameText.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.statusText.setText("⏳ Pendiente");
            holder.statusText.setTextColor(
                holder.itemView.getContext().getResources().getColor(R.color.text_secondary));
            holder.completeButton.setVisibility(View.VISIBLE);
        }
        
        // Listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onActivityClick(activity);
            }
        });
        
        holder.completeButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onActivityComplete(activity);
            }
        });
        
        holder.speakButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onActivitySpeak(activity);
            }
        });
    }

    @Override
    public int getItemCount() {
        return activities.size();
    }

    public static class ActivityViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView pictogramImageView;
        TextView activityNameText, activityTimeText, statusText;
        ImageButton completeButton, speakButton;

        public ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.activityCardView);
            pictogramImageView = itemView.findViewById(R.id.pictogramImageView);
            activityNameText = itemView.findViewById(R.id.activityNameText);
            activityTimeText = itemView.findViewById(R.id.activityTimeText);
            statusText = itemView.findViewById(R.id.statusText);
            completeButton = itemView.findViewById(R.id.completeButton);
            speakButton = itemView.findViewById(R.id.speakButton);
        }
    }
}
