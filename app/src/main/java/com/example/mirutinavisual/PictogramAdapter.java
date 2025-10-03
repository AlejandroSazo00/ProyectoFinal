package com.example.mirutinavisual;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PictogramAdapter extends RecyclerView.Adapter<PictogramAdapter.PictogramViewHolder> {

    private List<Pictogram> pictograms;
    private OnPictogramClickListener listener;
    private ArasaacApiService arasaacService;
    private int selectedPosition = -1;

    public interface OnPictogramClickListener {
        void onPictogramClick(Pictogram pictogram);
    }

    public PictogramAdapter(List<Pictogram> pictograms, OnPictogramClickListener listener) {
        this.pictograms = pictograms;
        this.listener = listener;
        this.arasaacService = new ArasaacApiService();
    }

    @NonNull
    @Override
    public PictogramViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pictogram, parent, false);
        return new PictogramViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PictogramViewHolder holder, int position) {
        Pictogram pictogram = pictograms.get(position);
        
        // Cargar imagen del pictograma
        arasaacService.loadPictogramImage(pictogram, holder.pictogramImageView);
        
        // Mostrar primera keyword como texto
        if (!pictogram.getKeywords().isEmpty()) {
            holder.pictogramTextView.setText(pictogram.getKeywords().get(0));
        } else {
            holder.pictogramTextView.setText("Pictograma");
        }
        
        // Destacar el elemento seleccionado
        if (position == selectedPosition) {
            holder.cardView.setCardBackgroundColor(
                holder.itemView.getContext().getResources().getColor(R.color.primary_color));
            holder.pictogramTextView.setTextColor(
                holder.itemView.getContext().getResources().getColor(android.R.color.white));
        } else {
            holder.cardView.setCardBackgroundColor(
                holder.itemView.getContext().getResources().getColor(R.color.background_white));
            holder.pictogramTextView.setTextColor(
                holder.itemView.getContext().getResources().getColor(R.color.text_primary));
        }
        
        // Click listener
        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = position;
            
            // Actualizar elementos visuales
            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);
            
            // Callback
            if (listener != null) {
                listener.onPictogramClick(pictogram);
            }
        });
    }

    @Override
    public int getItemCount() {
        return pictograms.size();
    }

    public static class PictogramViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView pictogramImageView;
        TextView pictogramTextView;

        public PictogramViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.pictogramCardView);
            pictogramImageView = itemView.findViewById(R.id.pictogramImageView);
            pictogramTextView = itemView.findViewById(R.id.pictogramTextView);
        }
    }
}
