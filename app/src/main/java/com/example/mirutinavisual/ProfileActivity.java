package com.example.mirutinavisual;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int STORAGE_PERMISSION_CODE = 101;
    
    private TextToSpeech textToSpeech;
    private ImageView profileImageView;
    private EditText nameEditText, ageEditText;
    private Spinner needTypeSpinner;
    private Button saveButton, selectPhotoButton, takePhotoButton, rewardsButton;
    private ImageButton backButton;
    
    private SharedPreferences sharedPreferences;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        
        // Inicializar Text-to-Speech
        textToSpeech = new TextToSpeech(this, this);
        
        // Inicializar SharedPreferences por usuario
        String userId = getCurrentUserId();
        sharedPreferences = getSharedPreferences("UserProfile_" + userId, MODE_PRIVATE);
        
        // Inicializar vistas
        initViews();
        
        // Configurar launchers para cámara y galería
        setupActivityLaunchers();
        
        // Configurar listeners
        setupClickListeners();
        
        // Cargar datos guardados
        loadUserData();
        
        // Mensaje de bienvenida
        speakText("Configurar mi perfil");
    }

    private void initViews() {
        profileImageView = findViewById(R.id.profileImageView);
        nameEditText = findViewById(R.id.nameEditText);
        ageEditText = findViewById(R.id.ageEditText);
        needTypeSpinner = findViewById(R.id.needTypeSpinner);
        saveButton = findViewById(R.id.saveButton);
        selectPhotoButton = findViewById(R.id.selectPhotoButton);
        takePhotoButton = findViewById(R.id.takePhotoButton);
        rewardsButton = findViewById(R.id.rewardsButton);
        backButton = findViewById(R.id.backButton);
        
        // Configurar spinner con tipos de necesidades
        String[] needTypes = {
            "Seleccionar tipo de necesidad",
            "Autismo (TEA)",
            "Síndrome de Down",
            "Discapacidad cognitiva leve",
            "TDAH",
            "Otro"
        };
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, needTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        needTypeSpinner.setAdapter(adapter);
    }

    private void setupActivityLaunchers() {
        // Launcher para cámara
        cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null) {
                        Bitmap imageBitmap = (Bitmap) extras.get("data");
                        profileImageView.setImageBitmap(imageBitmap);
                        speakText("Foto tomada correctamente");
                    }
                }
            }
        );

        // Launcher para galería
        galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                            getContentResolver(), imageUri);
                        profileImageView.setImageBitmap(bitmap);
                        speakText("Foto seleccionada correctamente");
                    } catch (IOException e) {
                        showToast("Error al cargar la imagen");
                    }
                }
            }
        );
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> {
            speakText("Volver");
            finish();
        });

        takePhotoButton.setOnClickListener(v -> {
            speakText("Tomar foto");
            if (checkCameraPermission()) {
                openCamera();
            } else {
                requestCameraPermission();
            }
        });

        selectPhotoButton.setOnClickListener(v -> {
            speakText("Seleccionar foto");
            if (checkStoragePermission()) {
                openGallery();
            } else {
                requestStoragePermission();
            }
        });

        saveButton.setOnClickListener(v -> {
            speakText("Guardar perfil");
            saveUserData();
        });

        rewardsButton.setOnClickListener(v -> {
            speakText("Abrir mis recompensas");
            Intent intent = new Intent(ProfileActivity.this, RewardsActivity.class);
            startActivity(intent);
        });

        profileImageView.setOnClickListener(v -> {
            speakText("Mi foto de perfil. Toca los botones para cambiarla");
        });

        nameEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                speakText("Escribe tu nombre");
            }
        });

        ageEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                speakText("Escribe tu edad");
            }
        });
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            == PackageManager.PERMISSION_GRANTED;
    }

    private boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
            == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, 
            new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, 
            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(cameraIntent);
        }
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, 
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(galleryIntent);
    }

    private void saveUserData() {
        String name = nameEditText.getText().toString().trim();
        String age = ageEditText.getText().toString().trim();
        int needTypePosition = needTypeSpinner.getSelectedItemPosition();

        if (name.isEmpty()) {
            speakText("Por favor escribe tu nombre");
            nameEditText.requestFocus();
            return;
        }

        if (age.isEmpty()) {
            speakText("Por favor escribe tu edad");
            ageEditText.requestFocus();
            return;
        }

        if (needTypePosition == 0) {
            speakText("Por favor selecciona tu tipo de necesidad");
            return;
        }

        // Guardar en SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("user_name", name);
        editor.putString("user_age", age);
        editor.putInt("need_type", needTypePosition);
        editor.putBoolean("profile_completed", true);
        editor.apply();

        speakText("Perfil guardado correctamente");
        showToast("Perfil guardado exitosamente");
        
        // Volver a la pantalla principal
        finish();
    }

    private void loadUserData() {
        String name = sharedPreferences.getString("user_name", "");
        String age = sharedPreferences.getString("user_age", "");
        int needType = sharedPreferences.getInt("need_type", 0);

        nameEditText.setText(name);
        ageEditText.setText(age);
        needTypeSpinner.setSelection(needType);
        
        // Cargar avatar (pictograma o foto)
        loadUserAvatar();
    }
    
    private void loadUserAvatar() {
        String avatarType = sharedPreferences.getString("avatar_type", "");
        
        if ("pictogram".equals(avatarType)) {
            // Cargar pictograma como avatar
            String pictogramId = sharedPreferences.getString("avatar_pictogram_id", "");
            if (!pictogramId.isEmpty()) {
                loadPictogramAsAvatar(pictogramId);
            } else {
                // Avatar por defecto
                profileImageView.setImageResource(R.drawable.ic_profile_default);
            }
        } else {
            // Intentar cargar foto guardada (funcionalidad existente)
            String imagePath = sharedPreferences.getString("profile_image_path", "");
            if (!imagePath.isEmpty()) {
                try {
                    android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(imagePath);
                    if (bitmap != null) {
                        profileImageView.setImageBitmap(bitmap);
                    } else {
                        profileImageView.setImageResource(R.drawable.ic_profile_default);
                    }
                } catch (Exception e) {
                    System.out.println("PROFILE: Error al cargar imagen: " + e.getMessage());
                    profileImageView.setImageResource(R.drawable.ic_profile_default);
                }
            } else {
                profileImageView.setImageResource(R.drawable.ic_profile_default);
            }
        }
    }
    
    private void loadPictogramAsAvatar(String pictogramId) {
        String imageUrl = "https://api.arasaac.org/api/pictograms/" + pictogramId + "?download=false";
        
        // Usar Glide para cargar la imagen
        com.bumptech.glide.Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.ic_profile_default)
            .error(R.drawable.ic_profile_default)
            .circleCrop() // Hacer la imagen circular
            .into(profileImageView);
            
        System.out.println("PROFILE: Cargando pictograma como avatar: " + pictogramId);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Recargar avatar por si se cambió en RewardsActivity
        loadUserAvatar();
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                showToast("Permiso de cámara denegado");
            }
        } else if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                showToast("Permiso de almacenamiento denegado");
            }
        }
    }
    
    private String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            return user.getUid();
        }
        return "default_user"; // Fallback
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
