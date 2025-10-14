package com.example.mirutinavisual;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Locale;

public class FullScreenActivityActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextToSpeech textToSpeech;
    private DatabaseReference databaseReference;
    
    private ImageView activityImageView;
    private TextView activityNameText, instructionText;
    private Button completeButton, postponeButton, closeButton;
    
    private String activityId;
    private String activityName;
    private int pictogramId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Configurar pantalla completa
        setupFullScreen();
        
        setContentView(R.layout.activity_fullscreen_activity);
        
        // Inicializar Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference();
        
        // Inicializar Text-to-Speech
        textToSpeech = new TextToSpeech(this, this);
        
        // Obtener datos del intent
        getIntentData();
        
        // Inicializar vistas
        initViews();
        
        // Configurar listeners
        setupClickListeners();
        
        // Cargar datos de la actividad
        loadActivityData();
    }

    private void setupFullScreen() {
        try {
            // Mantener pantalla encendida (más seguro para emuladores)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            
            // Solo en dispositivos reales, no en emuladores
            if (!isEmulator()) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            }
            
            // Pantalla completa más suave
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_IMMERSIVE
            );
            
        } catch (Exception e) {
            // Si falla, continuar sin pantalla completa
            System.out.println("Error configurando pantalla completa: " + e.getMessage());
        }
    }
    
    private boolean isEmulator() {
        return android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(android.os.Build.PRODUCT);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        activityId = intent.getStringExtra("activity_id");
        activityName = intent.getStringExtra("activity_name");
        pictogramId = intent.getIntExtra("pictogram_id", 0);
        
        // Valores por defecto si no se reciben datos
        if (activityName == null) {
            activityName = "Actividad";
        }
        if (activityId == null) {
            activityId = "unknown";
        }
    }

    private void initViews() {
        activityImageView = findViewById(R.id.activityImageView);
        activityNameText = findViewById(R.id.activityNameText);
        instructionText = findViewById(R.id.instructionText);
        completeButton = findViewById(R.id.completeButton);
        postponeButton = findViewById(R.id.postponeButton);
        closeButton = findViewById(R.id.closeButton);
    }

    private void setupClickListeners() {
        completeButton.setOnClickListener(v -> {
            speakText("¡Muy bien! Actividad completada: " + activityName);
            markAsCompleted();
        });

        postponeButton.setOnClickListener(v -> {
            speakText("Recordatorio pospuesto por 5 minutos");
            postponeReminder();
        });

        closeButton.setOnClickListener(v -> {
            speakText("Cerrando recordatorio");
            goToTodayRoutine();
        });

        // Hacer que la imagen sea clickeable para repetir el mensaje
        activityImageView.setOnClickListener(v -> {
            speakText("Es hora de: " + activityName + ". Toca el botón verde cuando hayas terminado");
        });
    }

    private void loadActivityData() {
        // Configurar nombre de la actividad
        activityNameText.setText(activityName);
        instructionText.setText("¡Es hora de realizar esta actividad!");
        
        // Cargar pictograma
        if (pictogramId > 0) {
            String pictogramUrl = "https://api.arasaac.org/api/pictograms/" + pictogramId + "?download=false";
            Glide.with(this)
                    .load(pictogramUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_error)
                    .into(activityImageView);
        } else {
            activityImageView.setImageResource(R.drawable.ic_image_placeholder);
        }
        
        // Mensaje de voz automático después de 1 segundo
        activityImageView.postDelayed(() -> {
            speakText("¡Es hora de: " + activityName + "! Toca el botón verde cuando hayas terminado la actividad");
        }, 1000);
    }

    private void markAsCompleted() {
        if (!activityId.equals("unknown")) {
            databaseReference.child("activities").child(activityId)
                    .child("completed").setValue(true)
                    .addOnSuccessListener(aVoid -> {
                        showToast("¡Actividad completada! 🎉");
                        goToTodayRoutine();
                    })
                    .addOnFailureListener(e -> {
                        showToast("Error al marcar como completada");
                    });
        } else {
            showToast("¡Muy bien! Actividad completada 🎉");
            goToTodayRoutine();
        }
    }

    private void postponeReminder() {
        // Crear un recordatorio para 5 minutos después
        NotificationService notificationService = new NotificationService(this);
        
        // Crear actividad temporal para el recordatorio
        Activity tempActivity = new Activity();
        tempActivity.setId(activityId + "_postponed");
        tempActivity.setName(activityName);
        tempActivity.setPictogramId(pictogramId);
        
        // Calcular tiempo 5 minutos después
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.add(java.util.Calendar.MINUTE, 5);
        int hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);
        int minute = calendar.get(java.util.Calendar.MINUTE);
        tempActivity.setTime(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
        
        notificationService.scheduleActivityReminder(tempActivity);
        
        showToast("Recordatorio pospuesto por 5 minutos");
        goToTodayRoutine();
    }

    private void goToTodayRoutine() {
        // Redirigir siempre a "Mi Rutina de Hoy" para el niño
        Intent intent = new Intent(this, TodayRoutineActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(new Locale("es", "ES"));
            
            if (result == TextToSpeech.LANG_MISSING_DATA || 
                result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech.setLanguage(Locale.US);
            }
            
            textToSpeech.setSpeechRate(0.7f); // Más lento para mejor comprensión
            textToSpeech.setPitch(1.1f); // Tono ligeramente más alto para llamar la atención
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

    @Override
    public void onBackPressed() {
        // Prevenir que se cierre accidentalmente con el botón atrás
        speakText("Usa los botones en pantalla para continuar");
    }
}
