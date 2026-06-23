package com.example.savio4

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val prefs = context.getSharedPreferences("savio_prefs", Context.MODE_PRIVATE)
        val sosActive = prefs.getBoolean("sosActive", false)

        if (!sosActive) return

        // SOS je bio aktivan pre restarta — restartuj servis
        val startLat = prefs.getString("sosStartLat", null)?.toDoubleOrNull() ?: return
        val startLon = prefs.getString("sosStartLon", null)?.toDoubleOrNull() ?: return
        val incidentId = prefs.getString("incidentId", "") ?: ""
        val incidentPriority = prefs.getString("incidentPriority", "") ?: ""
        val incidentCondition = prefs.getString("incidentCondition", "") ?: ""

        // Rekonstruiši SOS brojeve — podrazumevani broj ako nema u prefs
        val sosPhone = prefs.getString("sosPhone", "+381652013323") ?: "+381652013323"
        val sosNumbers = arrayListOf(sosPhone)

        val serviceIntent = Intent(context, LocationMonitorService::class.java).apply {
            putExtra("start_lat", startLat)
            putExtra("start_lon", startLon)
            putExtra("incident_id", incidentId)
            putExtra("incident_priority", incidentPriority)
            putExtra("incident_condition", incidentCondition)
            putStringArrayListExtra("sos_numbers", sosNumbers)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
