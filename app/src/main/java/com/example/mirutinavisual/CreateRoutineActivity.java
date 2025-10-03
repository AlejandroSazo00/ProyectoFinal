package com.example.mirutinavisual;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CreateRoutineActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextToSpeech textToSpeech;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    
    private ImageButton backButton;
    private EditText activityNameEditText, searchPictogramEditText;
    private TimePicker timePicker;
    private Button searchButton, saveActivityButton;
    private RecyclerView pictogramsRecyclerView;
    private ImageView selectedPictogramImageView;
    private TextView selectedPictogramText;
    
    private PictogramAdapter pictogramAdapter;
    private List<Pictogram> pictogramList;
    private Pictogram selectedPictogram;
    private ArasaacApiService arasaacService;
    
    // Variables para modo edición
    private boolean isEditMode = false;
    private String editingActivityId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_routine);
        
        // Inicializar Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        
        // Inicializar Text-to-Speech
        textToSpeech = new TextToSpeech(this, this);
        
        // Inicializar servicio ARASAAC
        arasaacService = new ArasaacApiService();
        
        // Inicializar listas
        pictogramList = new ArrayList<>();
        
        // Inicializar vistas
        initViews();
        
        // Configurar RecyclerView
        setupRecyclerView();
        
        // Configurar listeners
        setupClickListeners();
        
        // Verificar si viene en modo edición
        checkEditMode();
        
        // Mensaje de bienvenida
        if (isEditMode) {
            speakText("Editar rutina. Modifica los datos y guarda los cambios");
        } else {
            speakText("Crear nueva rutina. Escribe el nombre de la actividad y busca un pictograma");
        }
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        activityNameEditText = findViewById(R.id.activityNameEditText);
        searchPictogramEditText = findViewById(R.id.searchPictogramEditText);
        timePicker = findViewById(R.id.timePicker);
        searchButton = findViewById(R.id.searchButton);
        saveActivityButton = findViewById(R.id.saveActivityButton);
        pictogramsRecyclerView = findViewById(R.id.pictogramsRecyclerView);
        selectedPictogramImageView = findViewById(R.id.selectedPictogramImageView);
        selectedPictogramText = findViewById(R.id.selectedPictogramText);
        
        // Configurar TimePicker en formato 24 horas
        timePicker.setIs24HourView(true);
    }

    private void setupRecyclerView() {
        pictogramAdapter = new PictogramAdapter(pictogramList, new PictogramAdapter.OnPictogramClickListener() {
            @Override
            public void onPictogramClick(Pictogram pictogram) {
                selectPictogram(pictogram);
            }
        });
        
        pictogramsRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        pictogramsRecyclerView.setAdapter(pictogramAdapter);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> {
            speakText("Volver");
            finish();
        });

        searchButton.setOnClickListener(v -> {
            String searchTerm = searchPictogramEditText.getText().toString().trim();
            if (!TextUtils.isEmpty(searchTerm)) {
                speakText("Buscando pictogramas de " + searchTerm);
                searchPictograms(searchTerm);
            } else {
                speakText("Por favor escribe qué pictograma buscar");
                searchPictogramEditText.requestFocus();
            }
        });

        saveActivityButton.setOnClickListener(v -> {
            speakText("Guardando actividad");
            saveActivity();
        });

        activityNameEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                speakText("Escribe el nombre de la actividad");
            }
        });

        searchPictogramEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                speakText("Escribe qué pictograma quieres buscar");
            }
        });
    }

    private void searchPictograms(String searchTerm) {
        // Mostrar indicador de carga
        searchButton.setEnabled(false);
        searchButton.setText("Buscando...");
        
        // Buscar pictogramas usando ARASAAC API
        arasaacService.searchPictograms(searchTerm, new ArasaacApiService.PictogramSearchCallback() {
            @Override
            public void onSuccess(List<Pictogram> pictograms) {
                runOnUiThread(() -> {
                    pictogramList.clear();
                    pictogramList.addAll(pictograms);
                    pictogramAdapter.notifyDataSetChanged();
                    
                    searchButton.setEnabled(true);
                    searchButton.setText("🔍 Buscar");
                    
                    if (pictograms.isEmpty()) {
                        speakText("No se encontraron pictogramas. Intenta con otra palabra");
                        showToast("No se encontraron resultados");
                    } else {
                        speakText("Se encontraron " + pictograms.size() + " pictogramas. Toca uno para seleccionarlo");
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    searchButton.setEnabled(true);
                    searchButton.setText("🔍 Buscar");
                    speakText("Error al buscar pictogramas. Verifica tu conexión a internet");
                    showToast("Error: " + error);
                });
            }
        });
    }

    private void selectPictogram(Pictogram pictogram) {
        selectedPictogram = pictogram;
        
        // Mostrar pictograma seleccionado
        arasaacService.loadPictogramImage(pictogram, selectedPictogramImageView);
        selectedPictogramText.setText(pictogram.getKeywords().get(0));
        selectedPictogramText.setVisibility(View.VISIBLE);
        
        speakText("Pictograma seleccionado: " + pictogram.getKeywords().get(0));
        showToast("Pictograma seleccionado");
    }

    private void saveActivity() {
        String activityName = activityNameEditText.getText().toString().trim();
        
        // Validaciones
        if (TextUtils.isEmpty(activityName)) {
            speakText("Por favor escribe el nombre de la actividad");
            activityNameEditText.setError("Campo requerido");
            activityNameEditText.requestFocus();
            return;
        }

        if (selectedPictogram == null) {
            speakText("Por favor selecciona un pictograma");
            showToast("Selecciona un pictograma");
            return;
        }

        // Obtener hora seleccionada
        int hour = timePicker.getCurrentHour();
        int minute = timePicker.getCurrentMinute();
        String timeString = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);

        // Crear objeto de actividad
        Map<String, Object> activity = new HashMap<>();
        activity.put("name", activityName);
        activity.put("time", timeString);
        activity.put("pictogramId", selectedPictogram.getId());
        activity.put("pictogramKeyword", selectedPictogram.getKeywords().get(0));
        activity.put("createdAt", System.currentTimeMillis());
        activity.put("userId", firebaseAuth.getCurrentUser().getUid());

        // Guardar en Firebase
        saveActivityButton.setEnabled(false);
        
        String activityId;
        if (isEditMode && editingActivityId != null) {
            // Modo edición - actualizar actividad existente
            activityId = editingActivityId;
            activity.put("id", activityId);
            activity.put("updatedAt", System.currentTimeMillis());
            saveActivityButton.setText("Actualizando...");
            
            databaseReference.child("activities").child(activityId)
                    .updateChildren(activity)
                    .addOnSuccessListener(aVoid -> {
                        speakText("Actividad actualizada exitosamente");
                        showToast("¡Actividad actualizada!");
                        
                        // Reprogramar recordatorio
                        scheduleActivityReminder(activityId, activityName, timeString, selectedPictogram.getId());
                        
                        // Volver a la pantalla principal
                        Intent intent = new Intent(CreateRoutineActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        saveActivityButton.setEnabled(true);
                        saveActivityButton.setText("💾 Actualizar Actividad");
                        
                        speakText("Error al actualizar la actividad");
                        showToast("Error al actualizar: " + e.getMessage());
                    });
        } else {
            // Modo creación - crear nueva actividad
            activityId = databaseReference.child("activities").push().getKey();
            if (activityId != null) {
                activity.put("id", activityId);
                saveActivityButton.setText("Guardando...");
                
                databaseReference.child("activities").child(activityId)
                        .setValue(activity)
                        .addOnSuccessListener(aVoid -> {
                            speakText("Actividad guardada exitosamente. Recordatorio programado");
                            showToast("¡Actividad guardada y recordatorio programado!");
                            
                            // Programar recordatorio
                            scheduleActivityReminder(activityId, activityName, timeString, selectedPictogram.getId());
                            
                            // Volver a la pantalla principal
                            Intent intent = new Intent(CreateRoutineActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            saveActivityButton.setEnabled(true);
                            saveActivityButton.setText("💾 Guardar Actividad");
                            
                            speakText("Error al guardar la actividad");
                            showToast("Error al guardar: " + e.getMessage());
                        });
            }
        }
    }
    
    private void checkEditMode() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("activity_id")) {
            isEditMode = true;
            editingActivityId = intent.getStringExtra("activity_id");
            
            // Cargar datos de la actividad para editar
            String activityName = intent.getStringExtra("activity_name");
            String activityTime = intent.getStringExtra("activity_time");
            
            if (activityName != null) {
                activityNameEditText.setText(activityName);
            }
            
            if (activityTime != null) {
                // Parsear la hora y configurar el TimePicker
                String[] timeParts = activityTime.split(":");
                if (timeParts.length == 2) {
                    int hour = Integer.parseInt(timeParts[0]);
                    int minute = Integer.parseInt(timeParts[1]);
                    timePicker.setHour(hour);
                    timePicker.setMinute(minute);
                }
            }
            
            // Cambiar el texto del botón
            saveActivityButton.setText("💾 Actualizar Actividad");
        }
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

    private void scheduleActivityReminder(String activityId, String activityName, String time, int pictogramId) {
        // Crear objeto Activity para el recordatorio
        Activity reminderActivity = new Activity();
        reminderActivity.setId(activityId);
        reminderActivity.setName(activityName);
        reminderActivity.setTime(time);
        reminderActivity.setPictogramId(pictogramId);
        reminderActivity.setUserId(firebaseAuth.getCurrentUser().getUid());
        
        // Programar recordatorio
        NotificationService notificationService = new NotificationService(this);
        notificationService.scheduleActivityReminder(reminderActivity);
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
