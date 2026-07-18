package com.example.savio4

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class OnboardingActivity : AppCompatActivity() {

    private val permissionRequestCode = 201

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
    }

    override fun onResume() {
        super.onResume()
        // Osvježi ekran kada se korisnik vrati iz podešavanja
        buildScreen()
    }

    private fun buildScreen() {
        val scrollView = ScrollView(this)
        scrollView.setBackgroundColor(Color.rgb(10, 12, 16))

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(32, 50, 32, 32)

        // ─── NASLOV ───
        val title = TextView(this)
        title.text = t("⚙️ PODEŠAVANJE APLIKACIJE", "⚙️ APP SETUP", "⚙️ НАСТРОЙКА ПРИЛОЖЕНИЯ", "⚙️ APP-EINRICHTUNG")
        title.textSize = 26f
        title.setTextColor(Color.WHITE)
        title.typeface = android.graphics.Typeface.DEFAULT_BOLD
        title.setPadding(0, 0, 0, 8)

        val subtitle = TextView(this)
        subtitle.text = t(
            "Da bi aplikacija radila ispravno, potrebno je odobriti nekoliko podešavanja. Ovo se radi samo jednom.",
            "To make the app work correctly, a few settings need to be approved. This is done only once.",
            "Для правильной работы приложения необходимо одобрить несколько настроек. Это делается один раз.",
            "Damit die App korrekt funktioniert, müssen einige Einstellungen genehmigt werden. Dies wird nur einmal gemacht."
        )
        subtitle.textSize = 14f
        subtitle.setTextColor(Color.rgb(180, 180, 180))
        subtitle.setPadding(0, 0, 0, 32)

        container.addView(title)
        container.addView(subtitle)

        // ─── KORAK 1 — DOZVOLE ───
        val allPermsGranted = checkAllPermissions()
        container.addView(buildStep(
            number = "1",
            title = t("DOZVOLE ZA APLIKACIJU", "APP PERMISSIONS", "РАЗРЕШЕНИЯ ПРИЛОЖЕНИЯ", "APP-BERECHTIGUNGEN"),
            description = t(
                "Lokacija (GPS), Slanje SMS, Telefonski pozivi, Obaveštenja",
                "Location (GPS), Send SMS, Phone calls, Notifications",
                "Местоположение (GPS), Отправка SMS, Звонки, Уведомления",
                "Standort (GPS), SMS senden, Anrufe, Benachrichtigungen"
            ),
            buttonText = if (allPermsGranted)
                t("✅ SVE DOZVOLE ODOBRENE", "✅ ALL PERMISSIONS GRANTED", "✅ ВСЕ РАЗРЕШЕНИЯ ОДОБРЕНЫ", "✅ ALLE BERECHTIGUNGEN ERTEILT")
            else
                t("ODOBRI DOZVOLE", "GRANT PERMISSIONS", "РАЗРЕШИТЬ", "BERECHTIGUNGEN ERTEILEN"),
            buttonColor = if (allPermsGranted) Color.rgb(0, 130, 60) else Color.rgb(0, 100, 180),
            enabled = !allPermsGranted,
            onClick = { requestAllPermissions() }
        ))

        // ─── KORAK 2 — OBAVEŠTENJA ───
        container.addView(buildStep(
            number = "2",
            title = t("PODEŠAVANJA OBAVEŠTENJA", "NOTIFICATION SETTINGS", "НАСТРОЙКИ УВЕДОМЛЕНИЙ", "BENACHRICHTIGUNGSEINSTELLUNGEN"),
            description = t(
                "Postavite obaveštenja na maksimum — iskačući prozori sa detaljima. Ovo je važno da ne propustite SOS alarm.",
                "Set notifications to maximum — pop-up windows with details. This is important so you don't miss SOS alerts.",
                "Установите уведомления на максимум — всплывающие окна с деталями. Это важно, чтобы не пропустить SOS-сигнал.",
                "Benachrichtigungen auf Maximum setzen — Popup-Fenster mit Details. Wichtig, um SOS-Alarme nicht zu verpassen."
            ),
            buttonText = t(
                "OTVORI PODEŠAVANJA OBAVEŠTENJA",
                "OPEN NOTIFICATION SETTINGS",
                "ОТКРЫТЬ НАСТРОЙКИ УВЕДОМЛЕНИЙ",
                "BENACHRICHTIGUNGSEINSTELLUNGEN ÖFFNEN"
            ),
            buttonColor = Color.rgb(150, 80, 0),
            enabled = true,
            onClick = { openNotificationSettings() }
        ))

        // ─── KORAK 3 — BATERIJA ───
        val batteryOptimized = isBatteryOptimizationEnabled()
        container.addView(buildStep(
            number = "3",
            title = t("OPTIMIZACIJA BATERIJE", "BATTERY OPTIMIZATION", "ОПТИМИЗАЦИЯ БАТАРЕИ", "AKKUOPTIMIERUNG"),
            description = t(
                "Isključite optimizaciju baterije za SAVIO SOS — sprečava Android da gasi praćenje lokacije u pozadini.",
                "Disable battery optimization for SAVIO SOS — prevents Android from shutting down location tracking in background.",
                "Отключите оптимизацию батареи для SAVIO SOS — Android не будет останавливать отслеживание местоположения.",
                "Akkuoptimierung für SAVIO SOS deaktivieren — verhindert, dass Android die Standortverfolgung stoppt."
            ),
            buttonText = if (!batteryOptimized)
                t("✅ BATERIJA PODEŠENA", "✅ BATTERY SET", "✅ БАТАРЕЯ НАСТРОЕНА", "✅ AKKU EINGESTELLT")
            else
                t("ISKLJUČI OPTIMIZACIJU", "DISABLE OPTIMIZATION", "ОТКЛЮЧИТЬ ОПТИМИЗАЦИЮ", "OPTIMIERUNG DEAKTIVIEREN"),
            buttonColor = if (!batteryOptimized) Color.rgb(0, 130, 60) else Color.rgb(100, 50, 0),
            enabled = batteryOptimized,
            onClick = { openBatterySettings() }
        ))

        // ─── INFO BOX ───
        val infoBox = TextView(this)
        infoBox.text = "ℹ️ " + t(
            "Obaveštenja i optimizacija baterije se podešavaju ručno u sistemskim podešavanjima. Ovo je Android zahtev koji ne možemo zaobići.",
            "Notifications and battery optimization are set manually in system settings. This is an Android requirement we cannot bypass.",
            "Уведомления и оптимизация батареи настраиваются вручную в системных настройках. Это требование Android.",
            "Benachrichtigungen und Akkuoptimierung werden manuell in den Systemeinstellungen festgelegt. Android-Anforderung."
        )
        infoBox.textSize = 12f
        infoBox.setTextColor(Color.rgb(150, 150, 150))
        infoBox.setPadding(16, 16, 16, 16)
        val infoBg = GradientDrawable()
        infoBg.setColor(Color.rgb(20, 25, 35))
        infoBg.cornerRadius = 12f
        infoBg.setStroke(1, Color.rgb(50, 60, 80))
        infoBox.background = infoBg
        val infoParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        infoParams.setMargins(0, 16, 0, 24)
        infoBox.layoutParams = infoParams
        container.addView(infoBox)

        // ─── DUGME NASTAVI ───
        val btnContinue = Button(this)
        btnContinue.text = t(
            "NASTAVI NA UNOS PROFILA →",
            "CONTINUE TO PROFILE →",
            "ПРОДОЛЖИТЬ К ПРОФИЛЮ →",
            "WEITER ZUM PROFIL →"
        )
        btnContinue.setTextColor(Color.WHITE)
        btnContinue.setBackgroundColor(Color.rgb(0, 130, 60))
        btnContinue.textSize = 16f
        val btnParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        btnParams.setMargins(0, 0, 0, 16)
        btnContinue.layoutParams = btnParams
        btnContinue.setOnClickListener {
            getSharedPreferences("savio_prefs", MODE_PRIVATE).edit()
                .putBoolean("onboardingDone", true)
                .apply()
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }

        val btnSkip = Button(this)
        btnSkip.text = t(
            "PRESKOČI — podesiću kasnije",
            "SKIP — I'll set up later",
            "ПРОПУСТИТЬ — настрою позже",
            "ÜBERSPRINGEN — später einrichten"
        )
        btnSkip.setTextColor(Color.rgb(150, 150, 150))
        btnSkip.setBackgroundColor(Color.TRANSPARENT)
        btnSkip.textSize = 14f
        btnSkip.setOnClickListener {
            getSharedPreferences("savio_prefs", MODE_PRIVATE).edit()
                .putBoolean("onboardingDone", true)
                .apply()
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }

        container.addView(btnContinue)
        container.addView(btnSkip)

        scrollView.addView(container)
        setContentView(scrollView)
    }

    private fun buildStep(
        number: String,
        title: String,
        description: String,
        buttonText: String,
        buttonColor: Int,
        enabled: Boolean,
        onClick: () -> Unit
    ): LinearLayout {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(24, 20, 24, 20)

        val cardBg = GradientDrawable()
        cardBg.setColor(Color.rgb(18, 22, 30))
        cardBg.cornerRadius = 16f
        cardBg.setStroke(2, Color.rgb(40, 50, 70))
        card.background = cardBg

        val cardParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        cardParams.setMargins(0, 0, 0, 16)
        card.layoutParams = cardParams

        val headerRow = LinearLayout(this)
        headerRow.orientation = LinearLayout.HORIZONTAL
        headerRow.gravity = Gravity.CENTER_VERTICAL
        headerRow.setPadding(0, 0, 0, 8)

        val numberBadge = TextView(this)
        numberBadge.text = number
        numberBadge.textSize = 16f
        numberBadge.setTextColor(Color.WHITE)
        numberBadge.gravity = Gravity.CENTER
        val badgeBg = GradientDrawable()
        badgeBg.shape = GradientDrawable.OVAL
        badgeBg.setColor(Color.rgb(0, 100, 180))
        numberBadge.background = badgeBg
        val badgeParams = LinearLayout.LayoutParams(dpToPx(36), dpToPx(36))
        badgeParams.setMargins(0, 0, 16, 0)
        numberBadge.layoutParams = badgeParams
        numberBadge.typeface = android.graphics.Typeface.DEFAULT_BOLD

        val titleText = TextView(this)
        titleText.text = title
        titleText.textSize = 16f
        titleText.setTextColor(Color.WHITE)
        titleText.typeface = android.graphics.Typeface.DEFAULT_BOLD

        headerRow.addView(numberBadge)
        headerRow.addView(titleText)

        val descText = TextView(this)
        descText.text = description
        descText.textSize = 13f
        descText.setTextColor(Color.rgb(160, 160, 160))
        descText.setPadding(0, 0, 0, 16)

        val btn = Button(this)
        btn.text = buttonText
        btn.setTextColor(Color.WHITE)
        btn.setBackgroundColor(buttonColor)
        btn.textSize = 14f
        btn.isEnabled = enabled
        if (!enabled) btn.alpha = 0.6f
        btn.setOnClickListener { onClick() }

        card.addView(headerRow)
        card.addView(descText)
        card.addView(btn)

        return card
    }

    private fun checkAllPermissions(): Boolean {
        val permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CALL_PHONE
        )
        return permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
    }

    private fun requestAllPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CALL_PHONE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), permissionRequestCode)
    }

    private fun isBatteryOptimizationEnabled(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            return !pm.isIgnoringBatteryOptimizations(packageName)
        }
        return false
    }

    private fun openBatterySettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        } catch (e: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (_: Exception) {}
        }
    }

    private fun openNotificationSettings() {
        try {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionRequestCode) buildScreen()
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private fun currentLanguage(): String {
        return getSharedPreferences("savio_prefs", MODE_PRIVATE).getString("language", "sr") ?: "sr"
    }

    private fun t(sr: String, en: String, ru: String, de: String): String {
        return when (currentLanguage()) { "en" -> en; "ru" -> ru; "de" -> de; else -> sr }
    }
}
