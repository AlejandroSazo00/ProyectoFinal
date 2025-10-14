package com.example.mirutinavisual;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class AddStepDialog {

    public interface OnStepSavedListener {
        void onStepSaved(SequenceStep step);
        void onStepUpdated(SequenceStep step, int position);
    }

    private Context context;
    private OnStepSavedListener listener;
    private SequenceStep editingStep;
    private int editingPosition = -1;
    
    // Views del diálogo
    private EditText stepNameEditText, stepDescriptionEditText, stepPictogramSearchEditText;
    private ImageView selectedStepPictogramImageView;
    private TextView selectedStepPictogramNameText, selectedStepPictogramIdText, dialogTitleText;
    private RecyclerView stepPictogramsRecyclerView;
    private Button searchStepPictogramButton, cancelStepButton, saveStepButton;
    
    // Datos
    private Pictogram selectedPictogram;
    private List<Pictogram> pictogramsList;
    private PictogramAdapter pictogramAdapter;
    private ArasaacApiService arasaacService;

    public AddStepDialog(Context context, OnStepSavedListener listener) {
        this.context = context;
        this.listener = listener;
        this.pictogramsList = new ArrayList<>();
        this.arasaacService = new ArasaacApiService();
    }

    public void showAddDialog() {
        editingStep = null;
        editingPosition = -1;
        showDialog("➕ Agregar Nuevo Paso");
    }

    public void showEditDialog(SequenceStep step, int position) {
        editingStep = step;
        editingPosition = position;
        showDialog("✏️ Editar Paso");
    }

    private void showDialog(String title) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_step, null);
        
        // Inicializar vistas
        initViews(dialogView);
        
        // Configurar título
        dialogTitleText.setText(title);
        
        // Si estamos editando, llenar los campos
        if (editingStep != null) {
            fillEditingData();
        }
        
        // Configurar RecyclerView de pictogramas
        setupPictogramRecyclerView();
        
        // Crear y mostrar diálogo
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(true)
                .create();
        
        // Configurar listeners
        setupClickListeners(dialog);
        
        dialog.show();
    }

    private void initViews(View dialogView) {
        dialogTitleText = dialogView.findViewById(R.id.dialogTitleText);
        stepNameEditText = dialogView.findViewById(R.id.stepNameEditText);
        stepDescriptionEditText = dialogView.findViewById(R.id.stepDescriptionEditText);
        stepPictogramSearchEditText = dialogView.findViewById(R.id.stepPictogramSearchEditText);
        selectedStepPictogramImageView = dialogView.findViewById(R.id.selectedStepPictogramImageView);
        selectedStepPictogramNameText = dialogView.findViewById(R.id.selectedStepPictogramNameText);
        selectedStepPictogramIdText = dialogView.findViewById(R.id.selectedStepPictogramIdText);
        stepPictogramsRecyclerView = dialogView.findViewById(R.id.stepPictogramsRecyclerView);
        searchStepPictogramButton = dialogView.findViewById(R.id.searchStepPictogramButton);
        cancelStepButton = dialogView.findViewById(R.id.cancelStepButton);
        saveStepButton = dialogView.findViewById(R.id.saveStepButton);
    }

    private void fillEditingData() {
        stepNameEditText.setText(editingStep.getName());
        stepDescriptionEditText.setText(editingStep.getDescription());
        
        if (editingStep.getPictogramId() > 0) {
            // Crear pictograma temporal para mostrar
            selectedPictogram = new Pictogram();
            selectedPictogram.setId(editingStep.getPictogramId());
            selectedPictogram.setKeywords(List.of(editingStep.getPictogramKeyword()));
            
            updateSelectedPictogramView();
        }
    }

    private void setupPictogramRecyclerView() {
        try {
            System.out.println("STEP_DIALOG: Configurando RecyclerView de pictogramas");
            
            if (stepPictogramsRecyclerView != null) {
                stepPictogramsRecyclerView.setLayoutManager(new GridLayoutManager(context, 3));
                
                pictogramAdapter = new PictogramAdapter(pictogramsList, new PictogramAdapter.OnPictogramClickListener() {
                    @Override
                    public void onPictogramClick(Pictogram pictogram) {
                        try {
                            System.out.println("STEP_DIALOG: Pictograma seleccionado: " + pictogram.getId());
                            selectedPictogram = pictogram;
                            updateSelectedPictogramView();
                            stepPictogramsRecyclerView.setVisibility(View.GONE);
                        } catch (Exception e) {
                            System.out.println("STEP_DIALOG: Error al seleccionar pictograma: " + e.getMessage());
                            showToast("Error al seleccionar pictograma");
                        }
                    }
                });
                
                stepPictogramsRecyclerView.setAdapter(pictogramAdapter);
                System.out.println("STEP_DIALOG: RecyclerView configurado correctamente");
            } else {
                System.out.println("STEP_DIALOG: ERROR - stepPictogramsRecyclerView es null");
            }
        } catch (Exception e) {
            System.out.println("STEP_DIALOG: Error al configurar RecyclerView: " + e.getMessage());
        }
    }

    private void setupClickListeners(AlertDialog dialog) {
        // Buscar pictogramas
        searchStepPictogramButton.setOnClickListener(v -> {
            try {
                String keyword = stepPictogramSearchEditText.getText().toString().trim();
                System.out.println("STEP_DIALOG: Botón buscar presionado, keyword: '" + keyword + "'");
                
                if (!keyword.isEmpty() && keyword.length() >= 2) {
                    searchPictograms(keyword);
                } else {
                    showToast("Escribe al menos 2 letras para buscar");
                    System.out.println("STEP_DIALOG: Keyword muy corto o vacío");
                }
            } catch (Exception e) {
                System.out.println("STEP_DIALOG: Error en click buscar: " + e.getMessage());
                showToast("Error al procesar búsqueda");
            }
        });

        // Cancelar
        cancelStepButton.setOnClickListener(v -> dialog.dismiss());

        // Guardar paso
        saveStepButton.setOnClickListener(v -> {
            if (validateAndSaveStep()) {
                dialog.dismiss();
            }
        });
    }

    private void searchPictograms(String keyword) {
        try {
            System.out.println("STEP_DIALOG: Iniciando búsqueda de pictogramas para: " + keyword);
            
            // Mostrar RecyclerView y deshabilitar botón
            stepPictogramsRecyclerView.setVisibility(View.VISIBLE);
            searchStepPictogramButton.setEnabled(false);
            searchStepPictogramButton.setText("🔍...");
            
            arasaacService.searchPictograms(keyword, new ArasaacApiService.PictogramSearchCallback() {
                @Override
                public void onSuccess(List<Pictogram> pictograms) {
                    try {
                        System.out.println("STEP_DIALOG: Pictogramas encontrados: " + pictograms.size());
                        
                        // Asegurar que estamos en el hilo principal
                        if (context instanceof android.app.Activity) {
                            ((android.app.Activity) context).runOnUiThread(() -> {
                                try {
                                    pictogramsList.clear();
                                    pictogramsList.addAll(pictograms);
                                    
                                    if (pictogramAdapter != null) {
                                        pictogramAdapter.notifyDataSetChanged();
                                    }
                                    
                                    // Restaurar botón
                                    searchStepPictogramButton.setEnabled(true);
                                    searchStepPictogramButton.setText("🔍");
                                    
                                    if (pictograms.isEmpty()) {
                                        showToast("No se encontraron pictogramas para: " + keyword);
                                        stepPictogramsRecyclerView.setVisibility(View.GONE);
                                    }
                                    
                                    System.out.println("STEP_DIALOG: UI actualizada correctamente");
                                } catch (Exception e) {
                                    System.out.println("STEP_DIALOG: Error al actualizar UI: " + e.getMessage());
                                    handleSearchError("Error al mostrar resultados");
                                }
                            });
                        }
                    } catch (Exception e) {
                        System.out.println("STEP_DIALOG: Error en onSuccess: " + e.getMessage());
                        handleSearchError("Error al procesar resultados");
                    }
                }

                @Override
                public void onError(String error) {
                    System.out.println("STEP_DIALOG: Error en búsqueda: " + error);
                    handleSearchError(error);
                }
            });
            
        } catch (Exception e) {
            System.out.println("STEP_DIALOG: Error al iniciar búsqueda: " + e.getMessage());
            handleSearchError("Error al iniciar búsqueda");
        }
    }
    
    private void handleSearchError(String error) {
        try {
            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).runOnUiThread(() -> {
                    try {
                        // Restaurar botón
                        searchStepPictogramButton.setEnabled(true);
                        searchStepPictogramButton.setText("🔍");
                        
                        // Ocultar RecyclerView
                        stepPictogramsRecyclerView.setVisibility(View.GONE);
                        
                        // Mostrar error
                        showToast("Error al buscar pictogramas: " + error);
                        
                        System.out.println("STEP_DIALOG: Error manejado correctamente");
                    } catch (Exception e) {
                        System.out.println("STEP_DIALOG: Error al manejar error: " + e.getMessage());
                    }
                });
            }
        } catch (Exception e) {
            System.out.println("STEP_DIALOG: Error crítico en handleSearchError: " + e.getMessage());
        }
    }

    private void updateSelectedPictogramView() {
        try {
            System.out.println("STEP_DIALOG: Actualizando vista de pictograma seleccionado");
            
            if (selectedPictogram != null && selectedPictogram.getId() > 0) {
                if (selectedStepPictogramNameText != null) {
                    selectedStepPictogramNameText.setText("Pictograma seleccionado");
                }
                if (selectedStepPictogramIdText != null) {
                    selectedStepPictogramIdText.setText("ID: " + selectedPictogram.getId());
                }
                
                if (selectedStepPictogramImageView != null) {
                    try {
                        String imageUrl = "https://api.arasaac.org/api/pictograms/" + selectedPictogram.getId() + "?download=false";
                        System.out.println("STEP_DIALOG: Cargando imagen: " + imageUrl);
                        
                        Glide.with(context)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_placeholder)
                            .error(R.drawable.ic_error)
                            .into(selectedStepPictogramImageView);
                    } catch (Exception e) {
                        System.out.println("STEP_DIALOG: Error al cargar imagen con Glide: " + e.getMessage());
                        selectedStepPictogramImageView.setImageResource(R.drawable.ic_placeholder);
                    }
                }
                
                System.out.println("STEP_DIALOG: Vista actualizada para pictograma ID: " + selectedPictogram.getId());
            } else {
                if (selectedStepPictogramNameText != null) {
                    selectedStepPictogramNameText.setText("Ningún pictograma seleccionado");
                }
                if (selectedStepPictogramIdText != null) {
                    selectedStepPictogramIdText.setText("Busca y selecciona un pictograma");
                }
                if (selectedStepPictogramImageView != null) {
                    selectedStepPictogramImageView.setImageResource(R.drawable.ic_placeholder);
                }
                
                System.out.println("STEP_DIALOG: Vista restablecida - sin pictograma seleccionado");
            }
        } catch (Exception e) {
            System.out.println("STEP_DIALOG: Error al actualizar vista de pictograma: " + e.getMessage());
        }
    }

    private boolean validateAndSaveStep() {
        String stepName = stepNameEditText.getText().toString().trim();
        String stepDescription = stepDescriptionEditText.getText().toString().trim();

        // Validaciones
        if (stepName.isEmpty()) {
            showToast("❌ El nombre del paso es obligatorio");
            stepNameEditText.requestFocus();
            return false;
        }

        if (stepDescription.isEmpty()) {
            showToast("❌ La descripción del paso es obligatoria");
            stepDescriptionEditText.requestFocus();
            return false;
        }

        if (selectedPictogram == null) {
            showToast("❌ Selecciona un pictograma para el paso");
            return false;
        }

        // Crear o actualizar paso
        SequenceStep step;
        if (editingStep != null) {
            // Actualizar paso existente
            step = editingStep;
            step.setName(stepName);
            step.setDescription(stepDescription);
            step.setPictogramId(selectedPictogram.getId());
            step.setPictogramKeyword(selectedPictogram.getKeywords().get(0));
            step.setAudioText(stepName + ". " + stepDescription);
            
            if (listener != null) {
                listener.onStepUpdated(step, editingPosition);
            }
        } else {
            // Crear nuevo paso
            step = new SequenceStep();
            step.setId("step_" + System.currentTimeMillis());
            step.setName(stepName);
            step.setDescription(stepDescription);
            step.setPictogramId(selectedPictogram.getId());
            step.setPictogramKeyword(selectedPictogram.getKeywords().get(0));
            step.setStepNumber(1); // Se actualizará en la lista
            step.setCompleted(false);
            step.setAudioText(stepName + ". " + stepDescription);
            
            if (listener != null) {
                listener.onStepSaved(step);
            }
        }

        showToast("✅ Paso guardado correctamente");
        return true;
    }

    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
