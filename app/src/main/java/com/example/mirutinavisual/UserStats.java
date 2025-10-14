package com.example.mirutinavisual;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UserStats {
    private String userId;
    private int totalActivitiesCompleted;
    private int activitiesCompletedToday;
    private int currentStreak; // días consecutivos
    private int maxStreak; // mejor racha
    private String lastActivityDate; // formato "yyyy-MM-dd"
    private int totalPoints;
    private int unlockedAchievements;

    public UserStats() {
        // Constructor vacío requerido por Firebase
    }

    public UserStats(String userId) {
        this.userId = userId;
        this.totalActivitiesCompleted = 0;
        this.activitiesCompletedToday = 0;
        this.currentStreak = 0;
        this.maxStreak = 0;
        this.lastActivityDate = "";
        this.totalPoints = 0;
        this.unlockedAchievements = 0;
    }

    // Método para actualizar estadísticas cuando se completa una actividad
    public void onActivityCompleted() {
        String today = getCurrentDateString();
        
        // Incrementar totales
        totalActivitiesCompleted++;
        totalPoints += 10; // 10 puntos por actividad
        
        // Verificar si es el primer completado del día
        if (!today.equals(lastActivityDate)) {
            // Nuevo día
            if (isConsecutiveDay(lastActivityDate, today)) {
                // Día consecutivo - incrementar racha
                currentStreak++;
                if (currentStreak > maxStreak) {
                    maxStreak = currentStreak;
                }
            } else if (!lastActivityDate.isEmpty()) {
                // Se rompió la racha
                currentStreak = 1;
            } else {
                // Primera actividad ever
                currentStreak = 1;
            }
            
            activitiesCompletedToday = 1;
            lastActivityDate = today;
        } else {
            // Mismo día - incrementar contador diario
            activitiesCompletedToday++;
        }
        
        System.out.println("STATS: Actividad completada - Total: " + totalActivitiesCompleted + 
                         ", Hoy: " + activitiesCompletedToday + ", Racha: " + currentStreak);
    }

    // Verificar si dos fechas son días consecutivos
    private boolean isConsecutiveDay(String lastDate, String currentDate) {
        if (lastDate.isEmpty()) return false;
        
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date last = sdf.parse(lastDate);
            Date current = sdf.parse(currentDate);
            
            if (last != null && current != null) {
                long diffInMillis = current.getTime() - last.getTime();
                long diffInDays = diffInMillis / (24 * 60 * 60 * 1000);
                return diffInDays == 1;
            }
        } catch (Exception e) {
            System.out.println("STATS: Error al comparar fechas: " + e.getMessage());
        }
        
        return false;
    }

    // Obtener fecha actual como string
    private String getCurrentDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    // Resetear contador diario (llamar al cambio de día)
    public void resetDailyCount() {
        String today = getCurrentDateString();
        if (!today.equals(lastActivityDate)) {
            activitiesCompletedToday = 0;
        }
    }

    // Getters y Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public int getTotalActivitiesCompleted() { return totalActivitiesCompleted; }
    public void setTotalActivitiesCompleted(int totalActivitiesCompleted) { 
        this.totalActivitiesCompleted = totalActivitiesCompleted; 
    }

    public int getActivitiesCompletedToday() { return activitiesCompletedToday; }
    public void setActivitiesCompletedToday(int activitiesCompletedToday) { 
        this.activitiesCompletedToday = activitiesCompletedToday; 
    }

    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }

    public int getMaxStreak() { return maxStreak; }
    public void setMaxStreak(int maxStreak) { this.maxStreak = maxStreak; }

    public String getLastActivityDate() { return lastActivityDate; }
    public void setLastActivityDate(String lastActivityDate) { this.lastActivityDate = lastActivityDate; }

    public int getTotalPoints() { return totalPoints; }
    public void setTotalPoints(int totalPoints) { this.totalPoints = totalPoints; }

    public int getUnlockedAchievements() { return unlockedAchievements; }
    public void setUnlockedAchievements(int unlockedAchievements) { 
        this.unlockedAchievements = unlockedAchievements; 
    }
}
