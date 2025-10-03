package com.example.mirutinavisual;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ManageActivitiesActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextToSpeech textToSpeech;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    
    private ImageButton backButton;
    private LinearLayout emptyStateText;
    private RecyclerView activitiesRecyclerView;
    private ProgressBar loadingProgressBar;
    private FloatingActionButton addActivityFab;
    
    private ManageActivityAdapter activityAdapter;
    private List<Activity> activitiesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_manage_activities);
            
            // Inicializar Firebase
            firebaseAuth = FirebaseAuth.getInstance();
            databaseReference = FirebaseDatabase.getInstance().getReference();
            
            // Inicializar Text-to-Speech
            textToSpeech = new TextToSpeech(this, this);
            
            // Inicializar lista
            activitiesList = new ArrayList<>();
        
        // Inicializar vistas
        initViews();
        
        // Configurar listeners básicos
        setupClickListeners();
        
            // Configurar RecyclerView
            setupRecyclerView();
            
            // Mensaje de bienvenida
            speakText("Administrar actividades");
            
            // Cargar rutinas
            loadAllActivities();
            
        } catch (Exception e) {
            showToast("Error al cargar pantalla: " + e.getMessage());
            // NO hacer finish() - dejar que el usuario decida cuándo salir
        }
    }

    private void initViews() {
        try {
            backButton = findViewById(R.id.backButton);
            emptyStateText = findViewById(R.id.emptyStateText);
            activitiesRecyclerView = findViewById(R.id.activitiesRecyclerView);
            loadingProgressBar = findViewById(R.id.loadingProgressBar);
            addActivityFab = findViewById(R.id.addActivityFab);
            
            // Estado inicial
            if (emptyStateText != null) {
                emptyStateText.setVisibility(View.GONE);
            }
            
            if (loadingProgressBar != null) {
                loadingProgressBar.setVisibility(View.VISIBLE);
            }
            
        } catch (Exception e) {
            showToast("Error al inicializar vistas: " + e.getMessage());
        }
    }

    private void setupRecyclerView() {
        try {
            if (activitiesRecyclerView == null || activitiesList == null) {
                return;
            }
            
            activitiesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            
            activityAdapter = new ManageActivityAdapter(activitiesList, new ManageActivityAdapter.OnActivityActionListener() {
                @Override
                public void onEditActivity(Activity activity) {
                    editActivity(activity);
                }

                @Override
                public void onDeleteActivity(Activity activity) {
                    confirmDeleteActivity(activity);
                }

                @Override
                public void onToggleActivity(Activity activity) {
                    toggleActivityStatus(activity);
                }

                @Override
                public void onSpeakActivity(Activity activity) {
                    speakActivityDetails(activity);
                }
            });
            
            activitiesRecyclerView.setAdapter(activityAdapter);
            
        } catch (Exception e) {
            showToast("Error RecyclerView: " + e.getMessage());
        }
    }

    private void setupClickListeners() {
        try {
            if (backButton != null) {
                backButton.setOnClickListener(v -> {
                    try {
                        speakText("Volver");
                        finish();
                    } catch (Exception e) {
                        finish(); // Asegurar que siempre cierre
                    }
                });
            }

            if (addActivityFab != null) {
                addActivityFab.setOnClickListener(v -> {
                    try {
                        speakText("Crear nueva actividad");
                        Intent intent = new Intent(ManageActivitiesActivity.this, CreateRoutineActivity.class);
                        startActivity(intent);
                    } catch (Exception e) {
                        showToast("Error al abrir crear rutina");
                    }
                });
            }
            
        } catch (Exception e) {
            showToast("Error al configurar listeners: " + e.getMessage());
        }
    }

    private void loadAllActivities() {
        try {
            if (firebaseAuth.getCurrentUser() == null) {
                showToast("Error: Usuario no autenticado");
                return;
            }

            String userId = firebaseAuth.getCurrentUser().getUid();
            
            databaseReference.child("activities")
                    .orderByChild("userId")
                    .equalTo(userId)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            try {
                                activitiesList.clear();
                                
                                for (DataSnapshot activitySnapshot : dataSnapshot.getChildren()) {
                                    Activity activity = activitySnapshot.getValue(Activity.class);
                                    if (activity != null) {
                                        activity.setId(activitySnapshot.getKey());
                                        activitiesList.add(activity);
                                    }
                                }
                                
                                // Ordenar por hora
                                Collections.sort(activitiesList, (a1, a2) -> a1.getTime().compareTo(a2.getTime()));
                                
                                // Actualizar UI
                                if (activityAdapter != null) {
                                    activityAdapter.notifyDataSetChanged();
                                }
                                
                                if (loadingProgressBar != null) {
                                    loadingProgressBar.setVisibility(View.GONE);
                                }
                                
                                if (activitiesList.isEmpty()) {
                                    if (emptyStateText != null) {
                                        emptyStateText.setVisibility(View.VISIBLE);
                                    }
                                    speakText("No hay rutinas creadas");
                                } else {
                                    if (emptyStateText != null) {
                                        emptyStateText.setVisibility(View.GONE);
                                    }
                                    speakText("Se cargaron " + activitiesList.size() + " rutinas");
                                }
                                
                            } catch (Exception e) {
                                showToast("Error al mostrar rutinas: " + e.getMessage());
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            if (loadingProgressBar != null) {
                                loadingProgressBar.setVisibility(View.GONE);
                            }
                            if (emptyStateText != null) {
                                emptyStateText.setVisibility(View.VISIBLE);
                            }
                            showToast("Error al cargar rutinas: " + databaseError.getMessage());
                        }
                    });
                    
        } catch (Exception e) {
            showToast("Error: " + e.getMessage());
        }
    }
    private void editActivity(Activity activity) {
        speakText("Editando actividad: " + activity.getName());
        Intent intent = new Intent(ManageActivitiesActivity.this, CreateRoutineActivity.class);
        intent.putExtra("edit_mode", true);
        intent.putExtra("activity_id", activity.getId());
        intent.putExtra("activity_name", activity.getName());
        intent.putExtra("activity_time", activity.getTime());
        intent.putExtra("pictogram_id", activity.getPictogramId());
        startActivity(intent);
    }

    private void confirmDeleteActivity(Activity activity) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Eliminar Rutina");
        builder.setMessage("¿Estás seguro de que quieres eliminar '" + activity.getName() + "'?\n\nEsta acción no se puede deshacer.");
        
        builder.setPositiveButton("Eliminar", (dialog, which) -> {
            speakText("Eliminando rutina");
            deleteActivity(activity);
        });
        
        builder.setNegativeButton("Cancelar", (dialog, which) -> {
            speakText("Eliminación cancelada");
        });
        
        builder.show();
    }

    private void deleteActivity(Activity activity) {
        if (activity.getId() == null) return;
        
        databaseReference.child("activities").child(activity.getId())
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    speakText("Rutina eliminada: " + activity.getName());
                    showToast("Rutina eliminada correctamente");
                    
                    // Cancelar recordatorio
                    NotificationService notificationService = new NotificationService(this);
                    notificationService.cancelActivityReminder(activity.getId());
                })
                .addOnFailureListener(e -> {
                    speakText("Error al eliminar rutina");
                    showToast("Error al eliminar: " + e.getMessage());
                });
    }

    private void toggleActivityStatus(Activity activity) {
        if (activity.getId() == null) return;
        
        boolean newStatus = !activity.isCompleted();
        
        databaseReference.child("activities").child(activity.getId())
                .child("completed").setValue(newStatus)
                .addOnSuccessListener(aVoid -> {
                    activity.setCompleted(newStatus);
                    String statusText = newStatus ? "completada" : "marcada como pendiente";
                    speakText("Rutina " + statusText + ": " + activity.getName());
                    showToast("Estado actualizado");
                    
                    if (activityAdapter != null) {
                        activityAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    speakText("Error al cambiar estado");
                    showToast("Error al actualizar estado");
                });
    }

    private void speakActivityDetails(Activity activity) {
        String message = "Actividad: " + activity.getName() + 
                        ". Hora: " + activity.getTime();
        
        if (activity.isCompleted()) {
            message += ". Estado: Completada";
        } else {
            message += ". Estado: Pendiente";
        }
        
        speakText(message);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(new Locale("es", "ES"));
            
            if (result == TextToSpeech.LANG_MISSING_DATA || 
                result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech.setLanguage(Locale.US);
            }
            
            textToSpeech.setSpeechRate(0.8f);
            textToSpeech.setPitch(1.0f);
        }
    }

    private void speakText(String text) {
        try {
            if (textToSpeech != null) {
                // Detener cualquier speech anterior para evitar trabas
                textToSpeech.stop();
                // Pequeña pausa antes de hablar
                android.os.Handler handler = new android.os.Handler();
                handler.postDelayed(() -> {
                    textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
                }, 100);
            }
        } catch (Exception e) {
            // Si falla el TTS, no hacer nada para evitar crashes
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}
