package com.example.mirutinavisual;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
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
import java.util.Map;

public class TodayRoutineActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextToSpeech textToSpeech;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    
    private ImageButton backButton;
    private TextView dateText, emptyStateSubtitle;
    private LinearLayout emptyStateText;
    private CardView createRoutineButton;
    private RecyclerView activitiesRecyclerView;
    private ProgressBar loadingProgressBar;
    
    private ActivityAdapter activityAdapter;
    private List<Activity> activitiesList;
    
    // Sistema de logros
    private AchievementManager achievementManager;

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
            
            // Inicializar lista de actividades
            activitiesList = new ArrayList<>();
            
            // Inicializar sistema de logros
            achievementManager = new AchievementManager(this);
            achievementManager.setOnAchievementUnlockedListener(new AchievementManager.OnAchievementUnlockedListener() {
                @Override
                public void onAchievementUnlocked(Achievement achievement) {
                    // Mostrar celebración cuando se desbloquea un logro
                    speakText("¡Felicidades! Has desbloqueado el logro: " + achievement.getName());
                    System.out.println("TODAY: Logro desbloqueado: " + achievement.getName());
                }
            });
        
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
            dateText = findViewById(R.id.dateText);
            emptyStateText = findViewById(R.id.emptyStateText);
            emptyStateSubtitle = findViewById(R.id.emptyStateSubtitle);
            createRoutineButton = findViewById(R.id.createRoutineButton);
            activitiesRecyclerView = findViewById(R.id.activitiesRecyclerView);
            loadingProgressBar = findViewById(R.id.loadingProgressBar);
            
            // Verificar que las vistas existan
            if (dateText != null) {
                // Mostrar fecha actual
                SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd 'de' MMMM", new Locale("es", "ES"));
                String currentDate = dateFormat.format(new Date());
                dateText.setText(currentDate);
            }
            
            // Configurar interfaz según el modo
            configureUIForMode();
            
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
    
    private void configureUIForMode() {
        // Verificar si estamos en modo niño
        SharedPreferences prefs = getSharedPreferences("AppMode", MODE_PRIVATE);
        boolean isChildMode = prefs.getBoolean("child_mode", false);
        
        if (isChildMode) {
            // Modo niño: ocultar botón de crear rutina
            if (createRoutineButton != null) {
                createRoutineButton.setVisibility(View.GONE);
            }
            if (emptyStateSubtitle != null) {
                emptyStateSubtitle.setText("Pide a tu cuidador que agregue actividades");
            }
        } else {
            // Modo cuidador: mostrar botón de crear rutina
            if (createRoutineButton != null) {
                createRoutineButton.setVisibility(View.VISIBLE);
            }
            if (emptyStateSubtitle != null) {
                emptyStateSubtitle.setText("Toca el botón ➕ para agregar tu primera actividad");
            }
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
                // Crear adapter con listener para abrir actividades
                activityAdapter = new ActivityAdapter(activitiesList, new ActivityAdapter.OnActivityClickListener() {
                    @Override
                    public void onActivityClick(Activity activity) {
                        openActivity(activity);
                    }
                    
                    @Override
                    public void onActivityComplete(Activity activity) {
                        completeActivity(activity);
                    }
                    
                    @Override
                    public void onActivitySpeak(Activity activity) {
                        speakActivity(activity);
                    }
                });
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
                            Activity activity = createActivityFromSnapshot(activitySnapshot);
                            if (activity != null) {
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
    
    // Método para crear Activity desde DataSnapshot incluyendo pasos personalizados
    private Activity createActivityFromSnapshot(DataSnapshot activitySnapshot) {
        try {
            Activity activity = activitySnapshot.getValue(Activity.class);
            if (activity != null) {
                activity.setId(activitySnapshot.getKey());
                
                // Cargar pasos personalizados si existen
                Boolean isSequence = activitySnapshot.child("isSequence").getValue(Boolean.class);
                Object stepsData = activitySnapshot.child("steps").getValue();
                
                System.out.println("TODAY_LOAD: Actividad " + activity.getName() + 
                                 " - isSequence: " + isSequence + 
                                 " - tiene pasos: " + (stepsData != null));
                
                if (isSequence != null && isSequence && stepsData != null) {
                    // Cargar pasos personalizados
                    List<SequenceStep> customSteps = loadStepsFromSnapshot(stepsData);
                    if (!customSteps.isEmpty()) {
                        activity.setSequence(true);
                        activity.setSteps(customSteps);
                        System.out.println("TODAY_LOAD: Cargados " + customSteps.size() + 
                                         " pasos para " + activity.getName());
                    }
                } else {
                    System.out.println("TODAY_LOAD: " + activity.getName() + 
                                     " usará secuencia automática");
                }
            }
            return activity;
        } catch (Exception e) {
            System.out.println("TODAY_LOAD: Error al crear actividad desde snapshot: " + e.getMessage());
            return null;
        }
    }
    
    // Método para cargar pasos desde DataSnapshot
    private List<SequenceStep> loadStepsFromSnapshot(Object stepsData) {
        List<SequenceStep> steps = new ArrayList<>();
        
        try {
            if (stepsData instanceof List) {
                List<?> stepsList = (List<?>) stepsData;
                
                for (Object stepObj : stepsList) {
                    if (stepObj instanceof Map) {
                        Map<?, ?> stepMap = (Map<?, ?>) stepObj;
                        
                        SequenceStep step = new SequenceStep();
                        step.setId(getStringValue(stepMap, "id"));
                        step.setName(getStringValue(stepMap, "name"));
                        step.setDescription(getStringValue(stepMap, "description"));
                        step.setPictogramId((int) getLongValue(stepMap, "pictogramId"));
                        step.setPictogramKeyword(getStringValue(stepMap, "pictogramKeyword"));
                        step.setStepNumber(getIntValue(stepMap, "stepNumber"));
                        step.setCompleted(getBooleanValue(stepMap, "completed"));
                        step.setAudioText(getStringValue(stepMap, "audioText"));
                        
                        steps.add(step);
                    }
                }
                
                // Ordenar por número de paso
                steps.sort((s1, s2) -> Integer.compare(s1.getStepNumber(), s2.getStepNumber()));
            }
        } catch (Exception e) {
            System.out.println("TODAY_LOAD: Error al cargar pasos: " + e.getMessage());
        }
        
        return steps;
    }
    
    // Métodos auxiliares para extraer datos de Map de forma segura
    private String getStringValue(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }
    
    private long getLongValue(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0;
    }
    
    private int getIntValue(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }
    
    private boolean getBooleanValue(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return false;
    }
    
    // Método para abrir una actividad (modo secuencia o normal)
    private void openActivity(Activity activity) {
        try {
            speakText("Abriendo " + activity.getName());
            
            // Verificar si es una secuencia
            if (activity.isSequence() && activity.getTotalSteps() > 0) {
                // Abrir modo secuencia
                Intent intent = new Intent(this, SequenceActivity.class);
                intent.putExtra("activity_json", new com.google.gson.Gson().toJson(activity));
                startActivity(intent);
                System.out.println("TODAY: Abriendo modo secuencia para: " + activity.getName());
            } else {
                // Para actividades normales, crear una secuencia simple de ejemplo
                createSimpleSequence(activity);
            }
            
        } catch (Exception e) {
            System.out.println("TODAY: Error al abrir actividad: " + e.getMessage());
            showToast("Error al abrir la actividad");
        }
    }
    
    // Crear secuencia simple para actividades normales
    private void createSimpleSequence(Activity activity) {
        try {
            // Convertir actividad normal en secuencia de un paso
            activity.setSequence(true);
            
            SequenceStep step = new SequenceStep();
            step.setId("step_1");
            step.setName(activity.getName());
            step.setDescription("Realiza la actividad: " + activity.getName());
            step.setPictogramId(activity.getPictogramId());
            step.setPictogramKeyword(activity.getPictogramKeyword());
            step.setStepNumber(1);
            step.setCompleted(false);
            
            activity.addStep(step);
            
            // Abrir modo secuencia
            Intent intent = new Intent(this, SequenceActivity.class);
            intent.putExtra("activity_json", new com.google.gson.Gson().toJson(activity));
            startActivity(intent);
            
            System.out.println("TODAY: Creada secuencia simple para: " + activity.getName());
            
        } catch (Exception e) {
            System.out.println("TODAY: Error al crear secuencia simple: " + e.getMessage());
            showToast("Error al crear la secuencia");
        }
    }
    
    // Método para completar una actividad
    private void completeActivity(Activity activity) {
        try {
            activity.setCompleted(true);
            
            // Actualizar en Firebase
            if (databaseReference != null && firebaseAuth.getCurrentUser() != null) {
                databaseReference.child("activities").child(activity.getId())
                    .child("completed").setValue(true)
                    .addOnSuccessListener(aVoid -> {
                        speakText("¡Actividad " + activity.getName() + " completada! ¡Muy bien!");
                        showToast("✅ ¡Actividad completada!");
                        
                        // Actualizar la lista
                        if (activityAdapter != null) {
                            activityAdapter.notifyDataSetChanged();
                        }
                        
                        // *** VERIFICAR LOGROS ***
                        if (achievementManager != null) {
                            achievementManager.onActivityCompleted();
                        }
                        
                        System.out.println("TODAY: Actividad completada: " + activity.getName());
                    })
                    .addOnFailureListener(e -> {
                        System.out.println("TODAY: Error al completar actividad: " + e.getMessage());
                        showToast("Error al completar la actividad");
                    });
            }
            
        } catch (Exception e) {
            System.out.println("TODAY: Error al completar actividad: " + e.getMessage());
            showToast("Error al completar la actividad");
        }
    }
    
    // Método para leer en voz alta una actividad
    private void speakActivity(Activity activity) {
        try {
            String textToRead = "Actividad: " + activity.getName() + 
                              ". Programada para las " + activity.getTime();
            
            if (activity.isCompleted()) {
                textToRead += ". Esta actividad ya está completada.";
            } else {
                textToRead += ". Toca para abrir los pasos de la actividad.";
            }
            
            speakText(textToRead);
            System.out.println("TODAY: Leyendo actividad: " + activity.getName());
            
        } catch (Exception e) {
            System.out.println("TODAY: Error al leer actividad: " + e.getMessage());
            speakText("Error al leer la actividad");
        }
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
