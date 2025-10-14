package com.example.mirutinavisual;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import androidx.core.app.NotificationCompat;
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
import androidx.recyclerview.widget.LinearLayoutManager;
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
    private Button searchButton, saveActivityButton, addStepButton;
    private RecyclerView pictogramsRecyclerView, stepsRecyclerView;
    private ImageView selectedPictogramImageView;
    private TextView selectedPictogramText;
    
    // Variables para pasos personalizados
    private List<SequenceStep> customStepsList;
    private StepAdapter stepAdapter;
    private AddStepDialog addStepDialog;
    
    private PictogramAdapter pictogramAdapter;
    private List<Pictogram> pictogramList;
    private Pictogram selectedPictogram;
    private ArasaacApiService arasaacService;
    
    // Variables para modo edición
    private boolean isEditMode = false;
    private String editingActivityId = null;
    
    // Variable para actividad pendiente de programar
    private Activity pendingActivity = null;

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
        customStepsList = new ArrayList<>();
        
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
        
        // Nuevas vistas para pasos
        stepsRecyclerView = findViewById(R.id.stepsRecyclerView);
        addStepButton = findViewById(R.id.addStepButton);
        
        // Configurar TimePicker en formato 24 horas
        timePicker.setIs24HourView(true);
    }

    private void setupRecyclerView() {
        // Configurar RecyclerView de pictogramas
        pictogramAdapter = new PictogramAdapter(pictogramList, new PictogramAdapter.OnPictogramClickListener() {
            @Override
            public void onPictogramClick(Pictogram pictogram) {
                selectPictogram(pictogram);
            }
        });
        
        pictogramsRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        pictogramsRecyclerView.setAdapter(pictogramAdapter);
        
        // Configurar RecyclerView de pasos
        setupStepsRecyclerView();
        
        // Inicializar diálogo de pasos
        initStepDialog();
    }
    
    private void setupStepsRecyclerView() {
        stepAdapter = new StepAdapter(customStepsList, new StepAdapter.OnStepActionListener() {
            @Override
            public void onEditStep(SequenceStep step, int position) {
                addStepDialog.showEditDialog(step, position);
            }
            
            @Override
            public void onDeleteStep(SequenceStep step, int position) {
                deleteStep(position);
            }
        });
        
        stepsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        stepsRecyclerView.setAdapter(stepAdapter);
        stepsRecyclerView.setNestedScrollingEnabled(false);
    }
    
    private void initStepDialog() {
        addStepDialog = new AddStepDialog(this, new AddStepDialog.OnStepSavedListener() {
            @Override
            public void onStepSaved(SequenceStep step) {
                addNewStep(step);
            }
            
            @Override
            public void onStepUpdated(SequenceStep step, int position) {
                updateStep(step, position);
            }
        });
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
        
        // Listener para agregar paso
        addStepButton.setOnClickListener(v -> {
            speakText("Agregar nuevo paso");
            addStepDialog.showAddDialog();
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
        
        // Obtener la primera palabra clave de forma segura
        String keyword = "pictograma";
        if (pictogram.getKeywords() != null && !pictogram.getKeywords().isEmpty()) {
            keyword = pictogram.getKeywords().get(0);
        }
        
        selectedPictogramText.setText(keyword);
        selectedPictogramText.setVisibility(View.VISIBLE);
        
        speakText("Pictograma seleccionado: " + keyword);
        showToast("Pictograma seleccionado");
    }
    
    // Métodos para manejar pasos personalizados
    private void addNewStep(SequenceStep step) {
        // Actualizar número del paso
        step.setStepNumber(customStepsList.size() + 1);
        
        // Agregar a la lista
        customStepsList.add(step);
        
        // Notificar al adapter
        stepAdapter.notifyItemInserted(customStepsList.size() - 1);
        
        speakText("Paso agregado: " + step.getName());
        showToast("✅ Paso agregado correctamente");
        
        System.out.println("CREATE: Paso agregado - Total pasos: " + customStepsList.size());
    }
    
    private void updateStep(SequenceStep step, int position) {
        if (position >= 0 && position < customStepsList.size()) {
            customStepsList.set(position, step);
            stepAdapter.notifyItemChanged(position);
            
            speakText("Paso actualizado: " + step.getName());
            showToast("✅ Paso actualizado correctamente");
            
            System.out.println("CREATE: Paso actualizado en posición: " + position);
        }
    }
    
    private void deleteStep(int position) {
        if (position >= 0 && position < customStepsList.size()) {
            SequenceStep removedStep = customStepsList.get(position);
            
            // Mostrar confirmación
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Eliminar Paso")
                .setMessage("¿Estás seguro de que quieres eliminar el paso '" + removedStep.getName() + "'?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    customStepsList.remove(position);
                    
                    // Actualizar números de pasos
                    updateStepNumbers();
                    
                    stepAdapter.notifyDataSetChanged();
                    
                    speakText("Paso eliminado: " + removedStep.getName());
                    showToast("🗑️ Paso eliminado");
                    
                    System.out.println("CREATE: Paso eliminado - Total pasos: " + customStepsList.size());
                })
                .setNegativeButton("Cancelar", null)
                .show();
        }
    }
    
    private void updateStepNumbers() {
        for (int i = 0; i < customStepsList.size(); i++) {
            customStepsList.get(i).setStepNumber(i + 1);
        }
    }

    private void saveActivity() {
        String activityName = activityNameEditText.getText().toString().trim();
        
        System.out.println("GUARDAR: Iniciando proceso de guardado para: " + activityName);
        
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
        
        System.out.println("GUARDAR: Hora seleccionada: " + timeString + " (hora=" + hour + ", minuto=" + minute + ")");

        // Crear objeto de actividad
        Map<String, Object> activity = new HashMap<>();
        activity.put("name", activityName);
        activity.put("time", timeString);
        activity.put("pictogramId", selectedPictogram.getId());
        // Usar la primera palabra clave si existe
        if (selectedPictogram.getKeywords() != null && !selectedPictogram.getKeywords().isEmpty()) {
            activity.put("pictogramKeyword", selectedPictogram.getKeywords().get(0));
        } else {
            activity.put("pictogramKeyword", "pictograma");
        }
        activity.put("createdAt", System.currentTimeMillis());
        activity.put("userId", firebaseAuth.getCurrentUser().getUid());
        
        // *** AGREGAR PASOS PERSONALIZADOS ***
        if (!customStepsList.isEmpty()) {
            activity.put("isSequence", true);
            activity.put("totalSteps", customStepsList.size());
            
            // Convertir pasos a Map para Firebase
            List<Map<String, Object>> stepsMapList = new ArrayList<>();
            for (SequenceStep step : customStepsList) {
                Map<String, Object> stepMap = new HashMap<>();
                stepMap.put("id", step.getId());
                stepMap.put("name", step.getName());
                stepMap.put("description", step.getDescription());
                stepMap.put("pictogramId", step.getPictogramId());
                stepMap.put("pictogramKeyword", step.getPictogramKeyword());
                stepMap.put("stepNumber", step.getStepNumber());
                stepMap.put("completed", step.isCompleted());
                stepMap.put("audioText", step.getAudioText());
                stepsMapList.add(stepMap);
            }
            activity.put("steps", stepsMapList);
            
            System.out.println("GUARDAR: Agregando " + customStepsList.size() + " pasos personalizados a Firebase");
        } else {
            activity.put("isSequence", false);
            activity.put("totalSteps", 0);
            System.out.println("GUARDAR: Sin pasos personalizados, se usará secuencia automática");
        }

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
                        
                        // Reprogramar recordatorio (usar el mismo sistema que funciona)
                        String keyword = "pictograma";
                        if (selectedPictogram.getKeywords() != null && !selectedPictogram.getKeywords().isEmpty()) {
                            keyword = selectedPictogram.getKeywords().get(0);
                        }
                        
                        System.out.println("ACTUALIZAR: Reprogramando recordatorio...");
                        
                        // Usar EXACTAMENTE el mismo sistema que los botones de prueba
                        Activity updatedActivity = new Activity();
                        updatedActivity.setId(activityId);
                        updatedActivity.setName(activityName);
                        updatedActivity.setTime(timeString);
                        updatedActivity.setPictogramId(selectedPictogram.getId());
                        updatedActivity.setPictogramKeyword(keyword);
                        updatedActivity.setUserId(firebaseAuth.getCurrentUser().getUid());
                        
                        // Verificar permisos antes de programar
                        if (checkAlarmPermissions()) {
                            // Usar directamente NotificationService (igual que los botones)
                            NotificationService notificationService = new NotificationService(CreateRoutineActivity.this);
                            notificationService.scheduleActivityReminder(updatedActivity);
                            System.out.println("ACTUALIZAR: Recordatorio reprogramado directamente");
                        } else {
                            System.out.println("ACTUALIZAR: Solicitando permisos de alarma...");
                            requestAlarmPermissions();
                            // Guardar la actividad para programar después
                            pendingActivity = updatedActivity;
                        }
                        
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
                            System.out.println("GUARDAR: Actividad guardada en Firebase exitosamente");
                            speakText("Actividad guardada exitosamente. Recordatorio programado");
                            showToast("¡Actividad guardada y recordatorio programado!");
                            
                            // Programar recordatorio
                            String keyword = "pictograma";
                            if (selectedPictogram.getKeywords() != null && !selectedPictogram.getKeywords().isEmpty()) {
                                keyword = selectedPictogram.getKeywords().get(0);
                            }
                            System.out.println("GUARDAR: Iniciando programación de recordatorio...");
                            
                            // Usar EXACTAMENTE el mismo sistema que los botones de prueba
                            Activity realActivity = new Activity();
                            realActivity.setId(activityId);
                            realActivity.setName(activityName);
                            realActivity.setTime(timeString);
                            realActivity.setPictogramId(selectedPictogram.getId());
                            realActivity.setPictogramKeyword(keyword);
                            realActivity.setUserId(firebaseAuth.getCurrentUser().getUid());
                            
                            // Usar pasos personalizados si existen, sino crear secuencia automática
                            if (!customStepsList.isEmpty()) {
                                // Usar pasos personalizados
                                realActivity.setSequence(true);
                                realActivity.setSteps(new ArrayList<>(customStepsList));
                                System.out.println("GUARDAR: Usando " + customStepsList.size() + " pasos personalizados");
                            } else {
                                // Crear secuencia automática como antes
                                createExampleSequence(realActivity, activityName, selectedPictogram);
                                System.out.println("GUARDAR: Creando secuencia automática");
                            }
                            
                            System.out.println("GUARDAR: Creando actividad con hora: " + timeString);
                            
                            // Verificar permisos antes de programar
                            System.out.println("GUARDAR: ===== INICIANDO PROGRAMACIÓN DE ALARMA =====");
                            System.out.println("GUARDAR: Actividad ID: " + activityId);
                            System.out.println("GUARDAR: Nombre: " + activityName);
                            System.out.println("GUARDAR: Hora: " + timeString);
                            
                            boolean hasPerms = checkAlarmPermissions();
                            System.out.println("GUARDAR: ¿Tiene permisos de alarma? " + hasPerms);
                            
                            if (hasPerms) {
                                System.out.println("GUARDAR: PROGRAMANDO ALARMA AHORA...");
                                // Usar directamente NotificationService (igual que los botones)
                                NotificationService notificationService = new NotificationService(CreateRoutineActivity.this);
                                notificationService.scheduleActivityReminder(realActivity);
                                System.out.println("GUARDAR: ===== ALARMA PROGRAMADA EXITOSAMENTE =====");
                            } else {
                                System.out.println("GUARDAR: ===== SIN PERMISOS - SOLICITANDO =====");
                                requestAlarmPermissions();
                                // Guardar la actividad para programar después
                                pendingActivity = realActivity;
                            }
                            
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
            
            // *** CARGAR PASOS EXISTENTES DESDE FIREBASE ***
            loadExistingSteps();
        }
    }
    
    private void loadExistingSteps() {
        if (editingActivityId != null) {
            System.out.println("EDIT: Cargando pasos existentes para actividad: " + editingActivityId);
            
            databaseReference.child("activities").child(editingActivityId)
                .get()
                .addOnSuccessListener(dataSnapshot -> {
                    try {
                        if (dataSnapshot.exists()) {
                            // Verificar si tiene pasos personalizados
                            Boolean isSequence = dataSnapshot.child("isSequence").getValue(Boolean.class);
                            Object stepsData = dataSnapshot.child("steps").getValue();
                            
                            System.out.println("EDIT: isSequence = " + isSequence);
                            System.out.println("EDIT: stepsData = " + (stepsData != null ? "existe" : "null"));
                            
                            if (isSequence != null && isSequence && stepsData != null) {
                                // Cargar pasos personalizados
                                loadStepsFromFirebase(stepsData);
                            } else {
                                System.out.println("EDIT: No hay pasos personalizados, actividad usa secuencia automática");
                            }
                        } else {
                            System.out.println("EDIT: Actividad no encontrada en Firebase");
                        }
                    } catch (Exception e) {
                        System.out.println("EDIT: Error al procesar datos de actividad: " + e.getMessage());
                    }
                })
                .addOnFailureListener(e -> {
                    System.out.println("EDIT: Error al cargar actividad desde Firebase: " + e.getMessage());
                    showToast("Error al cargar datos de la actividad");
                });
        }
    }
    
    private void loadStepsFromFirebase(Object stepsData) {
        try {
            customStepsList.clear();
            
            if (stepsData instanceof List) {
                List<?> stepsList = (List<?>) stepsData;
                
                for (Object stepObj : stepsList) {
                    if (stepObj instanceof Map) {
                        Map<?, ?> stepMap = (Map<?, ?>) stepObj;
                        
                        SequenceStep step = new SequenceStep();
                        step.setId(getString(stepMap, "id"));
                        step.setName(getString(stepMap, "name"));
                        step.setDescription(getString(stepMap, "description"));
                        step.setPictogramId((int) getLong(stepMap, "pictogramId"));
                        step.setPictogramKeyword(getString(stepMap, "pictogramKeyword"));
                        step.setStepNumber(getInt(stepMap, "stepNumber"));
                        step.setCompleted(getBoolean(stepMap, "completed"));
                        step.setAudioText(getString(stepMap, "audioText"));
                        
                        customStepsList.add(step);
                    }
                }
                
                // Ordenar por número de paso
                customStepsList.sort((s1, s2) -> Integer.compare(s1.getStepNumber(), s2.getStepNumber()));
                
                // Actualizar adapter
                if (stepAdapter != null) {
                    stepAdapter.notifyDataSetChanged();
                }
                
                System.out.println("EDIT: Cargados " + customStepsList.size() + " pasos personalizados");
                showToast("✅ Pasos cargados: " + customStepsList.size());
            }
        } catch (Exception e) {
            System.out.println("EDIT: Error al convertir pasos desde Firebase: " + e.getMessage());
            showToast("Error al cargar pasos");
        }
    }
    
    // Métodos auxiliares para extraer datos de Map de forma segura
    private String getString(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }
    
    private long getLong(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0;
    }
    
    private int getInt(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }
    
    private boolean getBoolean(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return false;
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

    // Métodos para verificar y solicitar permisos de alarma
    private boolean checkAlarmPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                boolean canSchedule = alarmManager.canScheduleExactAlarms();
                System.out.println("PERMISOS: canScheduleExactAlarms = " + canSchedule);
                return canSchedule;
            }
        }
        return true; // En versiones anteriores no se necesita verificación
    }
    
    private void requestAlarmPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 1001);
                
                showToast("⚠️ Se necesitan permisos para programar recordatorios");
                speakText("Se necesitan permisos para programar recordatorios. Por favor, activa los permisos.");
            } catch (Exception e) {
                System.out.println("ERROR: No se pudo abrir configuración de permisos: " + e.getMessage());
                showToast("❌ No se pudieron solicitar permisos");
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1001) {
            // Usuario regresó de la configuración de permisos
            if (checkAlarmPermissions()) {
                System.out.println("PERMISOS: Permisos concedidos, programando actividad pendiente");
                if (pendingActivity != null) {
                    NotificationService notificationService = new NotificationService(this);
                    notificationService.scheduleActivityReminder(pendingActivity);
                    showToast("✅ Recordatorio programado exitosamente");
                    speakText("Recordatorio programado exitosamente");
                    pendingActivity = null;
                }
            } else {
                System.out.println("PERMISOS: Permisos denegados");
                showToast("❌ Sin permisos, el recordatorio no funcionará");
                speakText("Sin permisos, el recordatorio no funcionará correctamente");
            }
        }
    }

    // Método para crear secuencias de ejemplo
    private void createExampleSequence(Activity activity, String activityName, Pictogram pictogram) {
        try {
            // Crear secuencias para actividades comunes
            if (activityName.toLowerCase().contains("vestir") || 
                activityName.toLowerCase().contains("ropa")) {
                createVestirseSequence(activity, pictogram);
            } else if (activityName.toLowerCase().contains("diente") || 
                       activityName.toLowerCase().contains("cepill")) {
                createCepillarDientesSequence(activity, pictogram);
            } else if (activityName.toLowerCase().contains("desayun") || 
                       activityName.toLowerCase().contains("comer")) {
                createDesayunarSequence(activity, pictogram);
            } else {
                // Para otras actividades, crear secuencia simple
                createSimpleSequence(activity, activityName, pictogram);
            }
            
            System.out.println("CREAR_SECUENCIA: Secuencia creada para: " + activityName + 
                             " con " + activity.getTotalSteps() + " pasos");
            
        } catch (Exception e) {
            System.out.println("CREAR_SECUENCIA: Error: " + e.getMessage());
        }
    }
    
    private void createVestirseSequence(Activity activity, Pictogram pictogram) {
        activity.setSequence(true);
        
        SequenceStep step1 = new SequenceStep("step_1", "Ropa interior", 
            "Ponte la ropa interior", pictogram.getId(), "ropa", 1);
        
        SequenceStep step2 = new SequenceStep("step_2", "Camiseta", 
            "Ponte la camiseta", pictogram.getId(), "camiseta", 2);
            
        SequenceStep step3 = new SequenceStep("step_3", "Pantalón", 
            "Ponte el pantalón", pictogram.getId(), "pantalon", 3);
            
        SequenceStep step4 = new SequenceStep("step_4", "Zapatos", 
            "Ponte los zapatos", pictogram.getId(), "zapatos", 4);
        
        activity.addStep(step1);
        activity.addStep(step2);
        activity.addStep(step3);
        activity.addStep(step4);
    }
    
    private void createCepillarDientesSequence(Activity activity, Pictogram pictogram) {
        activity.setSequence(true);
        
        SequenceStep step1 = new SequenceStep("step_1", "Tomar cepillo", 
            "Toma tu cepillo de dientes", pictogram.getId(), "cepillo", 1);
            
        SequenceStep step2 = new SequenceStep("step_2", "Pasta dental", 
            "Pon pasta dental en el cepillo", pictogram.getId(), "pasta", 2);
            
        SequenceStep step3 = new SequenceStep("step_3", "Cepillar", 
            "Cepilla tus dientes por 2 minutos", pictogram.getId(), "cepillar", 3);
            
        SequenceStep step4 = new SequenceStep("step_4", "Enjuagar", 
            "Enjuaga tu boca con agua", pictogram.getId(), "agua", 4);
        
        activity.addStep(step1);
        activity.addStep(step2);
        activity.addStep(step3);
        activity.addStep(step4);
    }
    
    private void createDesayunarSequence(Activity activity, Pictogram pictogram) {
        activity.setSequence(true);
        
        SequenceStep step1 = new SequenceStep("step_1", "Preparar mesa", 
            "Pon el plato y los cubiertos", pictogram.getId(), "mesa", 1);
            
        SequenceStep step2 = new SequenceStep("step_2", "Servir comida", 
            "Sirve tu desayuno", pictogram.getId(), "comida", 2);
            
        SequenceStep step3 = new SequenceStep("step_3", "Comer", 
            "Disfruta tu desayuno", pictogram.getId(), "comer", 3);
            
        SequenceStep step4 = new SequenceStep("step_4", "Limpiar", 
            "Recoge los platos", pictogram.getId(), "limpiar", 4);
        
        activity.addStep(step1);
        activity.addStep(step2);
        activity.addStep(step3);
        activity.addStep(step4);
    }
    
    private void createSimpleSequence(Activity activity, String activityName, Pictogram pictogram) {
        activity.setSequence(true);
        
        SequenceStep step1 = new SequenceStep("step_1", "Prepararse", 
            "Prepárate para " + activityName.toLowerCase(), pictogram.getId(), 
            pictogram.getKeywords() != null && !pictogram.getKeywords().isEmpty() ? 
            pictogram.getKeywords().get(0) : "actividad", 1);
            
        SequenceStep step2 = new SequenceStep("step_2", activityName, 
            "Realiza la actividad: " + activityName, pictogram.getId(), 
            pictogram.getKeywords() != null && !pictogram.getKeywords().isEmpty() ? 
            pictogram.getKeywords().get(0) : "actividad", 2);
            
        SequenceStep step3 = new SequenceStep("step_3", "Terminar", 
            "Termina y guarda todo", pictogram.getId(), 
            pictogram.getKeywords() != null && !pictogram.getKeywords().isEmpty() ? 
            pictogram.getKeywords().get(0) : "actividad", 3);
        
        activity.addStep(step1);
        activity.addStep(step2);
        activity.addStep(step3);
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
