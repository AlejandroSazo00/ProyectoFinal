package com.example.mirutinavisual;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;

import java.util.Locale;

public class SequenceActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextToSpeech textToSpeech;
    private DatabaseReference databaseReference;
    
    private Activity currentActivity;
    private SequenceStep currentStep;
    
    // Sistema de logros
    private AchievementManager achievementManager;
    
    // Views
    private ImageButton backButton;
    private TextView titleText, stepNumberText, stepNameText, stepDescriptionText, progressText;
    private ImageView stepImageView;
    private ProgressBar progressBar;
    private Button previousButton, nextButton, completeStepButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sequence);
        
        // Inicializar Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference();
        
        // Inicializar Text-to-Speech
        textToSpeech = new TextToSpeech(this, this);
        
        // Inicializar sistema de logros
        achievementManager = new AchievementManager(this);
        achievementManager.setOnAchievementUnlockedListener(achievement -> {
            speakText("¡Nuevo logro desbloqueado! " + achievement.getName());
        });
        
        // Obtener actividad del intent
        String activityJson = getIntent().getStringExtra("activity_json");
        if (activityJson != null) {
            try {
                currentActivity = new com.google.gson.Gson().fromJson(activityJson, Activity.class);
                System.out.println("SEQUENCE: Actividad cargada: " + currentActivity.getName());
                System.out.println("SEQUENCE: Total pasos: " + currentActivity.getTotalSteps());
            } catch (Exception e) {
                System.out.println("SEQUENCE: Error al deserializar actividad: " + e.getMessage());
                showToast("Error al cargar la actividad");
                finish();
                return;
            }
        } else {
            System.out.println("SEQUENCE: No se recibió actividad");
            showToast("No se pudo cargar la actividad");
            finish();
            return;
        }
        
        // Inicializar vistas
        initViews();
        
        // Configurar listeners
        setupClickListeners();
        
        // Cargar primer paso
        loadCurrentStep();
    }
    
    private void initViews() {
        backButton = findViewById(R.id.backButton);
        titleText = findViewById(R.id.titleText);
        stepNumberText = findViewById(R.id.stepNumberText);
        stepNameText = findViewById(R.id.stepNameText);
        stepDescriptionText = findViewById(R.id.stepDescriptionText);
        stepImageView = findViewById(R.id.stepImageView);
        progressBar = findViewById(R.id.progressBar);
        progressText = findViewById(R.id.progressText);
        previousButton = findViewById(R.id.previousButton);
        nextButton = findViewById(R.id.nextButton);
        completeStepButton = findViewById(R.id.completeStepButton);
    }
    
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> {
            speakText("Saliendo de la secuencia");
            finish();
        });
        
        previousButton.setOnClickListener(v -> {
            if (currentActivity.hasPreviousStep()) {
                currentActivity.previousStep();
                loadCurrentStep();
                speakText("Paso anterior");
            }
        });
        
        nextButton.setOnClickListener(v -> {
            if (currentActivity.hasNextStep()) {
                currentActivity.nextStep();
                loadCurrentStep();
                speakText("Siguiente paso");
            } else {
                // Última paso completado
                completeSequence();
            }
        });
        
        completeStepButton.setOnClickListener(v -> {
            completeCurrentStep();
        });
        
        stepImageView.setOnClickListener(v -> {
            if (currentStep != null) {
                speakText(currentStep.getAudioText());
            }
        });
    }
    
    private void loadCurrentStep() {
        if (currentActivity == null) return;
        
        currentStep = currentActivity.getCurrentStep();
        if (currentStep == null) return;
        
        // Actualizar UI
        titleText.setText(currentActivity.getName());
        stepNumberText.setText("Paso " + (currentActivity.getCurrentStepIndex() + 1) + " de " + currentActivity.getTotalSteps());
        stepNameText.setText(currentStep.getName());
        stepDescriptionText.setText(currentStep.getDescription());
        
        // Cargar imagen del pictograma
        String imageUrl = "https://api.arasaac.org/api/pictograms/" + currentStep.getPictogramId() + "?download=false";
        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.ic_placeholder)
            .error(R.drawable.ic_error)
            .into(stepImageView);
        
        // Actualizar progreso
        int progress = (int) currentActivity.getProgressPercentage();
        progressBar.setProgress(progress);
        progressText.setText(currentActivity.getCompletedStepsCount() + "/" + currentActivity.getTotalSteps() + " completados");
        
        // Actualizar botones
        previousButton.setEnabled(currentActivity.hasPreviousStep());
        nextButton.setText(currentActivity.hasNextStep() ? "Siguiente" : "Finalizar");
        
        // Actualizar estado del botón completar
        if (currentStep.isCompleted()) {
            completeStepButton.setText("✅ Completado");
            completeStepButton.setBackgroundColor(getResources().getColor(R.color.green));
        } else {
            completeStepButton.setText("Marcar como completado");
            completeStepButton.setBackgroundColor(getResources().getColor(R.color.primary));
        }
        
        // Leer automáticamente el paso
        speakText(currentStep.getAudioText());
    }
    
    private void completeCurrentStep() {
        if (currentStep == null) return;
        
        currentStep.setCompleted(!currentStep.isCompleted());
        
        if (currentStep.isCompleted()) {
            speakText("¡Paso completado! ¡Muy bien!");
            showToast("✅ ¡Paso completado!");
        } else {
            speakText("Paso marcado como pendiente");
            showToast("⏳ Paso pendiente");
        }
        
        // Actualizar UI
        loadCurrentStep();
        
        // Guardar en Firebase
        saveProgressToFirebase();
    }
    
    private void completeSequence() {
        currentActivity.setCompleted(true);
        
        speakText("¡Felicidades! Has completado toda la secuencia de " + currentActivity.getName());
        showToast("🎉 ¡Secuencia completada!");
        
        // *** VERIFICAR LOGROS ***
        if (achievementManager != null) {
            achievementManager.onActivityCompleted();
        }
        
        // Guardar en Firebase
        saveProgressToFirebase();
        
        // Volver a la pantalla anterior
        setResult(RESULT_OK);
        finish();
    }
    
    private void saveProgressToFirebase() {
        if (currentActivity.getId() != null) {
            databaseReference.child("activities").child(currentActivity.getId())
                .setValue(currentActivity)
                .addOnSuccessListener(aVoid -> {
                    System.out.println("SEQUENCE: Progreso guardado en Firebase");
                })
                .addOnFailureListener(e -> {
                    System.out.println("SEQUENCE: Error al guardar progreso: " + e.getMessage());
                });
        }
    }
    
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(new Locale("es", "ES"));
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech.setLanguage(Locale.getDefault());
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
