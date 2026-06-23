package com.example.savio4

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocationMonitorService : Service(), LocationListener {

    private var startLat: Double = 0.0
    private var startLon: Double = 0.0
    private var warningSent = false
    private var sosNumbers: ArrayList<String> = arrayListOf()
    private var lastKnownLocation: Location? = null

    // IZMENA #1: Čuvamo poslednju lokaciju od koje merimo pomeranje
    // Nakon potvrde upozorenja, ovo se ažurira na novu poziciju
    private var lastWarningLat: Double = 0.0
    private var lastWarningLon: Double = 0.0

    private var consecutiveMovementCount = 0
    private val requiredMovementConfirmations = 3
    private val movementThresholdMeters = 50f
    private val maxAcceptedAccuracyMeters = 50f

    private var toneGenerator: ToneGenerator? = null
    private var alarmActive = false

    // Pracenje battery upozorenja — svako se salje samo jednom
    private var batteryWarning15Sent = false
    private var batteryWarning10Sent = false

    private val handler = Handler(Looper.getMainLooper())

    private val channelId = "savio_sos_location_channel"
    private val alertChannelId = "savio_sos_alert_channel"
    private val notificationId = 2001
    private val alertNotificationId = 3001

    private val smsSentAction = "com.example.savio4.SMS_SENT"
    private var nextSmsId = 1
    private val smsQueue = mutableMapOf<Int, PendingSms>()
    private val smsPartStatus = mutableMapOf<Int, BooleanArray>()
    private val retryDelayMs = 60_000L
    private val maxSmsAttempts = 30

    data class PendingSms(
        val number: String,
        val message: String,
        var attempts: Int = 0
    )

    private val smsSentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val smsId = intent?.getIntExtra("sms_id", -1) ?: -1
            val partIndex = intent?.getIntExtra("part_index", -1) ?: -1
            val pending = smsQueue[smsId] ?: return

            if (resultCode == Activity.RESULT_OK) {
                val status = smsPartStatus[smsId]
                if (status != null && partIndex in status.indices) {
                    status[partIndex] = true
                    if (status.all { it }) {
                        smsQueue.remove(smsId)
                        smsPartStatus.remove(smsId)
                    }
                }
            } else {
                scheduleSmsRetry(smsId, pending)
            }
        }
    }

    private val alarmRunnable = object : Runnable {
        override fun run() {
            if (alarmActive) {
                try {
                    toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 650)
                } catch (_: Exception) {
                }
                handler.postDelayed(this, 850L)
            }
        }
    }

    private val periodicRunnable = object : Runnable {
        override fun run() {
            lastKnownLocation?.let {
                sendPeriodicLocationSms(it)
            }
            // Provjeri bateriju i upozori ako je nivo kritican
            checkBatteryAndWarn()
            // Adaptivni interval — zavisi od nivoa baterije
            handler.postDelayed(this, getPeriodicIntervalMs())
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        registerSmsReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent?.action == "STOP_ALARM_ONLY") {
            stopWarningAlarm()
            return START_STICKY
        }

        // IZMENA #1: Ako WarningActivity javlja da je korisnik potvrdio novu lokaciju
        if (intent?.action == "RESET_WARNING") {
            val newLat = intent.getDoubleExtra("new_lat", Double.NaN)
            val newLon = intent.getDoubleExtra("new_lon", Double.NaN)
            if (!newLat.isNaN() && !newLon.isNaN()) {
                resetWarningFromNewLocation(newLat, newLon)
            }
            return START_STICKY
        }

        val incomingNumbers = intent?.getStringArrayListExtra("sos_numbers")
        if (!incomingNumbers.isNullOrEmpty()) {
            sosNumbers = incomingNumbers
        }

        val incomingLat = intent?.getDoubleExtra("start_lat", Double.NaN) ?: Double.NaN
        val incomingLon = intent?.getDoubleExtra("start_lon", Double.NaN) ?: Double.NaN

        if (!incomingLat.isNaN() && !incomingLon.isNaN()) {
            startLat = incomingLat
            startLon = incomingLon
            lastWarningLat = incomingLat
            lastWarningLon = incomingLon
        }

        startForeground(notificationId, buildNotification())
        startLocationUpdates()
        startPeriodicLocationSending()

        return START_STICKY
    }

    // IZMENA #1: Reset upozorenja na novu lokaciju
    private fun resetWarningFromNewLocation(newLat: Double, newLon: Double) {
        lastWarningLat = newLat
        lastWarningLon = newLon
        startLat = newLat
        startLon = newLon
        warningSent = false
        consecutiveMovementCount = 0
        stopWarningAlarm()
    }

    private fun buildNotification() =
        NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("SAVIO SOS aktivan")
            .setContentText("Nadzor lokacije je aktivan. Lokacija se šalje na svakih 5 minuta.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mainChannel = NotificationChannel(
                channelId,
                "SAVIO SOS nadzor lokacije",
                NotificationManager.IMPORTANCE_HIGH
            )
            mainChannel.description = "Stalna notifikacija dok je SOS nadzor lokacije aktivan."

            val alertChannel = NotificationChannel(
                alertChannelId,
                "SAVIO SOS hitno upozorenje",
                NotificationManager.IMPORTANCE_HIGH
            )
            alertChannel.description = "Hitno upozorenje kada se korisnik pomeri od početne SOS lokacije."
            alertChannel.enableVibration(true)
            alertChannel.lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(mainChannel)
            manager.createNotificationChannel(alertChannel)
        }
    }

    private fun registerSmsReceiver() {
        val filter = IntentFilter(smsSentAction)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(smsSentReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(smsSentReceiver, filter)
        }
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf()
            return
        }

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 10f, this)
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 10f, this)
        } catch (e: SecurityException) {
            stopSelf()
        }
    }

    private fun startPeriodicLocationSending() {
        handler.removeCallbacks(periodicRunnable)
        handler.postDelayed(periodicRunnable, getPeriodicIntervalMs())
    }

    // Adaptivni interval slanja — štedi bateriju kada je nivo nizak
    private fun getPeriodicIntervalMs(): Long {
        val battery = getBatteryPercent()
        return when {
            battery < 0      -> 5 * 60 * 1000L   // Nepoznat nivo → 5 min
            battery > 50     -> 5 * 60 * 1000L   // Iznad 50%     → 5 min
            battery > 20     -> 10 * 60 * 1000L  // 21% - 50%     → 10 min
            else             -> 15 * 60 * 1000L  // 20% i manje   → 15 min
        }
    }

    override fun onLocationChanged(location: Location) {
        lastKnownLocation = location

        if (warningSent) return

        if (!isLocationReliable(location)) {
            return
        }

        // IZMENA #1: Merimo od poslednje potvrđene lokacije, ne od početne
        val referenceLocation = Location("reference").apply {
            latitude = startLat
            longitude = startLon
        }

        val distance = referenceLocation.distanceTo(location)

        if (distance >= movementThresholdMeters) {
            consecutiveMovementCount++
        } else {
            consecutiveMovementCount = 0
        }

        if (consecutiveMovementCount >= requiredMovementConfirmations) {
            warningSent = true
            playWarningAlarm()
            sendMovedLocationSms(location, distance)
            openWarningScreen(location, distance)
            showFullScreenWarningNotification(location, distance)
        }
    }

    private fun isLocationReliable(location: Location): Boolean {
        if (location.latitude == 0.0 && location.longitude == 0.0) return false

        if (location.hasAccuracy() && location.accuracy > maxAcceptedAccuracyMeters) {
            return false
        }

        return true
    }

    private fun openWarningScreen(location: Location, distance: Float) {
        val intent = Intent(this, WarningActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("distance", distance.toInt())
            putExtra("lat", location.latitude)
            putExtra("lon", location.longitude)
            putStringArrayListExtra("sos_numbers", sosNumbers)
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "UPOZORENJE: promenili ste lokaciju za ${distance.toInt()} m.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showFullScreenWarningNotification(location: Location, distance: Float) {
        val warningIntent = Intent(this, WarningActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("distance", distance.toInt())
            putExtra("lat", location.latitude)
            putExtra("lon", location.longitude)
            putStringArrayListExtra("sos_numbers", sosNumbers)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            4001,
            warningIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, alertChannelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("SAVIO SOS UPOZORENJE")
            .setContentText("Detektovano pomeranje od ${distance.toInt()} m. Dodirnite za potvrdu.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(false)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(alertNotificationId, notification)
    }

    private fun playWarningAlarm() {
        try {
            stopWarningAlarm()

            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)

            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            alarmActive = true
            handler.post(alarmRunnable)
        } catch (_: Exception) {
        }
    }

    private fun stopWarningAlarm() {
        try {
            alarmActive = false
            handler.removeCallbacks(alarmRunnable)
            toneGenerator?.release()
        } catch (_: Exception) {
        } finally {
            toneGenerator = null
        }
    }

    private fun sendMovedLocationSms(location: Location, distance: Float) {
        val lat = location.latitude
        val lon = location.longitude
        val battery = getBatteryPercent()

        // IZMENA #2: Dodajemo Incident ID
        val prefs = getSharedPreferences("savio_prefs", Context.MODE_PRIVATE)
        val incidentId = prefs.getString("incidentId", "Nepoznat") ?: "Nepoznat"

        val message = """
            AŽURIRANJE SOS POZICIJE!

            Incident ID: $incidentId

            Korisnik je promenio lokaciju za približno ${distance.toInt()} metara.
            Baterija: $battery%

            NOVA LOKACIJA:
            https://maps.google.com/?q=$lat,$lon

            Koordinate:
            $lat, $lon

            Napomena:
            Korisnik treba da ostane na novoj lokaciji osim ako je životno ugrožen.
        """.trimIndent()

        sosNumbers.forEach { number ->
            sendSms(number, message)
        }
    }

    private fun sendPeriodicLocationSms(location: Location) {
        val lat = location.latitude
        val lon = location.longitude
        val time = SimpleDateFormat("dd.MM.yyyy. HH:mm:ss", Locale.getDefault()).format(Date())
        val battery = getBatteryPercent()

        // IZMENA #2: Dodajemo Incident ID u periodični SMS
        val prefs = getSharedPreferences("savio_prefs", Context.MODE_PRIVATE)
        val incidentId = prefs.getString("incidentId", "Nepoznat") ?: "Nepoznat"

        val message = """
            STATUS SOS LOKACIJE

            Incident ID: $incidentId
            SOS režim je i dalje aktivan.
            Vreme slanja: $time
            Baterija: $battery%

            Trenutna lokacija:
            https://maps.google.com/?q=$lat,$lon

            Koordinate:
            $lat, $lon
        """.trimIndent()

        sosNumbers.forEach { number ->
            sendSms(number, message)
        }
    }

    private fun checkBatteryAndWarn() {
        val battery = getBatteryPercent()
        val location = lastKnownLocation
        val prefs = getSharedPreferences("savio_prefs", Context.MODE_PRIVATE)
        val incidentId = prefs.getString("incidentId", "Nepoznat") ?: "Nepoznat"

        val lat = location?.latitude
        val lon = location?.longitude
        val locationText = if (lat != null && lon != null) {
            """
            Poslednja poznata lokacija:
            https://maps.google.com/?q=$lat,$lon

            Koordinate:
            $lat, $lon
            """.trimIndent()
        } else {
            "Lokacija trenutno nije dostupna."
        }

        // Upozorenje na 15%
        if (battery in 1..15 && !batteryWarning15Sent) {
            batteryWarning15Sent = true

            val message = """
                UPOZORENJE — SLABA BATERIJA

                Incident ID: $incidentId

                Baterija uređaja je na $battery%.
                Uređaj se može uskoro isključiti.

                $locationText

                Korisnik je upozoren da sacuva bateriju.
            """.trimIndent()

            sosNumbers.forEach { number -> sendSms(number, message) }
            playBatteryWarningTone()
        }

        // Kritično upozorenje na 10%
        if (battery in 1..10 && !batteryWarning10Sent) {
            batteryWarning10Sent = true

            val message = """
                KRITICNO — BATERIJA ISPOD 10%!

                Incident ID: $incidentId

                Baterija uređaja je na $battery%.
                Uređaj ce se uskoro ugasiti.
                Ovo je možda poslednja poruka sa lokacijom.

                $locationText

                Hitno delujte.
            """.trimIndent()

            sosNumbers.forEach { number -> sendSms(number, message) }
            playBatteryWarningTone()
        }
    }

    private fun playBatteryWarningTone() {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)

            val tone = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            // Tri kratka signala — univerzalni signal upozorenja
            tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 300)
            handler.postDelayed({ tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 300) }, 500L)
            handler.postDelayed({ tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 300) }, 1000L)
            handler.postDelayed({ tone.release() }, 1500L)
        } catch (_: Exception) {
        }
    }

    private fun getBatteryPercent(): Int {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

        if (level < 0 || scale <= 0) return -1

        return (level * 100) / scale
    }

    private fun sendSms(number: String, message: String) {
        val smsId = nextSmsId++
        smsQueue[smsId] = PendingSms(number, message, 0)
        sendSmsAttempt(smsId)
    }

    private fun sendSmsAttempt(smsId: Int) {
        val pending = smsQueue[smsId] ?: return

        if (pending.attempts >= maxSmsAttempts) {
            smsQueue.remove(smsId)
            smsPartStatus.remove(smsId)
            return
        }

        try {
            val smsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(pending.message)
            smsPartStatus[smsId] = BooleanArray(parts.size) { false }

            val sentIntents = parts.indices.map { index ->
                val sentIntent = Intent(smsSentAction).apply {
                    putExtra("sms_id", smsId)
                    putExtra("part_index", index)
                    putExtra("parts_count", parts.size)
                }

                PendingIntent.getBroadcast(
                    this,
                    smsId * 100 + index,
                    sentIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

            pending.attempts++
            smsManager.sendMultipartTextMessage(
                pending.number,
                null,
                parts,
                ArrayList(sentIntents),
                null
            )
        } catch (_: Exception) {
            scheduleSmsRetry(smsId, pending)
        }
    }

    private fun scheduleSmsRetry(smsId: Int, pending: PendingSms) {
        if (pending.attempts >= maxSmsAttempts) {
            smsQueue.remove(smsId)
            smsPartStatus.remove(smsId)
            return
        }

        handler.postDelayed({
            if (smsQueue.containsKey(smsId)) {
                sendSmsAttempt(smsId)
            }
        }, retryDelayMs)
    }

    override fun onDestroy() {
        handler.removeCallbacks(periodicRunnable)
        handler.removeCallbacks(alarmRunnable)
        stopWarningAlarm()

        try {
            unregisterReceiver(smsSentReceiver)
        } catch (_: Exception) {
        }

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.removeUpdates(this)

        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onProviderEnabled(provider: String) {}

    override fun onProviderDisabled(provider: String) {}

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
}
