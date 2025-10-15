package com.example.mirutinavisual;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RewardsActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextToSpeech textToSpeech;
    private FirebaseAuth firebaseAuth;
    private AchievementManager achievementManager;

    // Views
    private ImageButton backButton;
    private TextView totalActivitiesText, currentStreakText, totalTrophiesText;
    private RecyclerView dailyAchievementsRecyclerView, streakAchievementsRecyclerView, specialAchievementsRecyclerView;

    // Adapters
    private AchievementAdapter dailyAdapter, streakAdapter, specialAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rewards);

        // Inicializar Firebase
        firebaseAuth = FirebaseAuth.getInstance();

        // Inicializar Text-to-Speech
        textToSpeech = new TextToSpeech(this, this);

        // Inicializar sistema de logros
        achievementManager = new AchievementManager(this);

        // Inicializar vistas
        initViews();

        // Configurar listeners
        setupClickListeners();

        // Configurar RecyclerViews
        setupRecyclerViews();

        // Cargar datos
        loadUserData();

        // Mensaje de bienvenida
        speakText("Pantalla de recompensas. Aquí puedes ver todos tus logros y trofeos ganados");
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        totalActivitiesText = findViewById(R.id.totalActivitiesText);
        currentStreakText = findViewById(R.id.currentStreakText);
        totalTrophiesText = findViewById(R.id.totalTrophiesText);
        dailyAchievementsRecyclerView = findViewById(R.id.dailyAchievementsRecyclerView);
        streakAchievementsRecyclerView = findViewById(R.id.streakAchievementsRecyclerView);
        specialAchievementsRecyclerView = findViewById(R.id.specialAchievementsRecyclerView);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> {
            speakText("Volver");
            finish();
        });
    }

    private void setupRecyclerViews() {
        // Configurar RecyclerViews
        dailyAchievementsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        streakAchievementsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        specialAchievementsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Inicializar adapters
        dailyAdapter = new AchievementAdapter(new ArrayList<>(), achievement -> {
            if (achievement.isUnlocked()) {
                speakText("Logro desbloqueado: " + achievement.getName() + ". " + achievement.getDescription());
                // Aquí se puede agregar funcionalidad para usar como avatar
                showUseAsAvatarDialog(achievement);
            } else {
                speakText("Logro bloqueado: " + achievement.getDescription());
            }
        });

        streakAdapter = new AchievementAdapter(new ArrayList<>(), achievement -> {
            if (achievement.isUnlocked()) {
                speakText("Logro desbloqueado: " + achievement.getName() + ". " + achievement.getDescription());
                showUseAsAvatarDialog(achievement);
            } else {
                speakText("Logro bloqueado: " + achievement.getDescription());
            }
        });

        specialAdapter = new AchievementAdapter(new ArrayList<>(), achievement -> {
            if (achievement.isUnlocked()) {
                speakText("Logro desbloqueado: " + achievement.getName() + ". " + achievement.getDescription());
                showUseAsAvatarDialog(achievement);
            } else {
                speakText("Logro bloqueado: " + achievement.getDescription());
            }
        });

        // Asignar adapters
        dailyAchievementsRecyclerView.setAdapter(dailyAdapter);
        streakAchievementsRecyclerView.setAdapter(streakAdapter);
        specialAchievementsRecyclerView.setAdapter(specialAdapter);
    }

    private void loadUserData() {
        if (firebaseAuth.getCurrentUser() == null) {
            finish();
            return;
        }

        String userId = firebaseAuth.getCurrentUser().getUid();

        // Cargar estadísticas del usuario
        achievementManager.getUserStats(userId, stats -> {
            runOnUiThread(() -> {
                totalActivitiesText.setText(String.valueOf(stats.getTotalActivitiesCompleted()));
                currentStreakText.setText(String.valueOf(stats.getCurrentStreak()));
                totalTrophiesText.setText(String.valueOf(stats.getUnlockedAchievements()));
            });
        });

        // Cargar logros del usuario
        achievementManager.getUserAchievements(userId, achievements -> {
            runOnUiThread(() -> {
                // Separar logros por categoría
                List<Achievement> dailyAchievements = new ArrayList<>();
                List<Achievement> streakAchievements = new ArrayList<>();
                List<Achievement> specialAchievements = new ArrayList<>();

                for (Achievement achievement : achievements) {
                    switch (achievement.getCategory()) {
                        case "daily":
                            dailyAchievements.add(achievement);
                            break;
                        case "streak":
                            streakAchievements.add(achievement);
                            break;
                        case "special":
                            specialAchievements.add(achievement);
                            break;
                    }
                }

                // Actualizar adapters
                dailyAdapter.updateAchievements(dailyAchievements);
                streakAdapter.updateAchievements(streakAchievements);
                specialAdapter.updateAchievements(specialAchievements);
            });
        });
    }

    private void showUseAsAvatarDialog(Achievement achievement) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("🎨 Usar como Avatar")
            .setMessage("¿Quieres usar este pictograma como tu foto de perfil?")
            .setPositiveButton("Sí, usar", (dialog, which) -> {
                // Guardar pictograma como avatar
                saveAvatarPictogram(achievement.getPictogramId());
                speakText("¡Perfecto! Tu nuevo avatar ha sido guardado. Ve a tu perfil para verlo");
                
                // Mostrar confirmación
                android.widget.Toast.makeText(this, "🎉 ¡Avatar actualizado!", android.widget.Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }
    
    private void saveAvatarPictogram(String pictogramId) {
        String userId = getCurrentUserId();
        android.content.SharedPreferences sharedPreferences = getSharedPreferences("UserProfile_" + userId, MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = sharedPreferences.edit();
        
        // Guardar el ID del pictograma como avatar
        editor.putString("avatar_pictogram_id", pictogramId);
        editor.putString("avatar_type", "pictogram"); // Indicar que es un pictograma, no una foto
        editor.apply();
        
        System.out.println("REWARDS: Avatar pictograma guardado: " + pictogramId);
    }
    
    private String getCurrentUserId() {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            return user.getUid();
        }
        return "default_user"; // Fallback
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(new Locale("es", "ES"));
            
            if (result == TextToSpeech.LANG_MISSING_DATA || 
                result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech.setLanguage(Locale.US);
            }
        }
    }

    private void speakText(String text) {
        if (textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
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
