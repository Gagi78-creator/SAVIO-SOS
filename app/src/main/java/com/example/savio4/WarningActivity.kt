package com.example.savio4

import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class WarningActivity : AppCompatActivity() {

    private var sosNumbers = arrayListOf("+381652013323")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        makeVisibleOverLockScreen()

        intent.getStringArrayListExtra("sos_numbers")?.let {
            if (it.isNotEmpty()) sosNumbers = it
        }

        val distance = intent.getIntExtra("distance", 0)
        val lat = intent.getDoubleExtra("lat", 0.0)
        val lon = intent.getDoubleExtra("lon", 0.0)

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setBackgroundColor(Color.rgb(30, 0, 0))
        container.setPadding(40, 80, 40, 40)

        val title = TextView(this)
        title.text = t("⚠ UPOZORENJE ⚠", "⚠ WARNING ⚠", "⚠ ПРЕДУПРЕЖДЕНИЕ ⚠", "⚠ WARNUNG ⚠")
        title.textSize = 32f
        title.setTextColor(Color.RED)

        val message = TextView(this)
        message.text = t(
            """
            Promenili ste lokaciju za više od 50 metara.

            Udaljenost od početne SOS tačke: približno $distance m.

            Ne pomerajte se dalje osim ako ste životno ugroženi.

            Nova lokacija je već poslata spasiocima.

            Ostanite na trenutnoj poziciji.
            """.trimIndent(),
            """
            You have moved more than 50 meters.

            Distance from the initial SOS point: approximately $distance m.

            Do not move further unless your life is in danger.

            The new location has already been sent to rescuers.

            Stay at your current position.
            """.trimIndent(),
            """
            Вы изменили местоположение более чем на 50 метров.

            Расстояние от начальной SOS-точки: примерно $distance м.

            Не перемещайтесь дальше, если вашей жизни ничего не угрожает.

            Новое местоположение уже отправлено спасателям.

            Оставайтесь на текущей позиции.
            """.trimIndent(),
            """
            Sie haben Ihren Standort um mehr als 50 Meter geändert.

            Entfernung vom ursprünglichen SOS-Punkt: ungefähr $distance m.

            Bewegen Sie sich nicht weiter, es sei denn, Ihr Leben ist in Gefahr.

            Der neue Standort wurde bereits an die Rettungskräfte gesendet.

            Bleiben Sie an Ihrer aktuellen Position.
            """.trimIndent()
        )
        message.textSize = 22f
        message.setTextColor(Color.WHITE)

        val btnConfirm = Button(this)
        btnConfirm.text = t(
            "POTVRDI UPOZORENJE",
            "CONFIRM WARNING",
            "ПОДТВЕРДИТЬ ПРЕДУПРЕЖДЕНИЕ",
            "WARNUNG BESTÄTIGEN"
        )

        val btnForcedMove = Button(this)
        btnForcedMove.text = t(
            "MORAO SAM DA PROMENIM POZICIJU",
            "I HAD TO CHANGE POSITION",
            "Я БЫЛ ВЫНУЖДЕН ИЗМЕНИТЬ ПОЗИЦИЮ",
            "ICH MUSSTE DIE POSITION ÄNDERN"
        )

        btnConfirm.setOnClickListener {
            // Korisnik ostaje na novoj lokaciji — resetuj praćenje od te tačke
            resetWarning(lat, lon)
            sendCommandToService("STOP_ALARM_ONLY")
            finish()
        }

        btnForcedMove.setOnClickListener {
            // Korisnik je morao da se pomeri — pošalji SMS i resetuj praćenje
            sendForcedMoveSms(lat, lon, distance)
            resetWarning(lat, lon)
            sendCommandToService("STOP_ALARM_ONLY")
            finish()
        }

        container.addView(title)
        container.addView(message)
        container.addView(btnConfirm)
        container.addView(btnForcedMove)

        setContentView(container)
    }

    // NOVO: Resetuje praćenje od nove lokacije
    private fun resetWarning(lat: Double, lon: Double) {
        val resetIntent = Intent(this, LocationMonitorService::class.java).apply {
            action = "RESET_WARNING"
            putExtra("new_lat", lat)
            putExtra("new_lon", lon)
        }
        startService(resetIntent)
    }

    private fun makeVisibleOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )
    }

    private fun sendCommandToService(action: String) {
        val intent = Intent(this, LocationMonitorService::class.java)
        intent.action = action
        startService(intent)
    }

    private fun sendForcedMoveSms(lat: Double, lon: Double, distance: Int) {
        val battery = getBatteryPercent()

        val prefs = getSharedPreferences("savio_prefs", MODE_PRIVATE)
        val incidentId = prefs.getString("incidentId", "Nepoznat") ?: "Nepoznat"

        val message = """
            HITNO AŽURIRANJE SOS POZICIJE!

            Incident ID: $incidentId

            Korisnik je potvrdio da je morao da promeni lokaciju zbog neposredne opasnosti.

            Udaljenost od početne SOS tačke: približno $distance m.
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

    private fun getBatteryPercent(): Int {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

        if (level < 0 || scale <= 0) return -1

        return (level * 100) / scale
    }

    private fun sendSms(number: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(number, null, parts, null, null)
        } catch (_: Exception) {
        }
    }

    private fun currentLanguage(): String {
        return getSharedPreferences("savio_prefs", MODE_PRIVATE)
            .getString("language", "sr") ?: "sr"
    }

    private fun t(sr: String, en: String, ru: String, de: String): String {
        return when (currentLanguage()) {
            "en" -> en
            "ru" -> ru
            "de" -> de
            else -> sr
        }
    }
}