package com.example.mirutinavisual;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class StatisticsActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextToSpeech textToSpeech;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    
    private ImageButton backButton;
    private TextView totalActivitiesText, completedTodayText, completionRateText, streakText;
    private TextView weeklyCompletionText, monthlyCompletionText, lastActivityText;
    private ProgressBar completionProgressBar;
    private CardView todayStatsCard, weeklyStatsCard, monthlyStatsCard, achievementsCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        
        // Inicializar Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        
        // Inicializar Text-to-Speech
        textToSpeech = new TextToSpeech(this, this);
        
        // Inicializar vistas
        initViews();
        
        // Configurar listeners
        setupClickListeners();
        
        // Cargar estadísticas
        loadStatistics();
        
        // Mensaje de bienvenida
        speakText("Estadísticas de progreso. Aquí puedes ver tu rendimiento y logros");
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        totalActivitiesText = findViewById(R.id.totalActivitiesText);
        completedTodayText = findViewById(R.id.completedTodayText);
        completionRateText = findViewById(R.id.completionRateText);
        streakText = findViewById(R.id.streakText);
        weeklyCompletionText = findViewById(R.id.weeklyCompletionText);
        monthlyCompletionText = findViewById(R.id.monthlyCompletionText);
        lastActivityText = findViewById(R.id.lastActivityText);
        completionProgressBar = findViewById(R.id.completionProgressBar);
        
        todayStatsCard = findViewById(R.id.todayStatsCard);
        weeklyStatsCard = findViewById(R.id.weeklyStatsCard);
        monthlyStatsCard = findViewById(R.id.monthlyStatsCard);
        achievementsCard = findViewById(R.id.achievementsCard);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> {
            speakText("Volver");
            finish();
        });

        todayStatsCard.setOnClickListener(v -> {
            speakText("Estadísticas de hoy: " + completedTodayText.getText() + " actividades completadas");
        });

        weeklyStatsCard.setOnClickListener(v -> {
            speakText("Estadísticas semanales: " + weeklyCompletionText.getText() + " por ciento de completación");
        });

        monthlyStatsCard.setOnClickListener(v -> {
            speakText("Estadísticas mensuales: " + monthlyCompletionText.getText() + " por ciento de completación");
        });

        achievementsCard.setOnClickListener(v -> {
            speakText("Racha actual: " + streakText.getText() + " días consecutivos");
        });
    }

    private void loadStatistics() {
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
                        calculateStatistics(dataSnapshot);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        showToast("Error al cargar estadísticas: " + databaseError.getMessage());
                    }
                });
    }

    private void calculateStatistics(DataSnapshot dataSnapshot) {
        int totalActivities = 0;
        int completedToday = 0;
        int totalCompleted = 0;
        int weeklyCompleted = 0;
        int weeklyTotal = 0;
        int monthlyCompleted = 0;
        int monthlyTotal = 0;
        
        // Fechas para comparación
        Calendar today = Calendar.getInstance();
        Calendar weekAgo = Calendar.getInstance();
        weekAgo.add(Calendar.DAY_OF_YEAR, -7);
        Calendar monthAgo = Calendar.getInstance();
        monthAgo.add(Calendar.MONTH, -1);
        
        String todayDateString = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(today.getTime());
        
        for (DataSnapshot activitySnapshot : dataSnapshot.getChildren()) {
            Activity activity = activitySnapshot.getValue(Activity.class);
            if (activity != null) {
                totalActivities++;
                
                if (activity.isCompleted()) {
                    totalCompleted++;
                    
                    // Verificar si fue completada hoy (simulación)
                    // En una implementación real, necesitarías guardar la fecha de completación
                    completedToday++; // Simplificado para demo
                }
                
                // Estadísticas semanales y mensuales (simuladas)
                long createdTime = activity.getCreatedAt();
                if (createdTime > weekAgo.getTimeInMillis()) {
                    weeklyTotal++;
                    if (activity.isCompleted()) {
                        weeklyCompleted++;
                    }
                }
                
                if (createdTime > monthAgo.getTimeInMillis()) {
                    monthlyTotal++;
                    if (activity.isCompleted()) {
                        monthlyCompleted++;
                    }
                }
            }
        }
        
        // Actualizar UI con estadísticas
        updateStatisticsUI(totalActivities, completedToday, totalCompleted, 
                          weeklyCompleted, weeklyTotal, monthlyCompleted, monthlyTotal);
    }

    private void updateStatisticsUI(int total, int completedToday, int totalCompleted,
                                  int weeklyCompleted, int weeklyTotal, 
                                  int monthlyCompleted, int monthlyTotal) {
        
        // Estadísticas básicas
        totalActivitiesText.setText(String.valueOf(total));
        completedTodayText.setText(String.valueOf(completedToday));
        
        // Tasa de completación general
        int completionRate = total > 0 ? (totalCompleted * 100) / total : 0;
        completionRateText.setText(completionRate + "%");
        completionProgressBar.setProgress(completionRate);
        
        // Estadísticas semanales
        int weeklyRate = weeklyTotal > 0 ? (weeklyCompleted * 100) / weeklyTotal : 0;
        weeklyCompletionText.setText(weeklyRate + "%");
        
        // Estadísticas mensuales
        int monthlyRate = monthlyTotal > 0 ? (monthlyCompleted * 100) / monthlyTotal : 0;
        monthlyCompletionText.setText(monthlyRate + "%");
        
        // Racha (simulada)
        int streak = calculateStreak(completedToday);
        streakText.setText(streak + " días");
        
        // Última actividad
        lastActivityText.setText("Hace 2 horas"); // Simulado
        
        // Mensaje de voz con resumen
        String summary = "Resumen de estadísticas: " + completedToday + " actividades completadas hoy. " +
                        "Tasa de completación general: " + completionRate + " por ciento. " +
                        "Racha actual: " + streak + " días consecutivos.";
        speakText(summary);
    }

    private int calculateStreak(int completedToday) {
        // Simulación de racha - en implementación real calcularías días consecutivos
        return completedToday > 0 ? 5 : 0;
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
