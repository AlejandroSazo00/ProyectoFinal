package com.example.mirutinavisual;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CompleteActivityReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String activityId = intent.getStringExtra("activity_id");
        
        if (activityId != null) {
            // Marcar actividad como completada en Firebase
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
            databaseReference.child("activities").child(activityId)
                    .child("completed").setValue(true)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "¡Actividad completada! 🎉", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Error al completar actividad", Toast.LENGTH_SHORT).show();
                    });
        }
    }
}
