package com.example.mirutinavisual;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.AchievementViewHolder> {

    public interface OnAchievementClickListener {
        void onAchievementClick(Achievement achievement);
    }

    private List<Achievement> achievementsList;
    private OnAchievementClickListener listener;

    public AchievementAdapter(List<Achievement> achievementsList, OnAchievementClickListener listener) {
        this.achievementsList = achievementsList;
        this.listener = listener;
    }

    public void updateAchievements(List<Achievement> newAchievements) {
        this.achievementsList = newAchievements;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AchievementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_achievement, parent, false);
        return new AchievementViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AchievementViewHolder holder, int position) {
        Achievement achievement = achievementsList.get(position);
        
        // Configurar textos
        holder.nameText.setText(achievement.getName());
        holder.descriptionText.setText(achievement.getDescription());
        
        // Configurar imagen del pictograma
        if (achievement.getPictogramId() != null && !achievement.getPictogramId().isEmpty()) {
            String imageUrl = "https://api.arasaac.org/api/pictograms/" + achievement.getPictogramId() + "?download=false";
            Glide.with(holder.itemView.getContext())
                .load(imageUrl)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_error)
                .into(holder.achievementImageView);
        } else {
            holder.achievementImageView.setImageResource(R.drawable.ic_placeholder);
        }
        
        // Configurar estado (desbloqueado/bloqueado)
        if (achievement.isUnlocked()) {
            holder.itemView.setAlpha(1.0f);
            holder.statusText.setText("✅ Desbloqueado");
            holder.statusText.setTextColor(holder.itemView.getContext().getColor(R.color.success));
            holder.itemView.setBackgroundColor(holder.itemView.getContext().getColor(R.color.background_white));
        } else {
            holder.itemView.setAlpha(0.5f);
            holder.statusText.setText("🔒 Bloqueado");
            holder.statusText.setTextColor(holder.itemView.getContext().getColor(R.color.text_secondary));
            holder.itemView.setBackgroundColor(holder.itemView.getContext().getColor(R.color.background_light));
        }
        
        // Configurar click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAchievementClick(achievement);
            }
        });
    }

    @Override
    public int getItemCount() {
        return achievementsList != null ? achievementsList.size() : 0;
    }

    public static class AchievementViewHolder extends RecyclerView.ViewHolder {
        ImageView achievementImageView;
        TextView nameText, descriptionText, statusText;

        public AchievementViewHolder(@NonNull View itemView) {
            super(itemView);
            achievementImageView = itemView.findViewById(R.id.achievementImageView);
            nameText = itemView.findViewById(R.id.achievementNameText);
            descriptionText = itemView.findViewById(R.id.achievementDescriptionText);
            statusText = itemView.findViewById(R.id.achievementStatusText);
        }
    }
}
