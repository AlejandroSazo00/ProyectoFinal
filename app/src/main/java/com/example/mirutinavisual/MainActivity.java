package com.example.mirutinavisual;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextToSpeech textToSpeech;
    private TextView welcomeText, userNameText;
    private ImageView profileImage;
    private ImageButton settingsButton;
    private CardView todayRoutineCard, createRoutineCard, profileCard, caregiverCard;
    private SharedPreferences sharedPreferences;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        // Inicializar Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();
        
        // Inicializar Text-to-Speech
        textToSpeech = new TextToSpeech(this, this);
        
        // Inicializar SharedPreferences
        sharedPreferences = getSharedPreferences("UserProfile", MODE_PRIVATE);
        
        // Inicializar vistas
        initViews();
        
        // Configurar listeners
        setupClickListeners();
        
        // Cargar datos del usuario
        loadUserData();
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        // Mensaje de bienvenida con voz
        speakText("¡Bienvenido a Mi Rutina Visual!");
    }

    private void initViews() {
        welcomeText = findViewById(R.id.welcomeText);
        userNameText = findViewById(R.id.userNameText);
        profileImage = findViewById(R.id.profileImage);
        settingsButton = findViewById(R.id.settingsButton);
        
        todayRoutineCard = findViewById(R.id.todayRoutineCard);
        createRoutineCard = findViewById(R.id.createRoutineCard);
        profileCard = findViewById(R.id.profileCard);
        caregiverCard = findViewById(R.id.caregiverCard);
    }

    private void setupClickListeners() {
        todayRoutineCard.setOnClickListener(v -> {
            speakText("Abriendo mi rutina de hoy");
            Intent intent = new Intent(MainActivity.this, TodayRoutineActivity.class);
            startActivity(intent);
        });

        createRoutineCard.setOnClickListener(v -> {
            speakText("Crear nueva rutina");
            Intent intent = new Intent(MainActivity.this, CreateRoutineActivity.class);
            startActivity(intent);
        });

        profileCard.setOnClickListener(v -> {
            speakText("Abriendo mi perfil");
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        caregiverCard.setOnClickListener(v -> {
            speakText("Activando modo cuidador");
            Intent intent = new Intent(MainActivity.this, CaregiverModeActivity.class);
            startActivity(intent);
        });

        settingsButton.setOnClickListener(v -> {
            speakText("Cerrar sesión");
            firebaseAuth.signOut();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        profileImage.setOnClickListener(v -> {
            speakText("Mi foto de perfil");
            showToast("Toca para cambiar foto");
        });
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            // Configurar idioma español
            int result = textToSpeech.setLanguage(new Locale("es", "ES"));
            
            if (result == TextToSpeech.LANG_MISSING_DATA || 
                result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Si español no está disponible, usar inglés
                textToSpeech.setLanguage(Locale.US);
            }
            
            // Configurar velocidad de habla más lenta para mejor comprensión
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

    private void loadUserData() {
        // Obtener usuario de Firebase
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        
        String userName = sharedPreferences.getString("user_name", "");
        if (!userName.isEmpty()) {
            welcomeText.setText("¡Hola " + userName + "!");
            userNameText.setText("Mi Rutina Visual");
        } else if (currentUser != null && currentUser.getEmail() != null) {
            // Usar email de Firebase si no hay nombre local
            String email = currentUser.getEmail();
            String displayName = email.substring(0, email.indexOf("@"));
            welcomeText.setText("¡Hola " + displayName + "!");
            userNameText.setText("Mi Rutina Visual");
        } else {
            welcomeText.setText("¡Hola!");
            userNameText.setText("Configura tu perfil");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Actualizar datos del usuario cuando regrese de la pantalla de perfil
        loadUserData();
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