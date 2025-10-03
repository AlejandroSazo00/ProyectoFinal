package com.example.mirutinavisual;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;

public class ReminderBroadcastReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String activityId = intent.getStringExtra("activity_id");
        String activityName = intent.getStringExtra("activity_name");
        String activityTime = intent.getStringExtra("activity_time");
        int pictogramId = intent.getIntExtra("pictogram_id", 0);
        String pictogramKeyword = intent.getStringExtra("pictogram_keyword");
        
        if (activityName != null && activityId != null) {
            // Crear servicio de notificaciones
            NotificationService notificationService = new NotificationService(context);
            
            // Mostrar notificación y pantalla completa
            notificationService.showActivityNotification(activityName, activityId, pictogramId);
            
            // Vibrar el dispositivo
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                // Patrón de vibración: esperar 0ms, vibrar 1000ms, esperar 500ms, vibrar 1000ms
                long[] pattern = {0, 1000, 500, 1000};
                vibrator.vibrate(pattern, -1); // -1 significa no repetir
            }
            
            // Reproducir sonido de notificación
            try {
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone ringtone = RingtoneManager.getRingtone(context, notification);
                if (ringtone != null) {
                    ringtone.play();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            // Programar la misma actividad para mañana
            // (Las rutinas se repiten diariamente)
            scheduleForTomorrow(context, activityId, activityName, activityTime, pictogramId, pictogramKeyword);
        }
    }
    
    private void scheduleForTomorrow(Context context, String activityId, String activityName, 
                                   String activityTime, int pictogramId, String pictogramKeyword) {
        // Crear una nueva actividad para mañana con los mismos datos
        Activity tomorrowActivity = new Activity();
        tomorrowActivity.setId(activityId + "_tomorrow");
        tomorrowActivity.setName(activityName);
        tomorrowActivity.setTime(activityTime != null ? activityTime : "08:00");
        tomorrowActivity.setPictogramId(pictogramId);
        tomorrowActivity.setPictogramKeyword(pictogramKeyword);
        
        // Programar para mañana
        NotificationService notificationService = new NotificationService(context);
        notificationService.scheduleActivityReminder(tomorrowActivity);
    }
}
