package com.example.mirutinavisual;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

public class SpeakActivityReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String activityName = intent.getStringExtra("activity_name");
        
        if (activityName != null) {
            // Crear TTS para hablar el nombre de la actividad
            final TextToSpeech[] ttsArray = new TextToSpeech[1];
            
            ttsArray[0] = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status == TextToSpeech.SUCCESS && ttsArray[0] != null) {
                        int result = ttsArray[0].setLanguage(new Locale("es", "ES"));
                        
                        if (result == TextToSpeech.LANG_MISSING_DATA || 
                            result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            ttsArray[0].setLanguage(Locale.US);
                        }
                        
                        ttsArray[0].speak("Es hora de: " + activityName, TextToSpeech.QUEUE_FLUSH, null, null);
                        
                        // Liberar recursos después de un tiempo
                        android.os.Handler handler = new android.os.Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (ttsArray[0] != null) {
                                    ttsArray[0].shutdown();
                                }
                            }
                        }, 3000); // 3 segundos después
                    }
                }
            });
        }
    }
}
