package com.example.mirutinavisual;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.ImageButton;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CaregiverModeActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextToSpeech textToSpeech;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    
    private ImageButton backButton;
    private TextView totalActivitiesText, completedActivitiesText, pendingActivitiesText;
    private CardView manageActivitiesCard, viewStatisticsCard, settingsCard, helpCard;
    private RecyclerView recentActivitiesRecyclerView;
    
    private ActivityAdapter recentActivitiesAdapter;
    private List<Activity> recentActivitiesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caregiver_mode);
        
        // Inicializar Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        
        // Inicializar Text-to-Speech
        textToSpeech = new TextToSpeech(this, this);
        
        // Inicializar lista
        recentActivitiesList = new ArrayList<>();
        
        // Inicializar vistas
        initViews();
        
        // Configurar RecyclerView
        setupRecyclerView();
        
        // Configurar listeners
        setupClickListeners();
        
        // Cargar estadísticas
        loadStatistics();
        
        // Mensaje de bienvenida
        speakText("Modo cuidador activado. Aquí puedes administrar las rutinas y ver estadísticas");
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        totalActivitiesText = findViewById(R.id.totalActivitiesText);
        completedActivitiesText = findViewById(R.id.completedActivitiesText);
        pendingActivitiesText = findViewById(R.id.pendingActivitiesText);
        
        manageActivitiesCard = findViewById(R.id.manageActivitiesCard);
        viewStatisticsCard = findViewById(R.id.viewStatisticsCard);
        settingsCard = findViewById(R.id.settingsCard);
        helpCard = findViewById(R.id.helpCard);
        
        recentActivitiesRecyclerView = findViewById(R.id.recentActivitiesRecyclerView);
    }

    private void setupRecyclerView() {
        recentActivitiesAdapter = new ActivityAdapter(recentActivitiesList, new ActivityAdapter.OnActivityClickListener() {
            @Override
            public void onActivityClick(Activity activity) {
                showActivityManagementOptions(activity);
            }

            @Override
            public void onActivityComplete(Activity activity) {
                // En modo cuidador, no permitir completar desde aquí
                speakText("Usa las opciones de administración para modificar esta actividad");
                showToast("Usa el menú de administración");
            }

            @Override
            public void onActivitySpeak(Activity activity) {
                speakActivityDetails(activity);
            }
        });
        
        recentActivitiesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        recentActivitiesRecyclerView.setAdapter(recentActivitiesAdapter);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> {
            speakText("Saliendo del modo cuidador");
            finish();
        });

        manageActivitiesCard.setOnClickListener(v -> {
            speakText("Administrar actividades");
            Intent intent = new Intent(CaregiverModeActivity.this, ManageActivitiesActivity.class);
            startActivity(intent);
        });

        viewStatisticsCard.setOnClickListener(v -> {
            speakText("Ver estadísticas detalladas");
            Intent intent = new Intent(CaregiverModeActivity.this, StatisticsActivity.class);
            startActivity(intent);
        });

        settingsCard.setOnClickListener(v -> {
            speakText("Configuración del cuidador");
            showCaregiverSettings();
        });

        helpCard.setOnClickListener(v -> {
            speakText("Ayuda y guía de uso");
            showHelpDialog();
        });
    }

    private void loadStatistics() {
        if (firebaseAuth.getCurrentUser() == null) return;
        
        String userId = firebaseAuth.getCurrentUser().getUid();
        
        databaseReference.child("activities")
                .orderByChild("userId")
                .equalTo(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        int totalActivities = 0;
                        int completedActivities = 0;
                        int pendingActivities = 0;
                        
                        recentActivitiesList.clear();
                        
                        for (DataSnapshot activitySnapshot : dataSnapshot.getChildren()) {
                            Activity activity = activitySnapshot.getValue(Activity.class);
                            if (activity != null) {
                                activity.setId(activitySnapshot.getKey());
                                
                                totalActivities++;
                                if (activity.isCompleted()) {
                                    completedActivities++;
                                } else {
                                    pendingActivities++;
                                }
                                
                                // Agregar a la lista de actividades recientes (máximo 5)
                                if (recentActivitiesList.size() < 5) {
                                    recentActivitiesList.add(activity);
                                }
                            }
                        }
                        
                        // Actualizar estadísticas en la UI
                        updateStatisticsUI(totalActivities, completedActivities, pendingActivities);
                        
                        // Actualizar lista de actividades recientes
                        recentActivitiesAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        showToast("Error al cargar estadísticas: " + databaseError.getMessage());
                    }
                });
    }

    private void updateStatisticsUI(int total, int completed, int pending) {
        totalActivitiesText.setText(String.valueOf(total));
        completedActivitiesText.setText(String.valueOf(completed));
        pendingActivitiesText.setText(String.valueOf(pending));
    }

    private void showActivityManagementOptions(Activity activity) {
        // Crear diálogo con opciones de administración
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Administrar: " + activity.getName());
        
        String[] options = {
            "✏️ Editar actividad",
            "🗑️ Eliminar actividad",
            "🔄 Reprogramar hora",
            "📊 Ver estadísticas",
            "🔔 Configurar recordatorio"
        };
        
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // Editar
                    editActivity(activity);
                    break;
                case 1: // Eliminar
                    deleteActivity(activity);
                    break;
                case 2: // Reprogramar
                    rescheduleActivity(activity);
                    break;
                case 3: // Estadísticas
                    showActivityStatistics(activity);
                    break;
                case 4: // Recordatorio
                    configureReminder(activity);
                    break;
            }
        });
        
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void editActivity(Activity activity) {
        speakText("Editando actividad: " + activity.getName());
        Intent intent = new Intent(CaregiverModeActivity.this, CreateRoutineActivity.class);
        intent.putExtra("edit_mode", true);
        intent.putExtra("activity_id", activity.getId());
        intent.putExtra("activity_name", activity.getName());
        intent.putExtra("activity_time", activity.getTime());
        intent.putExtra("pictogram_id", activity.getPictogramId());
        startActivity(intent);
    }

    private void deleteActivity(Activity activity) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Eliminar Actividad");
        builder.setMessage("¿Estás seguro de que quieres eliminar '" + activity.getName() + "'?");
        
        builder.setPositiveButton("Eliminar", (dialog, which) -> {
            if (activity.getId() != null) {
                databaseReference.child("activities").child(activity.getId())
                        .removeValue()
                        .addOnSuccessListener(aVoid -> {
                            speakText("Actividad eliminada: " + activity.getName());
                            showToast("Actividad eliminada");
                            
                            // Cancelar recordatorio
                            NotificationService notificationService = new NotificationService(this);
                            notificationService.cancelActivityReminder(activity.getId());
                        })
                        .addOnFailureListener(e -> {
                            speakText("Error al eliminar actividad");
                            showToast("Error al eliminar");
                        });
            }
        });
        
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void rescheduleActivity(Activity activity) {
        speakText("Reprogramando actividad");
        showToast("Función de reprogramación - Próximamente");
        // TODO: Implementar diálogo de selección de nueva hora
    }

    private void showActivityStatistics(Activity activity) {
        speakText("Mostrando estadísticas de " + activity.getName());
        showToast("Estadísticas de actividad - Próximamente");
        // TODO: Mostrar estadísticas específicas de la actividad
    }

    private void configureReminder(Activity activity) {
        speakText("Configurando recordatorio para " + activity.getName());
        
        // Reprogramar recordatorio
        NotificationService notificationService = new NotificationService(this);
        notificationService.scheduleActivityReminder(activity);
        
        showToast("Recordatorio reconfigurado");
    }

    private void showCaregiverSettings() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Configuración del Cuidador");
        builder.setMessage("Configuraciones disponibles:\n\n" +
                "• Horarios de notificación\n" +
                "• Frecuencia de recordatorios\n" +
                "• Configuración de voz\n" +
                "• Backup de datos\n" +
                "• Modo sin conexión");
        
        builder.setPositiveButton("Entendido", null);
        builder.show();
    }

    private void showHelpDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Guía del Modo Cuidador");
        builder.setMessage("El Modo Cuidador te permite:\n\n" +
                "📱 Administrar rutinas de usuarios\n" +
                "📊 Ver estadísticas de cumplimiento\n" +
                "⏰ Configurar recordatorios\n" +
                "✏️ Editar o eliminar actividades\n" +
                "🔔 Personalizar notificaciones\n\n" +
                "Ideal para padres, maestros y terapeutas que trabajan con personas con discapacidad cognitiva.");
        
        builder.setPositiveButton("Entendido", null);
        builder.show();
    }

    private void speakActivityDetails(Activity activity) {
        String message = "Actividad: " + activity.getName() + 
                        ". Programada para las " + activity.getTime();
        
        if (activity.isCompleted()) {
            message += ". Estado: Completada";
        } else {
            message += ". Estado: Pendiente";
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

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}
