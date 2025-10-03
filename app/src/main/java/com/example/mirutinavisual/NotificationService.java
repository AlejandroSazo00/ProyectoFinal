package com.example.mirutinavisual;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Calendar;

public class NotificationService {
    
    private static final String CHANNEL_ID = "ROUTINE_REMINDERS";
    private static final String CHANNEL_NAME = "Recordatorios de Rutina";
    private static final String CHANNEL_DESCRIPTION = "Notificaciones para recordar actividades de la rutina diaria";
    
    private Context context;
    private NotificationManagerCompat notificationManager;
    
    public NotificationService(Context context) {
        this.context = context;
        this.notificationManager = NotificationManagerCompat.from(context);
        createNotificationChannel();
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    public void scheduleActivityReminder(Activity activity) {
        // Parsear la hora de la actividad
        String[] timeParts = activity.getTime().split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);
        
        // Crear calendario para la notificación
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        
        // Si la hora ya pasó hoy, programar para mañana
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        
        // Crear intent para el broadcast receiver
        Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
        intent.putExtra("activity_id", activity.getId());
        intent.putExtra("activity_name", activity.getName());
        intent.putExtra("activity_time", activity.getTime());
        intent.putExtra("pictogram_id", activity.getPictogramId());
        intent.putExtra("pictogram_keyword", activity.getPictogramKeyword());
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            activity.getId().hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Debug: mostrar información
        System.out.println("RECORDATORIO: Programando para " + activity.getName() + " a las " + 
                          java.text.DateFormat.getTimeInstance().format(calendar.getTime()));
        
        // Programar la alarma
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
                );
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
                );
            }
        }
    }
    
    public void showActivityNotification(String activityName, String activityId, int pictogramId) {
        // Intent para abrir la pantalla de actividad completa
        Intent fullScreenIntent = new Intent(context, FullScreenActivityActivity.class);
        fullScreenIntent.putExtra("activity_name", activityName);
        fullScreenIntent.putExtra("activity_id", activityId);
        fullScreenIntent.putExtra("pictogram_id", pictogramId);
        fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            activityId.hashCode(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Crear notificación
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("¡Es hora de: " + activityName + "!")
                .setContentText("Toca para ver tu actividad")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 1000, 500, 1000})
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setContentIntent(fullScreenPendingIntent)
                .addAction(R.drawable.ic_check, "Completar", createCompleteAction(activityId))
                .addAction(R.drawable.ic_volume, "Escuchar", createSpeakAction(activityName));
        
        // Mostrar notificación
        notificationManager.notify(activityId.hashCode(), builder.build());
        
        // Abrir automáticamente la pantalla completa
        context.startActivity(fullScreenIntent);
    }
    
    private PendingIntent createCompleteAction(String activityId) {
        Intent completeIntent = new Intent(context, CompleteActivityReceiver.class);
        completeIntent.putExtra("activity_id", activityId);
        
        return PendingIntent.getBroadcast(
            context,
            ("complete_" + activityId).hashCode(),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
    
    private PendingIntent createSpeakAction(String activityName) {
        Intent speakIntent = new Intent(context, SpeakActivityReceiver.class);
        speakIntent.putExtra("activity_name", activityName);
        
        return PendingIntent.getBroadcast(
            context,
            ("speak_" + activityName).hashCode(),
            speakIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
    
    public void cancelActivityReminder(String activityId) {
        Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            activityId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            System.out.println("RECORDATORIO CANCELADO para actividad");
        }
    }
    
    public void cancelAllReminders() {
        try {
            System.out.println("CANCELANDO TODOS LOS RECORDATORIOS");
            // Este método se puede usar para limpiar todas las alarmas si es necesario
        } catch (Exception e) {
            System.out.println("Error al cancelar recordatorios: " + e.getMessage());
        }
    }
}
