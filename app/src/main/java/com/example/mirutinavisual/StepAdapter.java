package com.example.mirutinavisual;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class StepAdapter extends RecyclerView.Adapter<StepAdapter.StepViewHolder> {

    private List<SequenceStep> stepsList;
    private OnStepActionListener listener;

    public interface OnStepActionListener {
        void onEditStep(SequenceStep step, int position);
        void onDeleteStep(SequenceStep step, int position);
    }

    public StepAdapter(List<SequenceStep> stepsList, OnStepActionListener listener) {
        this.stepsList = stepsList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public StepViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_step_create, parent, false);
        return new StepViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StepViewHolder holder, int position) {
        SequenceStep step = stepsList.get(position);
        
        // Número del paso
        holder.stepNumberText.setText("Paso " + (position + 1));
        
        // Nombre del paso
        holder.stepNameText.setText(step.getName());
        
        // Descripción del paso
        holder.stepDescriptionText.setText(step.getDescription());
        
        // Cargar imagen del pictograma
        if (step.getPictogramId() > 0) {
            String imageUrl = "https://api.arasaac.org/api/pictograms/" + step.getPictogramId() + "?download=false";
            Glide.with(holder.itemView.getContext())
                .load(imageUrl)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_error)
                .into(holder.stepImageView);
        } else {
            holder.stepImageView.setImageResource(R.drawable.ic_placeholder);
        }
        
        // Listeners para botones
        holder.editButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditStep(step, position);
            }
        });
        
        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteStep(step, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return stepsList != null ? stepsList.size() : 0;
    }

    public static class StepViewHolder extends RecyclerView.ViewHolder {
        TextView stepNumberText, stepNameText, stepDescriptionText;
        ImageView stepImageView;
        ImageButton editButton, deleteButton;

        public StepViewHolder(@NonNull View itemView) {
            super(itemView);
            stepNumberText = itemView.findViewById(R.id.stepNumberText);
            stepNameText = itemView.findViewById(R.id.stepNameText);
            stepDescriptionText = itemView.findViewById(R.id.stepDescriptionText);
            stepImageView = itemView.findViewById(R.id.stepImageView);
            editButton = itemView.findViewById(R.id.editStepButton);
            deleteButton = itemView.findViewById(R.id.deleteStepButton);
        }
    }
}
