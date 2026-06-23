package com.example.savio4

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import android.provider.Settings

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (mediaPlayer == null) {
            mediaPlayer = createSafeMediaPlayer()
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        }

        return START_STICKY
    }

    // Pokušava redom: alarm → notifikacija → zvono → tišina
    // Notifikacije i vizuelna upozorenja ostaju aktivna u svakom slučaju
    private fun createSafeMediaPlayer(): MediaPlayer? {
        val uris = listOf(
            Settings.System.DEFAULT_ALARM_ALERT_URI,
            Settings.System.DEFAULT_NOTIFICATION_URI,
            Settings.System.DEFAULT_RINGTONE_URI
        )

        for (uri in uris) {
            if (uri == null) continue
            try {
                val player = MediaPlayer.create(this, uri)
                if (player != null) return player
            } catch (_: Exception) {
                continue
            }
        }

        // Nema dostupnog zvuka — vraća null
        // Vizuelna i tekstualna upozorenja na zaključanom i početnom ekranu ostaju aktivna
        return null
    }

    override fun onDestroy() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}