package com.example.mirutinavisual;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;

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
    private CardView todayRoutineCard, profileCard, caregiverCard;
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
        
        // Inicializar SharedPreferences por usuario
        String userId = getCurrentUserId();
        sharedPreferences = getSharedPreferences("UserProfile_" + userId, MODE_PRIVATE);
        
        // Verificar si viene de una notificación
        checkNotificationIntent();
        
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
        profileCard = findViewById(R.id.profileCard);
        caregiverCard = findViewById(R.id.caregiverCard);
    }

    private void setupClickListeners() {
        todayRoutineCard.setOnClickListener(v -> {
            speakText("Abriendo mi rutina de hoy");
            Intent intent = new Intent(MainActivity.this, TodayRoutineActivity.class);
            startActivity(intent);
        });


        profileCard.setOnClickListener(v -> {
            speakText("Abriendo mi perfil");
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        caregiverCard.setOnClickListener(v -> {
            speakText("Acceso restringido. Ingrese contraseña del cuidador");
            showCaregiverPasswordDialog();
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
    
    // Método para verificar si viene de una notificación
    private void checkNotificationIntent() {
        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra("from_notification", false)) {
            String activityName = intent.getStringExtra("activity_name");
            String activityId = intent.getStringExtra("activity_id");
            
            if (activityName != null) {
                System.out.println("MAIN: Usuario llegó desde notificación de: " + activityName);
                showToast("🔔 Recordatorio: " + activityName);
                speakText("Tienes un recordatorio para " + activityName + ". Puedes ir a Mi Rutina de Hoy para verlo.");
                
                // Opcional: Resaltar el botón "Mi Rutina de Hoy"
                highlightTodayRoutineButton();
            }
        }
    }
    
    // Método para resaltar visualmente el botón de rutina de hoy
    private void highlightTodayRoutineButton() {
        if (todayRoutineCard != null) {
            // Animación sutil para llamar la atención
            todayRoutineCard.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(300)
                .withEndAction(() -> {
                    todayRoutineCard.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(300)
                        .start();
                })
                .start();
        }
    }

    private void loadUserData() {
        // Obtener usuario de Firebase
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        
        // Actualizar SharedPreferences con el usuario actual
        if (currentUser != null) {
            String userId = currentUser.getUid();
            sharedPreferences = getSharedPreferences("UserProfile_" + userId, MODE_PRIVATE);
        }
        
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
    
    private String getCurrentUserId() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            return user.getUid();
        }
        return "default_user"; // Fallback
    }

    private void showCaregiverPasswordDialog() {
        // Crear el diálogo de contraseña
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🔐 Acceso de Cuidador");
        builder.setMessage("Solo el cuidador puede acceder a esta sección.\nIngrese su contraseña:");

        // Crear campo de contraseña
        final EditText passwordInput = new EditText(this);
        passwordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setHint("Contraseña del cuidador");
        passwordInput.setTextSize(18);
        passwordInput.setTextColor(0xFF000000); // Negro
        passwordInput.setHintTextColor(0xFF888888); // Gris
        passwordInput.setPadding(50, 30, 50, 30);
        passwordInput.setBackgroundResource(android.R.drawable.edit_text);
        
        builder.setView(passwordInput);

        // Botón Acceder
        builder.setPositiveButton("🔑 Acceder", (dialog, which) -> {
            String enteredPassword = passwordInput.getText().toString().trim();
            
            if (enteredPassword.isEmpty()) {
                speakText("Debe ingresar una contraseña");
                showToast("Debe ingresar una contraseña");
                return;
            }
            
            // Verificar contraseña con Firebase Auth
            verifyPasswordAndAccess(enteredPassword);
        });

        // Botón Cancelar
        builder.setNegativeButton("❌ Cancelar", (dialog, which) -> {
            speakText("Acceso cancelado");
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
        
        // Hacer que el diálogo sea más accesible
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextSize(16);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextSize(16);
    }

    private void verifyPasswordAndAccess(String password) {
        // Obtener el email del usuario actual
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null || currentUser.getEmail() == null) {
            speakText("Error de autenticación");
            showToast("Error de autenticación");
            return;
        }

        String email = currentUser.getEmail();
        
        // Intentar autenticar con la contraseña ingresada
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Contraseña correcta - acceder al modo cuidador
                    speakText("Acceso autorizado. Entrando al modo cuidador");
                    showToast("✅ Acceso autorizado");
                    
                    Intent intent = new Intent(MainActivity.this, CaregiverModeActivity.class);
                    startActivity(intent);
                } else {
                    // Contraseña incorrecta
                    speakText("Contraseña incorrecta. Acceso denegado");
                    showToast("❌ Contraseña incorrecta");
                }
            })
            .addOnFailureListener(e -> {
                speakText("Error al verificar contraseña");
                showToast("Error al verificar contraseña: " + e.getMessage());
            });
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