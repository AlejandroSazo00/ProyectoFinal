package com.example.mirutinavisual;

import android.content.Context;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AchievementManager {
    
    public interface OnAchievementUnlockedListener {
        void onAchievementUnlocked(Achievement achievement);
    }
    
    private Context context;
    private DatabaseReference databaseReference;
    private FirebaseAuth firebaseAuth;
    private OnAchievementUnlockedListener listener;
    
    private List<Achievement> allAchievements;
    private UserStats currentUserStats;
    
    public AchievementManager(Context context) {
        this.context = context;
        this.databaseReference = FirebaseDatabase.getInstance().getReference();
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.allAchievements = new ArrayList<>();
        
        initializeAchievements();
    }
    
    public void setOnAchievementUnlockedListener(OnAchievementUnlockedListener listener) {
        this.listener = listener;
    }
    
    // Inicializar logros predefinidos
    private void initializeAchievements() {
        allAchievements.clear();
        
        // LOGROS DIARIOS
        allAchievements.add(new Achievement(
            "daily_bronze", "🥉 Bronce Diario", "Completa 1 actividad en un día",
            "", "daily", 1, "2557" // pictograma de medalla
        ));
        
        allAchievements.add(new Achievement(
            "daily_silver", "🥈 Plata Diario", "Completa 3 actividades en un día",
            "", "daily", 3, "2558" // pictograma de trofeo
        ));
        
        allAchievements.add(new Achievement(
            "daily_gold", "🥇 Oro Diario", "Completa 5 actividades en un día",
            "", "daily", 5, "2559" // pictograma de corona
        ));
        
        // LOGROS DE RACHA
        allAchievements.add(new Achievement(
            "streak_fire", "🔥 Racha de Fuego", "Completa actividades 3 días seguidos",
            "", "streak", 3, "8566" // pictograma de fuego
        ));
        
        allAchievements.add(new Achievement(
            "streak_diamond", "💎 Diamante Semanal", "Completa actividades 7 días seguidos",
            "", "streak", 7, "2595" // pictograma de diamante
        ));
        
        allAchievements.add(new Achievement(
            "streak_crown", "👑 Corona de Campeón", "Completa actividades 30 días seguidos",
            "", "streak", 30, "2560" // pictograma de rey
        ));
        
        // LOGROS ESPECIALES
        allAchievements.add(new Achievement(
            "explorer", "🌟 Explorador", "Completa tu primera actividad",
            "", "special", 1, "2561" // pictograma de estrella
        ));
        
        allAchievements.add(new Achievement(
            "veteran", "🎉 Veterano", "Usa la app durante 10 días",
            "", "special", 10, "2562" // pictograma de celebración
        ));
        
        allAchievements.add(new Achievement(
            "perfectionist", "🎯 Perfeccionista", "Completa 50 actividades en total",
            "", "special", 50, "2563" // pictograma de diana
        ));
        
        System.out.println("ACHIEVEMENTS: Inicializados " + allAchievements.size() + " logros");
    }
    
    // Método principal: verificar logros cuando se completa una actividad
    public void onActivityCompleted() {
        if (firebaseAuth.getCurrentUser() == null) return;
        
        String userId = firebaseAuth.getCurrentUser().getUid();
        
        // Cargar estadísticas actuales
        loadUserStats(userId, () -> {
            // Actualizar estadísticas
            if (currentUserStats == null) {
                currentUserStats = new UserStats(userId);
            }
            
            currentUserStats.onActivityCompleted();
            
            // Guardar estadísticas actualizadas
            saveUserStats();
            
            // Verificar nuevos logros
            checkForNewAchievements();
        });
    }
    
    // Cargar estadísticas del usuario
    private void loadUserStats(String userId, Runnable onComplete) {
        databaseReference.child("userStats").child(userId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        currentUserStats = dataSnapshot.getValue(UserStats.class);
                    } else {
                        currentUserStats = new UserStats(userId);
                    }
                    
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
                
                @Override
                public void onCancelled(DatabaseError databaseError) {
                    System.out.println("ACHIEVEMENTS: Error al cargar stats: " + databaseError.getMessage());
                    currentUserStats = new UserStats(userId);
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            });
    }
    
    // Guardar estadísticas del usuario
    private void saveUserStats() {
        if (currentUserStats != null && firebaseAuth.getCurrentUser() != null) {
            String userId = firebaseAuth.getCurrentUser().getUid();
            databaseReference.child("userStats").child(userId)
                .setValue(currentUserStats)
                .addOnSuccessListener(aVoid -> {
                    System.out.println("ACHIEVEMENTS: Estadísticas guardadas");
                })
                .addOnFailureListener(e -> {
                    System.out.println("ACHIEVEMENTS: Error al guardar stats: " + e.getMessage());
                });
        }
    }
    
    // Verificar si se desbloquearon nuevos logros
    private void checkForNewAchievements() {
        if (currentUserStats == null || firebaseAuth.getCurrentUser() == null) return;
        
        String userId = firebaseAuth.getCurrentUser().getUid();
        
        for (Achievement achievement : allAchievements) {
            if (!achievement.isUnlocked()) {
                boolean shouldUnlock = false;
                
                switch (achievement.getCategory()) {
                    case "daily":
                        shouldUnlock = currentUserStats.getActivitiesCompletedToday() >= achievement.getRequiredValue();
                        break;
                    case "streak":
                        shouldUnlock = currentUserStats.getCurrentStreak() >= achievement.getRequiredValue();
                        break;
                    case "special":
                        if (achievement.getId().equals("explorer")) {
                            shouldUnlock = currentUserStats.getTotalActivitiesCompleted() >= 1;
                        } else if (achievement.getId().equals("perfectionist")) {
                            shouldUnlock = currentUserStats.getTotalActivitiesCompleted() >= 50;
                        }
                        break;
                }
                
                if (shouldUnlock) {
                    unlockAchievement(userId, achievement);
                }
            }
        }
    }
    
    // Desbloquear un logro
    private void unlockAchievement(String userId, Achievement achievement) {
        achievement.setUnlocked(true);
        achievement.setUnlockedDate(System.currentTimeMillis());
        
        // Guardar en Firebase
        Map<String, Object> achievementData = new HashMap<>();
        achievementData.put("unlocked", true);
        achievementData.put("unlockedDate", achievement.getUnlockedDate());
        
        databaseReference.child("userAchievements").child(userId).child(achievement.getId())
            .updateChildren(achievementData)
            .addOnSuccessListener(aVoid -> {
                System.out.println("ACHIEVEMENTS: Logro desbloqueado: " + achievement.getName());
                
                // Actualizar contador de logros
                currentUserStats.setUnlockedAchievements(currentUserStats.getUnlockedAchievements() + 1);
                saveUserStats();
                
                // Notificar al listener
                if (listener != null) {
                    listener.onAchievementUnlocked(achievement);
                }
                
                // Mostrar toast
                Toast.makeText(context, "🎉 ¡Nuevo logro desbloqueado!\n" + achievement.getName(), 
                             Toast.LENGTH_LONG).show();
            })
            .addOnFailureListener(e -> {
                System.out.println("ACHIEVEMENTS: Error al desbloquear logro: " + e.getMessage());
            });
    }
    
    // Obtener todos los logros del usuario
    public void getUserAchievements(String userId, OnAchievementsLoadedListener listener) {
        databaseReference.child("userAchievements").child(userId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    List<Achievement> userAchievements = new ArrayList<>();
                    
                    for (Achievement achievement : allAchievements) {
                        Achievement userAchievement = new Achievement(
                            achievement.getId(),
                            achievement.getName(),
                            achievement.getDescription(),
                            achievement.getIconUrl(),
                            achievement.getCategory(),
                            achievement.getRequiredValue(),
                            achievement.getPictogramId()
                        );
                        
                        // Verificar si está desbloqueado
                        if (dataSnapshot.hasChild(achievement.getId())) {
                            DataSnapshot achSnapshot = dataSnapshot.child(achievement.getId());
                            Boolean unlocked = achSnapshot.child("unlocked").getValue(Boolean.class);
                            Long unlockedDate = achSnapshot.child("unlockedDate").getValue(Long.class);
                            
                            if (unlocked != null && unlocked) {
                                userAchievement.setUnlocked(true);
                                userAchievement.setUnlockedDate(unlockedDate != null ? unlockedDate : 0);
                            }
                        }
                        
                        userAchievements.add(userAchievement);
                    }
                    
                    listener.onAchievementsLoaded(userAchievements);
                }
                
                @Override
                public void onCancelled(DatabaseError databaseError) {
                    System.out.println("ACHIEVEMENTS: Error al cargar logros: " + databaseError.getMessage());
                    listener.onAchievementsLoaded(new ArrayList<>());
                }
            });
    }
    
    public interface OnAchievementsLoadedListener {
        void onAchievementsLoaded(List<Achievement> achievements);
    }
    
    // Obtener estadísticas del usuario
    public void getUserStats(String userId, OnStatsLoadedListener listener) {
        loadUserStats(userId, () -> {
            if (listener != null) {
                listener.onStatsLoaded(currentUserStats != null ? currentUserStats : new UserStats(userId));
            }
        });
    }
    
    public interface OnStatsLoadedListener {
        void onStatsLoaded(UserStats stats);
    }
}
