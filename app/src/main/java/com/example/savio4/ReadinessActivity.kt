package com.example.savio4

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class ReadinessActivity : AppCompatActivity() {

    private val testNumber = "+381652013323"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
    }

    private fun buildScreen() {
        val scrollView = ScrollView(this)

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(32, 50, 32, 32)
        container.setBackgroundColor(Color.rgb(10, 12, 16))

        val title = TextView(this)
        title.text = t(
            "PROVERA SPREMNOSTI",
            "READINESS CHECK",
            "ПРОВЕРКА ГОТОВНОСТИ",
            "BEREITSCHAFTSPRÜFUNG"
        )
        title.textSize = 28f
        title.setTextColor(Color.WHITE)

        val info = TextView(this)
        info.text = t(
            "Ovaj ekran proverava osnovne uslove za rad SAVIO SOS aplikacije.",
            "This screen checks the basic conditions required for SAVIO SOS to work.",
            "Этот экран проверяет основные условия для работы SAVIO SOS.",
            "Dieser Bildschirm prüft die grundlegenden Voraussetzungen für SAVIO SOS."
        )
        info.textSize = 15f
        info.setTextColor(Color.LTGRAY)
        info.setPadding(0, 10, 0, 24)

        val gpsEnabled = isGpsEnabled()
        val fineLocation = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        val smsAllowed = hasPermission(Manifest.permission.SEND_SMS)
        val callAllowed = hasPermission(Manifest.permission.CALL_PHONE)
        val notificationsAllowed = areNotificationsAllowed()
        val batteryUnrestricted = isBatteryUnrestricted()
        val gsmAvailable = isGsmAvailable()

        val statusBox = TextView(this)
        statusBox.text = buildStatusText(
            gpsEnabled,
            fineLocation,
            smsAllowed,
            callAllowed,
            notificationsAllowed,
            batteryUnrestricted,
            gsmAvailable
        )
        statusBox.textSize = 17f
        statusBox.setTextColor(Color.WHITE)
        statusBox.setPadding(0, 10, 0, 22)

        // ─────────────────────────────────────────────
        // GSM UPOZORENJE — prikazuje se uvijek
        // ─────────────────────────────────────────────

        val gsmWarningBox = TextView(this)
        gsmWarningBox.text = t(
            "⚠️ VAŽNO OGRANIČENJE\n\nSAVIO SOS funkcioniše samo ako telefon ima GSM signal u trenutku aktivacije ili u nekom momentu nakon aktivacije.\n\nNa terenima bez GSM pokrivenosti aplikacija NE MOŽE garantovati isporuku SOS signala.\n\nZa rad na potpuno izolovanim terenima preporučujemo satelitski uređaj:\n• Garmin inReach\n• SPOT X",
            "⚠️ IMPORTANT LIMITATION\n\nSAVIO SOS works only if the phone has a GSM signal at the time of activation or at some point after activation.\n\nIn areas without GSM coverage, the application CANNOT guarantee delivery of the SOS signal.\n\nFor use in completely isolated areas, we recommend a satellite device:\n• Garmin inReach\n• SPOT X",
            "⚠️ ВАЖНОЕ ОГРАНИЧЕНИЕ\n\nSAVIO SOS работает только при наличии GSM-сигнала в момент активации или в какой-либо момент после неё.\n\nВ районах без GSM-покрытия приложение НЕ МОЖЕТ гарантировать доставку SOS-сигнала.\n\nДля работы в полностью изолированных районах рекомендуем спутниковые устройства:\n• Garmin inReach\n• SPOT X",
            "⚠️ WICHTIGE EINSCHRÄNKUNG\n\nSAVIO SOS funktioniert nur, wenn das Telefon zum Zeitpunkt der Aktivierung oder danach ein GSM-Signal hat.\n\nIn Gebieten ohne GSM-Abdeckung KANN die Anwendung die Zustellung des SOS-Signals NICHT garantieren.\n\nFür den Einsatz in völlig abgelegenen Gebieten empfehlen wir ein Satellitengerät:\n• Garmin inReach\n• SPOT X"
        )
        gsmWarningBox.textSize = 14f
        gsmWarningBox.setTextColor(Color.WHITE)
        gsmWarningBox.setPadding(24, 24, 24, 24)

        val gsmWarningBg = android.graphics.drawable.GradientDrawable()
        gsmWarningBg.setColor(Color.rgb(50, 35, 0))
        gsmWarningBg.cornerRadius = 16f
        gsmWarningBg.setStroke(2, Color.rgb(255, 165, 0))
        gsmWarningBox.background = gsmWarningBg

        val gsmWarningParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        gsmWarningParams.setMargins(0, 0, 0, 24)
        gsmWarningBox.layoutParams = gsmWarningParams

        // ─────────────────────────────────────────────
        // DUGMAD
        // ─────────────────────────────────────────────

        val btnTestSms = Button(this)
        btnTestSms.text = t(
            "POŠALJI TEST SMS",
            "SEND TEST SMS",
            "ОТПРАВИТЬ ТЕСТОВОЕ SMS",
            "TEST-SMS SENDEN"
        )
        btnTestSms.setTextColor(Color.WHITE)
        btnTestSms.setBackgroundColor(Color.rgb(0, 130, 60))

        btnTestSms.setOnClickListener {
            if (!smsAllowed) {
                Toast.makeText(
                    this,
                    t(
                        "SMS dozvola nije odobrena.",
                        "SMS permission is not granted.",
                        "Разрешение SMS не предоставлено.",
                        "SMS-Berechtigung fehlt."
                    ),
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            sendTestSms()
        }

        val btnAppSettings = Button(this)
        btnAppSettings.text = t(
            "OTVORI PODEŠAVANJA APLIKACIJE",
            "OPEN APP SETTINGS",
            "ОТКРЫТЬ НАСТРОЙКИ ПРИЛОЖЕНИЯ",
            "APP-EINSTELLUNGEN ÖFFNEN"
        )

        btnAppSettings.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }

        val btnRefresh = Button(this)
        btnRefresh.text = t(
            "OSVEŽI STATUS",
            "REFRESH STATUS",
            "ОБНОВИТЬ СТАТУС",
            "STATUS AKTUALISIEREN"
        )
        btnRefresh.setOnClickListener {
            buildScreen()
        }

        val btnBack = Button(this)
        btnBack.text = t("NAZAD", "BACK", "НАЗАД", "ZURÜCK")
        btnBack.setOnClickListener {
            finish()
        }

        container.addView(title)
        container.addView(info)
        container.addView(statusBox)
        container.addView(gsmWarningBox)
        container.addView(btnTestSms)
        container.addView(btnAppSettings)
        container.addView(btnRefresh)
        container.addView(btnBack)

        scrollView.addView(container)
        setContentView(scrollView)
    }

    private fun buildStatusText(
        gpsEnabled: Boolean,
        fineLocation: Boolean,
        smsAllowed: Boolean,
        callAllowed: Boolean,
        notificationsAllowed: Boolean,
        batteryUnrestricted: Boolean,
        gsmAvailable: Boolean
    ): String {
        val ok = "🟢"
        val bad = "🔴"
        val warn = "🟡"

        return """
            ${if (gpsEnabled) ok else bad} ${t("GPS", "GPS", "GPS", "GPS")}: ${yesNo(gpsEnabled, t("UKLJUČEN", "ON", "ВКЛЮЧЕН", "EIN"), t("ISKLJUČEN", "OFF", "ВЫКЛЮЧЕН", "AUS"))}

            ${if (fineLocation) ok else bad} ${t("Precizna lokacija", "Precise location", "Точная геолокация", "Genauer Standort")}: ${yesNo(fineLocation, t("DOZVOLJENA", "ALLOWED", "РАЗРЕШЕНА", "ERLAUBT"), t("NIJE DOZVOLJENA", "NOT ALLOWED", "НЕ РАЗРЕШЕНА", "NICHT ERLAUBT"))}

            ${if (smsAllowed) ok else bad} SMS: ${yesNo(smsAllowed, t("DOZVOLJEN", "ALLOWED", "РАЗРЕШЕНО", "ERLAUBT"), t("NIJE DOZVOLJEN", "NOT ALLOWED", "НЕ РАЗРЕШЕНО", "NICHT ERLAUBT"))}

            ${if (callAllowed) ok else bad} ${t("Pozivi", "Calls", "Звонки", "Anrufe")}: ${yesNo(callAllowed, t("DOZVOLJENI", "ALLOWED", "РАЗРЕШЕНЫ", "ERLAUBT"), t("NISU DOZVOLJENI", "NOT ALLOWED", "НЕ РАЗРЕШЕНЫ", "NICHT ERLAUBT"))}

            ${if (notificationsAllowed) ok else bad} ${t("Obaveštenja", "Notifications", "Уведомления", "Benachrichtigungen")}: ${yesNo(notificationsAllowed, t("UKLJUČENA", "ON", "ВКЛЮЧЕНЫ", "EIN"), t("ISKLJUČENA", "OFF", "ВЫКЛЮЧЕНЫ", "AUS"))}

            ${if (batteryUnrestricted) ok else warn} ${t("Baterija", "Battery", "Батарея", "Akku")}: ${
            if (batteryUnrestricted)
                t("bez ograničenja", "unrestricted", "без ограничений", "uneingeschränkt")
            else
                t("proveriti ručno / možda optimizovana", "check manually / may be optimized", "проверить вручную / возможно оптимизировано", "manuell prüfen / evtl. optimiert")
        }

            ${if (gsmAvailable) ok else bad} ${t("GSM signal", "GSM signal", "GSM-сигнал", "GSM-Signal")}: ${
            if (gsmAvailable)
                t("DOSTUPAN", "AVAILABLE", "ДОСТУПЕН", "VERFÜGBAR")
            else
                t("NIJE DOSTUPAN — SOS poruka neće biti isporučena!", "NOT AVAILABLE — SOS message will not be delivered!", "НЕДОСТУПЕН — SOS-сообщение не будет доставлено!", "NICHT VERFÜGBAR — SOS-Nachricht wird nicht zugestellt!")
        }

            🟢 ${t("SOS broj", "SOS number", "SOS-номер", "SOS-Nummer")}: $testNumber

            ⚪ ${t("Test poruka", "Test message", "Тестовое сообщение", "Testnachricht")}: ${t("nije proverena", "not checked", "не проверено", "nicht geprüft")}
        """.trimIndent()
    }

    private fun yesNo(condition: Boolean, yes: String, no: String): String {
        return if (condition) yes else no
    }

    private fun isGpsEnabled(): Boolean {
        return try {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (_: Exception) {
            false
        }
    }

    private fun isGsmAvailable(): Boolean {
        return try {
            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val state = telephonyManager.dataState
            val networkType = telephonyManager.networkType
            telephonyManager.simState == TelephonyManager.SIM_STATE_READY &&
                    networkType != TelephonyManager.NETWORK_TYPE_UNKNOWN
        } catch (_: Exception) {
            false
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun areNotificationsAllowed(): Boolean {
        return NotificationManagerCompat.from(this).areNotificationsEnabled()
    }

    private fun isBatteryUnrestricted(): Boolean {
        return try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isIgnoringBatteryOptimizations(packageName)
        } catch (_: Exception) {
            false
        }
    }

    private fun sendTestSms() {
        val battery = getBatteryPercent()
        val message = """
            SAVIO SOS TEST PORUKA

            Aplikacija je uspešno poslala test SMS.
            Baterija uređaja: $battery%

            Ovo nije SOS poziv.
        """.trimIndent()

        try {
            val smsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(testNumber, null, parts, null, null)

            Toast.makeText(
                this,
                t(
                    "Test SMS je poslat.",
                    "Test SMS sent.",
                    "Тестовое SMS отправлено.",
                    "Test-SMS gesendet."
                ),
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                t(
                    "Greška pri slanju test SMS-a.",
                    "Error sending test SMS.",
                    "Ошибка отправки тестового SMS.",
                    "Fehler beim Senden der Test-SMS."
                ),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun getBatteryPercent(): Int {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

        if (level < 0 || scale <= 0) return -1

        return (level * 100) / scale
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
