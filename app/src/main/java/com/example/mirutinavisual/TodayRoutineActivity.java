package com.example.mirutinavisual;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TodayRoutineActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextToSpeech textToSpeech;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    
    private ImageButton backButton, addActivityButton;
    private TextView dateText;
    private LinearLayout emptyStateText;
    private RecyclerView activitiesRecyclerView;
    private ProgressBar loadingProgressBar;
    
    private ActivityAdapter activityAdapter;
    private List<Activity> activitiesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_today_routine);
            
            // Inicializar Firebase
            firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        
        // Inicializar Text-to-Speech
        textToSpeech = new TextToSpeech(this, this);
        
        // Inicializar lista
        activitiesList = new ArrayList<>();
        
        // Inicializar vistas
        initViews();
        
        // Configurar RecyclerView
        setupRecyclerView();
        
        // Configurar listeners
        setupClickListeners();
        
            // Mensaje de bienvenida
            speakText("Mi rutina de hoy");
            
            // Mostrar estado vacío por defecto
            if (emptyStateText != null) {
                emptyStateText.setVisibility(View.VISIBLE);
            }
            if (loadingProgressBar != null) {
                loadingProgressBar.setVisibility(View.GONE);
            }
            
            // Cargar actividades (simplificado)
            loadTodayActivities();
            
        } catch (Exception e) {
            // Manejo de errores
            showToast("Error al cargar la pantalla: " + e.getMessage());
            finish();
        }
    }

    private void initViews() {
        try {
            backButton = findViewById(R.id.backButton);
            addActivityButton = findViewById(R.id.addActivityButton);
            dateText = findViewById(R.id.dateText);
            emptyStateText = findViewById(R.id.emptyStateText);
            activitiesRecyclerView = findViewById(R.id.activitiesRecyclerView);
            loadingProgressBar = findViewById(R.id.loadingProgressBar);
            
            // Verificar que las vistas existan
            if (dateText != null) {
                // Mostrar fecha actual
                SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd 'de' MMMM", new Locale("es", "ES"));
                String currentDate = dateFormat.format(new Date());
                dateText.setText(currentDate);
            }
            
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
                showToast("RecyclerView no disponible");
                return;
            }
            
            // Configurar RecyclerView de forma simple
            activitiesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            
            // Solo crear adapter si no existe
            if (activityAdapter == null) {
                // Crear adapter simple sin listeners por ahora
                activityAdapter = new ActivityAdapter(activitiesList, null);
            }
            
            activitiesRecyclerView.setAdapter(activityAdapter);
            
        } catch (Exception e) {
            showToast("Error RecyclerView: " + e.getMessage());
            // Si falla, ocultar el RecyclerView
            if (activitiesRecyclerView != null) {
                activitiesRecyclerView.setVisibility(View.GONE);
            }
        }
    }

    private void setupClickListeners() {
        try {
            if (backButton != null) {
                backButton.setOnClickListener(v -> {
                    speakText("Volver");
                    finish();
                });
            }

            if (addActivityButton != null) {
                addActivityButton.setOnClickListener(v -> {
                    speakText("Agregar nueva actividad");
                    Intent intent = new Intent(TodayRoutineActivity.this, CreateRoutineActivity.class);
                    startActivity(intent);
                });
            }
            
        } catch (Exception e) {
            showToast("Error al configurar listeners: " + e.getMessage());
        }
    }

    private void loadTodayActivities() {
        if (firebaseAuth.getCurrentUser() == null) {
            showToast("Error: Usuario no autenticado");
            return;
        }

        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(View.VISIBLE);
        }
        if (emptyStateText != null) {
            emptyStateText.setVisibility(View.GONE);
        }
        
        String userId = firebaseAuth.getCurrentUser().getUid();
        
        databaseReference.child("activities")
                .orderByChild("userId")
                .equalTo(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        activitiesList.clear();
                        
                        for (DataSnapshot activitySnapshot : dataSnapshot.getChildren()) {
                            Activity activity = activitySnapshot.getValue(Activity.class);
                            if (activity != null) {
                                activity.setId(activitySnapshot.getKey());
                                activitiesList.add(activity);
                            }
                        }
                        
                        try {
                            // Ordenar por hora
                            Collections.sort(activitiesList, (a1, a2) -> a1.getTime().compareTo(a2.getTime()));
                            
                            // Actualizar adapter solo si existe
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
                                speakText("No tienes actividades programadas para hoy");
                            } else {
                                if (emptyStateText != null) {
                                    emptyStateText.setVisibility(View.GONE);
                                }
                                speakText("Tienes " + activitiesList.size() + " actividades para hoy");
                            }
                        } catch (Exception e) {
                            showToast("Error al mostrar actividades: " + e.getMessage());
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
                        speakText("Error al cargar las actividades");
                        showToast("Error: " + databaseError.getMessage());
                    }
                });
    }

    private void showActivityDetail(Activity activity) {
        speakText("Actividad: " + activity.getName() + " a las " + activity.getTime());
        
        // Aquí podrías abrir una pantalla de detalle o mostrar un diálogo
        showToast("Actividad: " + activity.getName());
    }

    private void markActivityAsCompleted(Activity activity) {
        if (activity.getId() == null) return;
        
        // Actualizar estado en Firebase
        databaseReference.child("activities").child(activity.getId())
                .child("completed").setValue(true)
                .addOnSuccessListener(aVoid -> {
                    speakText("¡Muy bien! Actividad completada: " + activity.getName());
                    showToast("¡Actividad completada! 🎉");
                })
                .addOnFailureListener(e -> {
                    speakText("Error al marcar como completada");
                    showToast("Error al actualizar");
                });
    }

    private void speakActivityDetails(Activity activity) {
        String message = "Actividad: " + activity.getName() + 
                        ". Hora programada: " + activity.getTime();
        
        if (activity.isCompleted()) {
            message += ". Ya está completada";
        } else {
            message += ". Pendiente por realizar";
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
        if (textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
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
