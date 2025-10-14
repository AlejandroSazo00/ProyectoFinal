package com.example.mirutinavisual;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.content.ContextCompat;

import androidx.core.app.NotificationCompat;

import java.util.Calendar;

public class NotificationService {
    
    private static final String CHANNEL_ID = "ROUTINE_REMINDERS";
    private static final String CHANNEL_NAME = "Recordatorios de Rutina";
    private static final String CHANNEL_DESCRIPTION = "Notificaciones para recordatorios de actividades de rutina";
    
    private final Context context;
    private final NotificationManager notificationManager;
    
    public NotificationService(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }
    
    private void createNotificationChannel() {
        // Android 8.0+ (API 26+) - siempre necesario crear canal
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
    
    public void scheduleActivityReminder(Activity activity) {
        System.out.println("NOTIFICATION_SERVICE: ===== INICIANDO scheduleActivityReminder =====");
        System.out.println("NOTIFICATION_SERVICE: Actividad recibida: " + activity.getName());
        System.out.println("NOTIFICATION_SERVICE: Hora recibida: " + activity.getTime());
        System.out.println("NOTIFICATION_SERVICE: ID recibida: " + activity.getId());
        
        // Parsear la hora de la actividad
        String[] timeParts = activity.getTime().split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);
        
        System.out.println("NOTIFICATION_SERVICE: Hora parseada: " + hour + ":" + minute);
        
        // Crear calendario para la notificación (HOY, no mañana)
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        
        System.out.println("NOTIFICATION_SERVICE: Calendario inicial configurado para: " + calendar.getTime());
        System.out.println("NOTIFICATION_SERVICE: Zona horaria del calendario: " + calendar.getTimeZone().getDisplayName());
        System.out.println("NOTIFICATION_SERVICE: Hora actual del sistema: " + new java.util.Date());
        
        // PRUEBA: Crear alarma para 2 minutos desde ahora
        Calendar testCalendar = Calendar.getInstance();
        testCalendar.add(Calendar.MINUTE, 2);
        System.out.println("NOTIFICATION_SERVICE: 🧪 PRUEBA: Alarma de 2 minutos sería para: " + testCalendar.getTime());
        
        // Si la hora ya pasó por más de 5 minutos, programar para mañana
        // Esto permite crear recordatorios para los próximos minutos
        long currentTime = System.currentTimeMillis();
        long scheduledTime = calendar.getTimeInMillis();
        long timeDifference = scheduledTime - currentTime;
        
        System.out.println("NOTIFICATION_SERVICE: Tiempo actual: " + new java.util.Date(currentTime));
        System.out.println("NOTIFICATION_SERVICE: Tiempo programado: " + new java.util.Date(scheduledTime));
        System.out.println("NOTIFICATION_SERVICE: Diferencia en minutos: " + (timeDifference / 60000));
        
        if (timeDifference < -300000) { // Si pasó hace más de 5 minutos (300,000 ms)
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            System.out.println("NOTIFICATION_SERVICE: ⚠️ Hora ya pasó hace más de 5 min, programando para mañana");
            System.out.println("NOTIFICATION_SERVICE: Nueva fecha: " + calendar.getTime());
        } else if (timeDifference < 0 && timeDifference > -300000) {
            // Si la hora pasó hace menos de 5 minutos, programar para ahora + 1 minuto
            calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, 1);
            System.out.println("NOTIFICATION_SERVICE: ⚠️ Hora reciente, programando para 1 minuto desde ahora");
            System.out.println("NOTIFICATION_SERVICE: Nueva fecha: " + calendar.getTime());
        } else if (timeDifference > 0) {
            System.out.println("NOTIFICATION_SERVICE: ✅ Hora futura válida, programando normalmente");
        } else {
            System.out.println("NOTIFICATION_SERVICE: 🤔 Situación extraña con timeDifference: " + timeDifference);
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
        
        // Debug: mostrar información detallada (reutilizar variables existentes)
        scheduledTime = calendar.getTimeInMillis(); // Actualizar con la hora final
        long timeUntilAlarm = scheduledTime - System.currentTimeMillis(); // Recalcular
        
        System.out.println("NOTIFICATION_SERVICE: ===== INFORMACIÓN FINAL DE ALARMA =====");
        System.out.println("NOTIFICATION_SERVICE: Programando para: " + activity.getName());
        System.out.println("NOTIFICATION_SERVICE: Hora final programada: " + java.text.DateFormat.getTimeInstance().format(calendar.getTime()));
        System.out.println("NOTIFICATION_SERVICE: Tiempo hasta alarma: " + (timeUntilAlarm / 1000) + " segundos");
        System.out.println("NOTIFICATION_SERVICE: Tiempo hasta alarma: " + (timeUntilAlarm / 60000) + " minutos");
        System.out.println("NOTIFICATION_SERVICE: ID de actividad: " + activity.getId());
        System.out.println("NOTIFICATION_SERVICE: Intent creado con extras:");
        
        // Programar la alarma con verificación explícita de permisos
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            // Verificar permisos explícitamente como requiere Android Studio
            if (hasAlarmPermissions()) {
                try {
                    // Verificar permisos para Android 12+ (API 31+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                calendar.getTimeInMillis(),
                                pendingIntent
                            );
                            System.out.println("NOTIFICATION_SERVICE: ✅ ALARMA PROGRAMADA CON setExactAndAllowWhileIdle (Android 12+)");
                        } else {
                            // Usar alarma inexacta si no hay permisos exactos
                            alarmManager.setAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                calendar.getTimeInMillis(),
                                pendingIntent
                            );
                            System.out.println("NOTIFICATION_SERVICE: ⚠️ ALARMA PROGRAMADA CON setAndAllowWhileIdle (sin permisos exactos)");
                        }
                    } else {
                        // Android 6+ (API 23+) - siempre usamos setExactAndAllowWhileIdle
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                        );
                        System.out.println("NOTIFICATION_SERVICE: ✅ ALARMA PROGRAMADA CON setExactAndAllowWhileIdle");
                    }
                } catch (SecurityException e) {
                    System.out.println("ERROR: SecurityException al programar alarma: " + e.getMessage());
                    scheduleInexactAlarm(alarmManager, calendar, pendingIntent);
                } catch (Exception e) {
                    System.out.println("ERROR: Excepción general al programar alarma: " + e.getMessage());
                }
            } else {
                System.out.println("ERROR: Sin permisos de alarma, usando alarma inexacta");
                scheduleInexactAlarm(alarmManager, calendar, pendingIntent);
            }
        } else {
            System.out.println("NOTIFICATION_SERVICE: ❌ ERROR: AlarmManager es null");
        }
        
        System.out.println("NOTIFICATION_SERVICE: ===== FIN scheduleActivityReminder =====");
    }
    
    // Método para verificar permisos explícitamente
    private boolean hasAlarmPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ requiere permiso especial
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            return alarmManager != null && alarmManager.canScheduleExactAlarms();
        }
        return true; // Versiones anteriores no requieren verificación especial
    }
    
    // Método para programar alarma inexacta como fallback
    private void scheduleInexactAlarm(AlarmManager alarmManager, Calendar calendar, PendingIntent pendingIntent) {
        try {
            // Android 6+ (API 23+) - siempre usamos setAndAllowWhileIdle
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                pendingIntent
            );
            System.out.println("RECORDATORIO: Alarma inexacta programada con setAndAllowWhileIdle");
        } catch (Exception e) {
            System.out.println("ERROR: No se pudo programar ni siquiera alarma inexacta: " + e.getMessage());
        }
    }
    
    public void showActivityNotification(String activityName, String activityId) {
        // Intent que verifica sesión antes de decidir a dónde ir
        Intent notificationIntent = createSecureNotificationIntent(activityName, activityId);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent mainPendingIntent = PendingIntent.getActivity(
            context,
            activityId.hashCode(),
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Crear notificación simple y no invasiva
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("🔔 Recordatorio: " + activityName)
                .setContentText("Toca para abrir Mi Rutina Visual")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Menos invasiva
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 500, 200, 500}) // Vibración más suave
                .setContentIntent(mainPendingIntent) // Va a pantalla principal
                .addAction(R.drawable.ic_volume, "🎙️ Escuchar", createSpeakAction(activityName)); // Solo botón escuchar
        
        // Verificar permisos de notificación antes de mostrar
        if (hasNotificationPermissions()) {
            // Mostrar SOLO la notificación (NO abrir automáticamente)
            notificationManager.notify(activityId.hashCode(), builder.build());
            System.out.println("NOTIFICACIÓN: Enviada para " + activityName + " (no invasiva)");
        } else {
            System.out.println("ERROR: Sin permisos de notificación para " + activityName);
        }
    }
    
    // Método para verificar permisos de notificación
    private boolean hasNotificationPermissions() {
        // Para Android 13+ verificar permisos de notificación
        return ContextCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS") 
               == PackageManager.PERMISSION_GRANTED;
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
    
    // Método para crear intent seguro que verifica sesión
    private Intent createSecureNotificationIntent(String activityName, String activityId) {
        // Verificar si el usuario tiene sesión activa
        com.google.firebase.auth.FirebaseAuth firebaseAuth = com.google.firebase.auth.FirebaseAuth.getInstance();
        com.google.firebase.auth.FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        
        Intent intent;
        
        if (currentUser != null) {
            // Usuario logueado → Ir a pantalla principal
            intent = new Intent(context, MainActivity.class);
            intent.putExtra("from_notification", true);
            intent.putExtra("activity_name", activityName);
            intent.putExtra("activity_id", activityId);
            System.out.println("NOTIFICATION_SERVICE: ✅ Usuario logueado, dirigiendo a MainActivity");
        } else {
            // Usuario NO logueado → Ir a login
            intent = new Intent(context, LoginActivity.class);
            intent.putExtra("from_notification", true);
            intent.putExtra("pending_activity_name", activityName);
            intent.putExtra("pending_activity_id", activityId);
            System.out.println("NOTIFICATION_SERVICE: ⚠️ Usuario NO logueado, dirigiendo a LoginActivity");
        }
        
        return intent;
    }
    
    public void cancelActivityReminder(String activityId) {
        System.out.println("NOTIFICATION_SERVICE: Cancelando recordatorio para actividad: " + activityId);
        
        Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
        intent.putExtra("activity_id", activityId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            activityId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            System.out.println("NOTIFICATION_SERVICE: ✅ Recordatorio cancelado para actividad: " + activityId);
        } else {
            System.out.println("NOTIFICATION_SERVICE: ❌ Error: AlarmManager es null al cancelar");
        }
    }
}
